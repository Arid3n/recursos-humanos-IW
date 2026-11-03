import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TeamLeaveRequest } from '../../models/team-leave.models';

export type ResolveMode = 'approve' | 'reject';

export interface ResolveRequestDialogData {
  mode: ResolveMode;
  request: TeamLeaveRequest;
}

/** Resultado al confirmar: texto del comentario/motivo (null si se aprueba sin comentario). */
export interface ResolveRequestDialogResult {
  text: string | null;
}

export const REASON_MIN_LENGTH = 5;
export const REASON_MAX_LENGTH = 500;

@Component({
  selector: 'app-resolve-request-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title class="dialog-title" [class.dialog-title-danger]="!isApprove">
      <mat-icon aria-hidden="true">
        {{ isApprove ? 'check_circle' : 'cancel' }}
      </mat-icon>
      <span>{{ isApprove ? 'Aprobar solicitud' : 'Rechazar solicitud' }}</span>
    </h2>

    <mat-dialog-content class="dialog-content">
      <p>
        <strong>{{ data.request.employeeName }}</strong> ·
        {{ data.request.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso' }}
        del <strong>{{ data.request.startDate | date:'dd/MM/yyyy' }}</strong>
        al <strong>{{ data.request.endDate | date:'dd/MM/yyyy' }}</strong>
        ({{ data.request.calculatedDays }} {{ data.request.calculatedDays === 1 ? 'día' : 'días' }}).
      </p>

      <mat-form-field appearance="outline" class="full-width">
        <mat-label>{{ isApprove ? 'Comentario (opcional)' : 'Motivo del rechazo' }}</mat-label>
        <textarea
          matInput
          cdkFocusInitial
          rows="4"
          [formControl]="textControl"
          [attr.maxlength]="maxLength"
          [required]="!isApprove"></textarea>
        <mat-hint align="end">{{ textControl.value.length }} / {{ maxLength }}</mat-hint>
        @if (textControl.hasError('required')) {
          <mat-error>El motivo del rechazo es obligatorio.</mat-error>
        }
        @if (textControl.hasError('minlength')) {
          <mat-error>El motivo debe tener al menos {{ minLength }} caracteres.</mat-error>
        }
        @if (textControl.hasError('maxlength')) {
          <mat-error>El texto no puede superar {{ maxLength }} caracteres.</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button type="button" mat-dialog-close>Volver</button>
      <button mat-flat-button type="button" [color]="isApprove ? 'primary' : 'warn'" (click)="confirm()">
        <mat-icon>{{ isApprove ? 'check' : 'close' }}</mat-icon>
        {{ isApprove ? 'Confirmar aprobación' : 'Confirmar rechazo' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width { margin-top: 0.5rem; }
  `]
})
export class ResolveRequestDialogComponent {
  readonly data: ResolveRequestDialogData = inject(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<ResolveRequestDialogComponent, ResolveRequestDialogResult>);

  readonly isApprove = this.data.mode === 'approve';
  readonly minLength = REASON_MIN_LENGTH;
  readonly maxLength = REASON_MAX_LENGTH;

  readonly textControl = new FormControl<string>('', {
    nonNullable: true,
    validators: this.isApprove
      ? [Validators.maxLength(REASON_MAX_LENGTH)]
      : [Validators.required, Validators.minLength(REASON_MIN_LENGTH), Validators.maxLength(REASON_MAX_LENGTH)]
  });

  confirm(): void {
    // Se valida sobre el texto sin espacios extremos, igual que el servidor
    this.textControl.setValue(this.textControl.value.trim());
    if (this.textControl.invalid) {
      this.textControl.markAsTouched();
      return;
    }
    this.dialogRef.close({ text: this.textControl.value || null });
  }
}
