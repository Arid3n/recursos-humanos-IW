import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeService } from '../../../../employees/services/employee.service';
import { EmployeeProfile } from '../../../leave/models/leave.models';
import { LeaveBalanceWidgetComponent } from '../../../leave/components/leave-balance-widget/leave-balance-widget.component';

@Component({
  selector: 'app-my-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    LeaveBalanceWidgetComponent
  ],
  template: `
    <div class="page-container">
      <div *ngIf="loading()" class="loading-state" role="status">
        <mat-progress-spinner mode="indeterminate" diameter="44"></mat-progress-spinner>
        <span>Cargando tu ficha...</span>
      </div>

      <div *ngIf="!loading() && !profile()" class="alert-banner alert-error" role="alert">
        <mat-icon aria-hidden="true">error_outline</mat-icon>
        <p class="alert-text">No se pudo cargar tu ficha. Por favor, reintenta más tarde.</p>
      </div>

      <ng-container *ngIf="!loading() && profile()">
        <!-- Cabecera -->
        <div class="page-header">
          <div class="page-heading">
            <div class="avatar-circle" aria-hidden="true">
              {{ profile()?.firstName?.charAt(0) }}{{ profile()?.lastName?.charAt(0) }}
            </div>
            <div>
              <h1 class="page-title">{{ profile()?.firstName }} {{ profile()?.lastName }}</h1>
              <p class="page-subtitle">{{ profile()?.currentPosition }} · {{ profile()?.currentDepartment }}</p>
            </div>
          </div>
        </div>

        <!-- Secciones de Datos Personales y Contractuales -->
        <div class="details-grid">
          <!-- Tarjeta 1: Información Personal -->
          <mat-card class="section-card">
            <mat-card-header>
              <mat-card-title class="card-title"><mat-icon aria-hidden="true">badge</mat-icon>Datos Identificativos</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">Nombre Completo</span>
                  <span class="info-value">{{ profile()?.firstName }} {{ profile()?.lastName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">NIF / NIE</span>
                  <span class="info-value">{{ profile()?.nationalId }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Correo Electrónico</span>
                  <span class="info-value">{{ profile()?.email }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Fecha de Incorporación</span>
                  <span class="info-value">{{ profile()?.hireDate | date:'dd/MM/yyyy' }}</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <!-- Tarjeta 2: Régimen Contractual Vigente -->
          <mat-card class="section-card">
            <mat-card-header>
              <mat-card-title class="card-title"><mat-icon aria-hidden="true">work</mat-icon>Situación Contractual</mat-card-title>
            </mat-card-header>
            <mat-card-content>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">Puesto Actual</span>
                  <span class="info-value">{{ profile()?.currentPosition }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Departamento</span>
                  <span class="info-value">{{ profile()?.currentDepartment }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Convenio Colectivo</span>
                  <span class="info-value">{{ profile()?.currentAgreementName }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">Responsable de Equipo</span>
                  <span class="info-value">
                    {{ profile()?.managerFullName || 'Sin asignar' }}
                  </span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Bloque de Saldo de Vacaciones Embebido -->
        <mat-card class="section-card balance-card">
          <mat-card-content>
            <app-leave-balance-widget></app-leave-balance-widget>
          </mat-card-content>
        </mat-card>
      </ng-container>
    </div>
  `,
  styleUrl: './my-profile.component.scss'
})
export class MyProfileComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);

  readonly profile = signal<EmployeeProfile | null>(null);
  readonly loading = signal<boolean>(true);

  ngOnInit(): void {
    this.employeeService.getMyProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error al cargar perfil del empleado:', err);
        this.loading.set(false);
      }
    });
  }
}

