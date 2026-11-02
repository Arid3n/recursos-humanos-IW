import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { LeaveRequestSummary } from '../../models/leave.models';

export interface CancelConfirmDialogData {
  request: LeaveRequestSummary;
}

@Component({
  selector: 'app-cancel-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon class="warn-icon">warning_amber</mat-icon>
      <span>¿Cancelar esta solicitud?</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      <p>Estás a punto de cancelar tu solicitud de 
        <strong>{{ data.request.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}</strong> 
        del <strong>{{ data.request.startDate | date:'dd/MM/yyyy' }}</strong> al 
        <strong>{{ data.request.endDate | date:'dd/MM/yyyy' }}</strong> ({{ data.request.calculatedDays }} días).
      </p>
      <p class="warning-note">
        Esta acción retirará la solicitud de la bandeja de tu responsable y liberará los días sin penalización. Esta operación no se puede deshacer.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button [mat-dialog-close]="false">
        Mantener Solicitud
      </button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">
        Sí, Cancelar Solicitud
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.25rem;
      color: #0f172a;
      margin: 0;
    }
    .warn-icon {
      color: #ea580c;
    }
    .dialog-content {
      padding: 1rem 0;
      color: #334155;
      font-size: 0.95rem;
      line-height: 1.5;
    }
    .warning-note {
      font-size: 0.85rem;
      color: #64748b;
      background: #fff7ed;
      border-left: 3px solid #f97316;
      padding: 0.5rem 0.75rem;
      border-radius: 4px;
      margin-top: 0.75rem;
    }
    .dialog-actions {
      gap: 0.5rem;
    }
  `]
})
export class CancelConfirmDialogComponent {
  readonly data: CancelConfirmDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<CancelConfirmDialogComponent>);
}
