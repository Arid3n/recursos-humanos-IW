import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TeamCalendarEvent } from '../../models/team-calendar.models';

@Component({
  selector: 'app-absence-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <h2 mat-dialog-title class="dialog-title">
      <mat-icon aria-hidden="true">{{ data.leaveType === 'VACATION' ? 'beach_access' : 'event_note' }}</mat-icon>
      <span>Detalle de la ausencia</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      <dl class="detail-list">
        <dt>Empleado</dt>
        <dd>{{ data.employeeName }}</dd>
        <dt>Tipo</dt>
        <dd>{{ data.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}</dd>
        <dt>Fechas</dt>
        <dd>Del {{ data.startDate | date:'dd/MM/yyyy' }} al {{ data.endDate | date:'dd/MM/yyyy' }}</dd>
        <dt>Estado</dt>
        <dd>
          <span class="status-badge" [class.status-approved]="isApproved" [class.status-pending]="!isApproved">
            <mat-icon aria-hidden="true">{{ isApproved ? 'check_circle' : 'schedule' }}</mat-icon>
            {{ isApproved ? 'Aprobada' : 'Pendiente' }}
          </span>
        </dd>
      </dl>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button type="button" mat-dialog-close cdkFocusInitial>Cerrar</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .detail-list { display: grid; grid-template-columns: max-content 1fr; gap: 0.5rem 1rem; margin: 0; }
    .detail-list dt { font-weight: 600; color: var(--app-text-muted); }
    .detail-list dd { margin: 0; }

  `]
})
export class AbsenceDetailDialogComponent {
  readonly data: TeamCalendarEvent = inject(MAT_DIALOG_DATA);
  readonly isApproved = this.data.status === 'APPROVED';
}
