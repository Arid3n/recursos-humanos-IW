import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LeaveService } from '../../services/leave.service';
import { LeaveBalanceResponse, LeaveSummaryItem } from '../../models/leave.models';

@Component({
  selector: 'app-leave-balance-widget',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="leave-balance-container" role="region" aria-label="Saldo y balance de vacaciones">
      <!-- Encabezado con selector de año -->
      <div class="widget-header">
        <div class="header-titles">
          <h3>Saldo y Crédito de Vacaciones</h3>
          <span class="agreement-badge" *ngIf="balance()?.agreementName">
            Régimen: {{ balance()?.agreementName }}
          </span>
        </div>

        <mat-form-field appearance="outline" class="year-selector-field" subscriptSizing="dynamic">
          <mat-label>Año fiscal</mat-label>
          <mat-select [value]="selectedYear()" (selectionChange)="onYearChange($event.value)" aria-label="Seleccionar ejercicio fiscal">
            <mat-option *ngFor="let y of availableYears" [value]="y">
              {{ y }}
            </mat-option>
          </mat-select>
        </mat-form-field>
      </div>

      <!-- Banner de ejercicio cerrado / solo lectura -->
      <div *ngIf="balance()?.isClosedYear" class="closed-year-banner" role="status">
        <span class="banner-icon">🔒</span>
        <span>
          <strong>Ejercicio fiscal {{ selectedYear() }} consolidado (Solo lectura).</strong>
          Este balance refleja el cierre oficial del año y no admite nuevas solicitudes.
        </span>
      </div>

      <!-- Spinner de carga -->
      <div *ngIf="loading()" class="loading-state">
        <mat-progress-spinner mode="indeterminate" diameter="40"></mat-progress-spinner>
        <span>Actualizando cálculo de saldo...</span>
      </div>

      <!-- Cuadrícula de 4 tarjetas de saldo -->
      <div *ngIf="!loading() && balance()" class="metrics-grid">
        <!-- 1. Correspondientes -->
        <div class="metric-card entitled" tabindex="0" [attr.aria-label]="'Días correspondientes por convenio: ' + balance()?.entitledDays + ' días'">
          <div class="card-top">
            <span>📋</span>
            <span>Correspondientes</span>
          </div>
          <div class="metric-value">
            {{ balance()?.entitledDays }}<span class="metric-unit">días</span>
          </div>
          <p class="metric-desc">Fijados por convenio y fecha de alta</p>
        </div>

        <!-- 2. Disfrutados -->
        <div class="metric-card spent" tabindex="0" [attr.aria-label]="'Días disfrutados y aprobados: ' + balance()?.spentDays + ' días'">
          <div class="card-top">
            <span>🏖️</span>
            <span>Disfrutados</span>
          </div>
          <div class="metric-value">
            {{ balance()?.spentDays }}<span class="metric-unit">días</span>
          </div>
          <p class="metric-desc">Ausencias formalmente aprobadas</p>
        </div>

        <!-- 3. Disponibles -->
        <div class="metric-card available" tabindex="0" [attr.aria-label]="'Días disponibles restantes: ' + balance()?.availableDays + ' días'">
          <div class="card-top">
            <span>✨</span>
            <span>Disponibles</span>
          </div>
          <div class="metric-value">
            {{ balance()?.availableDays }}<span class="metric-unit">días</span>
          </div>
          <p class="metric-desc">Crédito libre restante para solicitar</p>
        </div>

        <!-- 4. En trámite -->
        <div class="metric-card pending" tabindex="0" [attr.aria-label]="'Días en solicitudes pendientes: ' + balance()?.pendingDays + ' días'">
          <div class="card-top">
            <span>⏳</span>
            <span>En trámite</span>
          </div>
          <div class="metric-value">
            {{ balance()?.pendingDays }}<span class="metric-unit">días</span>
          </div>
          <p class="metric-desc">Pendientes de resolución por responsable</p>
        </div>
      </div>

      <!-- Desglose de solicitudes del año -->
      <div *ngIf="!loading()" class="requests-section">
        <h4>Desglose de Ausencias en {{ selectedYear() }}</h4>
        <div *ngIf="requests().length > 0; else noRequests">
          <table class="requests-table" aria-label="Listado de solicitudes de vacaciones del año">
            <thead>
              <tr>
                <th scope="col">Tipo</th>
                <th scope="col">Desde</th>
                <th scope="col">Hasta</th>
                <th scope="col">Días</th>
                <th scope="col">Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let req of requests()">
                <td>{{ req.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}</td>
                <td>{{ req.startDate }}</td>
                <td>{{ req.endDate }}</td>
                <td><strong>{{ req.calculatedDays }}</strong></td>
                <td>
                  <span class="status-badge" [ngClass]="'status-' + req.status">
                    {{ req.status === 'APPROVED' ? 'Aprobada' : (req.status === 'PENDING' ? 'Pendiente' : 'Rechazada') }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <ng-template #noRequests>
          <div class="empty-requests">
            No constan solicitudes de ausencia formalizadas en el ejercicio {{ selectedYear() }}.
          </div>
        </ng-template>
      </div>
    </div>
  `,
  styleUrl: './leave-balance-widget.component.scss'
})
export class LeaveBalanceWidgetComponent implements OnInit {
  @Input() employeeId?: string;

  private readonly leaveService = inject(LeaveService);

  readonly currentYear = new Date().getFullYear();
  readonly availableYears: number[] = [
    this.currentYear,
    this.currentYear - 1,
    this.currentYear - 2,
    this.currentYear - 3
  ];

  readonly selectedYear = signal<number>(this.currentYear);
  readonly balance = signal<LeaveBalanceResponse | null>(null);
  readonly requests = signal<LeaveSummaryItem[]>([]);
  readonly loading = signal<boolean>(false);

  ngOnInit(): void {
    this.loadData();
  }

  onYearChange(year: number): void {
    this.selectedYear.set(year);
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    const year = this.selectedYear();

    const balance$ = this.employeeId
      ? this.leaveService.getEmployeeLeaveBalance(this.employeeId, year)
      : this.leaveService.getMyLeaveBalance(year);

    balance$.subscribe({
      next: (b) => {
        this.balance.set(b);
        this.loadRequests(year);
      },
      error: (err) => {
        console.error('Error cargando balance de vacaciones:', err);
        this.loading.set(false);
      }
    });
  }

  private loadRequests(year: number): void {
    if (!this.employeeId) {
      this.leaveService.getMyLeaveRequests(year).subscribe({
        next: (reqs) => {
          this.requests.set(reqs);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    } else {
      this.loading.set(false);
    }
  }
}

