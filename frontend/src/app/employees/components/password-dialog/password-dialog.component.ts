import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

export interface PasswordDialogData {
  fullName: string;
  email: string;
  temporaryPassword?: string;
}

@Component({
  selector: 'app-password-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatSnackBarModule],
  template: `
    <h2 mat-dialog-title>Alta Completada con Éxito</h2>
    <mat-dialog-content class="dialog-content">
      <p>Se ha registrado correctamente el alta de <strong>{{ data.fullName }}</strong> ({{ data.email }}).</p>
      
      <div class="credentials-card">
        <div class="card-label">Contraseña Temporal Generada:</div>
        <div class="password-box">
          <code>{{ data.temporaryPassword || 'No disponible' }}</code>
          <button mat-icon-button (click)="copyPassword()" aria-label="Copiar contraseña al portapapeles" title="Copiar contraseña">
            <mat-icon>content_copy</mat-icon>
          </button>
        </div>
        <small class="hint">Facilite esta clave provisional al empleado. Se le solicitará cambiarla en su primer inicio de sesión.</small>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-flat-button color="primary" [mat-dialog-close]="true">Entendido</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content {
      padding: 1rem 0;
    }
    .credentials-card {
      background: #f4f6f8;
      border: 1px solid #e0e4e8;
      border-radius: 8px;
      padding: 1rem;
      margin-top: 1rem;
    }
    .card-label {
      font-weight: 600;
      font-size: 0.9rem;
      color: #334155;
    }
    .password-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: #ffffff;
      border: 1px dashed #cbd5e1;
      padding: 0.5rem 0.75rem;
      border-radius: 6px;
      margin: 0.5rem 0;
    }
    code {
      font-size: 1.1rem;
      font-weight: 700;
      color: #0f172a;
      letter-spacing: 0.5px;
    }
    .hint {
      color: #64748b;
      font-size: 0.8rem;
    }
  `]
})
export class PasswordDialogComponent {
  readonly data: PasswordDialogData = inject(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<PasswordDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);

  copyPassword(): void {
    if (this.data.temporaryPassword) {
      navigator.clipboard.writeText(this.data.temporaryPassword).then(() => {
        this.snackBar.open('Contraseña copiada al portapapeles', 'Cerrar', { duration: 3000 });
      });
    }
  }
}
