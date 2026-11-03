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
    <h2 mat-dialog-title class="dialog-title dialog-title-danger">
      <mat-icon aria-hidden="true">warning_amber</mat-icon>
      <span>¿Cancelar esta solicitud?</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      <p>Estás a punto de cancelar tu solicitud de 
        <strong>{{ data.request.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}</strong> 
        del <strong>{{ data.request.startDate | date:'dd/MM/yyyy' }}</strong> al 
        <strong>{{ data.request.endDate | date:'dd/MM/yyyy' }}</strong> ({{ data.request.calculatedDays }} días).
      </p>
      <p class="warning-note">
        La solicitud se retirará de la bandeja de tu responsable y los días quedarán libres. Esta acción no se puede deshacer.
      </p>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button [mat-dialog-close]="false">Volver</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">
        <mat-icon>close</mat-icon>
        Confirmar cancelación
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .warning-note {
      font-size: 0.85rem;
      color: #713f12;
      background: var(--app-warning-soft);
      border-left: 3px solid var(--app-warning);
      padding: 0.5rem 0.75rem;
      border-radius: 4px;
      margin-top: 0.75rem;
    }
  `]
})
export class CancelConfirmDialogComponent {
  readonly data: CancelConfirmDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<CancelConfirmDialogComponent>);
}
