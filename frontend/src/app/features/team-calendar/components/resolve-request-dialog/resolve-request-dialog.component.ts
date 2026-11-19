import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormControl, ReactiveFormsModule, ValidationErrors, ValidatorFn } from '@angular/forms';
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

/** Textos del diálogo agrupados en un único sitio (Art. 21.3); el proyecto aún no usa i18n de Angular. */
export const RESOLVE_DIALOG_TEXTS = {
  approveTitle: 'Aprobar solicitud',
  rejectTitle: 'Rechazar solicitud',
  approveLabel: 'Comentario (opcional)',
  rejectLabel: 'Motivo del rechazo',
  back: 'Volver',
  confirmApprove: 'Confirmar aprobación',
  confirmReject: 'Confirmar rechazo',
  reasonRequired: 'El motivo del rechazo es obligatorio.',
  reasonTooShort: (min: number) => `El motivo debe tener al menos ${min} caracteres.`,
  textTooLong: (max: number) => `El texto no puede superar ${max} caracteres.`
} as const;

/** Valida sobre el texto sin espacios extremos, igual que el servidor (FR-003). */
const trimmedRequired: ValidatorFn = (control: AbstractControl): ValidationErrors | null =>
  String(control.value ?? '').trim().length === 0 ? { required: true } : null;

const trimmedMinLength = (min: number): ValidatorFn => (control) => {
  const length = String(control.value ?? '').trim().length;
  return length > 0 && length < min ? { minlength: { requiredLength: min, actualLength: length } } : null;
};

const trimmedMaxLength = (max: number): ValidatorFn => (control) => {
  const length = String(control.value ?? '').trim().length;
  return length > max ? { maxlength: { requiredLength: max, actualLength: length } } : null;
};

@Component({
  selector: 'app-resolve-request-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title class="dialog-title" [class.dialog-title-danger]="!isApprove">
      <mat-icon aria-hidden="true">
        {{ isApprove ? 'check_circle' : 'cancel' }}
      </mat-icon>
      <span>{{ isApprove ? texts.approveTitle : texts.rejectTitle }}</span>
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
        <mat-label>{{ isApprove ? texts.approveLabel : texts.rejectLabel }}</mat-label>
        <textarea
          matInput
          cdkFocusInitial
          rows="4"
          [formControl]="textControl"
          [required]="!isApprove"></textarea>
        <mat-hint align="end">{{ textControl.value.trim().length }} / {{ maxLength }}</mat-hint>
        @if (textControl.hasError('required')) {
          <mat-error>{{ texts.reasonRequired }}</mat-error>
        }
        @if (textControl.hasError('minlength')) {
          <mat-error>{{ texts.reasonTooShort(minLength) }}</mat-error>
        }
        @if (textControl.hasError('maxlength')) {
          <mat-error>{{ texts.textTooLong(maxLength) }}</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>

    <mat-dialog-actions align="end" class="dialog-actions">
      <button mat-button type="button" mat-dialog-close>{{ texts.back }}</button>
      <button mat-flat-button type="button" [color]="isApprove ? 'primary' : 'warn'" (click)="confirm()">
        <mat-icon>{{ isApprove ? 'check' : 'close' }}</mat-icon>
        {{ isApprove ? texts.confirmApprove : texts.confirmReject }}
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

  readonly texts = RESOLVE_DIALOG_TEXTS;
  readonly isApprove = this.data.mode === 'approve';
  readonly minLength = REASON_MIN_LENGTH;
  readonly maxLength = REASON_MAX_LENGTH;

  readonly textControl = new FormControl<string>('', {
    nonNullable: true,
    validators: this.isApprove
      ? [trimmedMaxLength(REASON_MAX_LENGTH)]
      : [trimmedRequired, trimmedMinLength(REASON_MIN_LENGTH), trimmedMaxLength(REASON_MAX_LENGTH)]
  });

  confirm(): void {
    this.textControl.markAsTouched();
    if (this.textControl.invalid) {
      return;
    }
    this.dialogRef.close({ text: this.textControl.value.trim() || null });
  }
}
