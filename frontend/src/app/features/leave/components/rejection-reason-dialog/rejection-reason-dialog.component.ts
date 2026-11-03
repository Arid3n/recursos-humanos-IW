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
    <h2 mat-dialog-title class="dialog-title dialog-title-danger">
      <mat-icon aria-hidden="true">cancel</mat-icon>
      <span>Motivo del rechazo</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      @if (data.leaveType || data.startDate) {
        <div class="request-summary">
          <span class="type-badge">{{ data.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}</span>
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
          <mat-icon class="reason-icon" aria-hidden="true">info</mat-icon>
          <span class="reason-label">Motivo indicado por tu responsable</span>
        </div>
        <p class="reason-text">{{ data.rejectionReason || 'No se indicó un motivo.' }}</p>
      </div>

      <p class="guidance-text">
        Puedes hacer una nueva solicitud con otras fechas o hablar con tu responsable para coordinar la disponibilidad.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button type="button" [mat-dialog-close]="true" cdkFocusInitial>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .request-summary {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 1rem;
      font-size: 0.9rem;
      color: var(--app-text-muted);
    }

    .type-badge {
      background: var(--app-surface-alt);
      color: var(--app-text);
      font-weight: 600;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      font-size: 0.8rem;
    }

    .dates-info {
      font-weight: 500;
    }

    .reason-card {
      background: var(--app-danger-soft);
      border: 1px solid #fecaca;
      border-left: 4px solid var(--app-danger);
      border-radius: 6px;
      padding: 1rem;
      margin-bottom: 1rem;
    }

    .reason-header {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      color: var(--app-danger);
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
      color: var(--app-text);
      white-space: pre-wrap;
      word-break: break-word;
    }

    .guidance-text {
      font-size: 0.85rem;
      color: var(--app-text-muted);
      margin: 0;
      line-height: 1.4;
    }
  `]
})
export class RejectionReasonDialogComponent {
  readonly data: RejectionReasonDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<RejectionReasonDialogComponent>);
}
