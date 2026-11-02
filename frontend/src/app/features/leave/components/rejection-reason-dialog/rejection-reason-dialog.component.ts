import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface RejectionReasonDialogData {
  rejectionReason: string | null;
  leaveType?: string;
  startDate?: string;
  endDate?: string;
  calculatedDays?: number;
}

@Component({
  selector: 'app-rejection-reason-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon class="title-icon" color="warn">cancel</mat-icon>
      <span>Motivo de Denegación</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      @if (data.leaveType || data.startDate) {
        <div class="request-summary">
          <span class="type-badge">{{ data.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso Retribuido' }}</span>
          @if (data.startDate && data.endDate) {
            <span class="dates-info">Del {{ data.startDate | date:'dd/MM/yyyy' }} al {{ data.endDate | date:'dd/MM/yyyy' }}</span>
          }
          @if (data.calculatedDays) {
            <span class="days-info">({{ data.calculatedDays }} días laborables)</span>
          }
        </div>
      }

      <div class="reason-card">
        <div class="reason-header">
          <mat-icon class="reason-icon">info</mat-icon>
          <span class="reason-label">Justificación del Responsable:</span>
        </div>
        <p class="reason-text">{{ data.rejectionReason || 'No se ha indicado un motivo específico para la denegación.' }}</p>
      </div>

      <p class="guidance-text">
        Si lo necesitas, puedes realizar una nueva solicitud adaptando las fechas o consultar con tu responsable de equipo para coordinar la disponibilidad.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" [mat-dialog-close]="true">
        Entendido
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.25rem;
      font-weight: 600;
      color: #1e293b;
      margin: 0 0 0.5rem 0;
    }

    .title-icon {
      color: #dc2626;
    }

    .dialog-content {
      padding: 0.5rem 0 1rem 0;
      color: #334155;
    }

    .request-summary {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
      font-size: 0.9rem;
      color: #64748b;
    }

    .type-badge {
      background: #f1f5f9;
      color: #0f172a;
      font-weight: 600;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.8rem;
    }

    .dates-info {
      font-weight: 500;
    }

    .reason-card {
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-left: 4px solid #ef4444;
      border-radius: 6px;
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .reason-header {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      color: #991b1b;
      font-weight: 600;
      font-size: 0.9rem;
      margin-bottom: 0.5rem;
    }

    .reason-icon {
      font-size: 1.2rem;
      height: 1.2rem;
      width: 1.2rem;
    }

    .reason-text {
      margin: 0;
      font-size: 0.95rem;
      line-height: 1.5;
      color: #7f1d1d;
      white-space: pre-wrap;
      word-break: break-word;
    }

    .guidance-text {
      font-size: 0.85rem;
      color: #64748b;
      margin: 0;
      line-height: 1.4;
    }
  `]
})
export class RejectionReasonDialogComponent {
  readonly data: RejectionReasonDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<RejectionReasonDialogComponent>);
}
