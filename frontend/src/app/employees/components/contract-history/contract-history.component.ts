import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { EmployeeService } from '../../services/employee.service';
import { ContractHistoryItemResponse } from '../../models/employee.models';

@Component({
  selector: 'app-contract-history',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatCardModule, MatChipsModule],
  template: `
    <mat-card class="history-card">
      <mat-card-header>
        <mat-card-title>Trayectoria Contractual (Inmutable)</mat-card-title>
        <mat-card-subtitle>Histórico cronológico de variaciones de puesto, departamento y condiciones salariales</mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        <table mat-table [dataSource]="history" class="mat-elevation-z1">

          <ng-container matColumnDef="effectiveFrom">
            <th mat-header-cell *matHeaderCellDef>Fecha de Efectos</th>
            <td mat-cell *matCellDef="let row">
              <strong>{{ row.effectiveFrom | date:'dd/MM/yyyy' }}</strong>
            </td>
          </ng-container>

          <ng-container matColumnDef="position">
            <th mat-header-cell *matHeaderCellDef>Puesto</th>
            <td mat-cell *matCellDef="let row">{{ row.position }}</td>
          </ng-container>

          <ng-container matColumnDef="department">
            <th mat-header-cell *matHeaderCellDef>Departamento</th>
            <td mat-cell *matCellDef="let row">{{ row.department }}</td>
          </ng-container>

          <ng-container matColumnDef="salary">
            <th mat-header-cell *matHeaderCellDef>Salario Bruto</th>
            <td mat-cell *matCellDef="let row">{{ row.salary | currency:'EUR':'symbol':'1.2-2':'es' }}</td>
          </ng-container>

          <ng-container matColumnDef="collectiveAgreement">
            <th mat-header-cell *matHeaderCellDef>Convenio</th>
            <td mat-cell *matCellDef="let row">{{ row.collectiveAgreementName }}</td>
          </ng-container>

          <ng-container matColumnDef="manager">
            <th mat-header-cell *matHeaderCellDef>Responsable</th>
            <td mat-cell *matCellDef="let row">{{ row.managerName || 'Sin asignar / CEO' }}</td>
          </ng-container>

          <ng-container matColumnDef="audit">
            <th mat-header-cell *matHeaderCellDef>Registrado el</th>
            <td mat-cell *matCellDef="let row">
              <small>{{ row.createdAt | date:'dd/MM/yyyy HH:mm' }} (por {{ row.createdBy }})</small>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>

        <div *ngIf="history.length === 0" class="empty-state">
          No existen registros contractuales disponibles.
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .history-card {
      margin-top: 1.5rem;
      border-radius: 12px;
    }
    table {
      width: 100%;
      margin-top: 1rem;
    }
    .empty-state {
      padding: 2rem;
      text-align: center;
      color: #64748b;
    }
  `]
})
export class ContractHistoryComponent implements OnInit {
  @Input({ required: true }) employeeId!: string;
  private readonly employeeService = inject(EmployeeService);

  history: ContractHistoryItemResponse[] = [];
  displayedColumns: string[] = ['effectiveFrom', 'position', 'department', 'salary', 'collectiveAgreement', 'manager', 'audit'];

  ngOnInit(): void {
    if (this.employeeId) {
      this.loadHistory();
    }
  }

  loadHistory(): void {
    this.employeeService.getContractHistory(this.employeeId).subscribe({
      next: (data) => (this.history = data),
      error: () => (this.history = [])
    });
  }
}
