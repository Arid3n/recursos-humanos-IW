import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-page">
      <div class="login-card-container">
        <!-- Logo / Header -->
        <div class="brand-header">
          <div class="brand-icon-wrapper">
            <span class="brand-icon">💼</span>
          </div>
          <h1 class="brand-title">Recursos Humanos</h1>
          <p class="brand-subtitle">Portal de Gestión y Trayectoria Laboral</p>
        </div>

        <mat-card class="login-card">
          <mat-card-header>
            <mat-card-title>Acceso al Sistema</mat-card-title>
            <mat-card-subtitle>Introduce tus credenciales para identificarte</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            @if (errorMessage()) {
              <div class="error-banner">
                <span class="error-icon">⚠️</span>
                <span class="error-text">{{ errorMessage() }}</span>
              </div>
            }

            <form [formGroup]="form" (ngSubmit)="onSubmit()" class="login-form">
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Usuario o Correo Electrónico</mat-label>
                <input
                  matInput
                  type="text"
                  formControlName="username"
                  placeholder="ej. rrhh@empresa.com"
                  autocomplete="username"
                  required
                />
                <mat-error *ngIf="form.get('username')?.hasError('required')">
                  El usuario es obligatorio
                </mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Contraseña</mat-label>
                <input
                  matInput
                  [type]="hidePassword() ? 'password' : 'text'"
                  formControlName="password"
                  placeholder="Introduce tu contraseña"
                  autocomplete="current-password"
                  required
                />
                <button
                  mat-icon-button
                  matSuffix
                  type="button"
                  (click)="hidePassword.set(!hidePassword())"
                  [attr.aria-label]="'Ocultar contraseña'"
                >
                  <span>{{ hidePassword() ? '👁️' : '🔒' }}</span>
                </button>
                <mat-error *ngIf="form.get('password')?.hasError('required')">
                  La contraseña es obligatoria
                </mat-error>
              </mat-form-field>

              <button
                mat-flat-button
                color="primary"
                type="submit"
                class="submit-btn"
                [disabled]="form.invalid || loading()"
              >
                @if (loading()) {
                  <span class="spinner-inline">Iniciando sesión...</span>
                } @else {
                  <span>Iniciar Sesión</span>
                }
              </button>
            </form>

            <div class="demo-box">
              <span class="demo-title">Credenciales de prueba (RRHH):</span>
              <button
                type="button"
                class="demo-chip"
                (click)="fillDemoCredentials('rrhh@empresa.com', 'admin123')"
              >
                <span>🔑 Usar rrhh@empresa.com (admin123)</span>
              </button>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: calc(100vh - 120px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1.5rem;
      background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
    }

    .login-card-container {
      width: 100%;
      max-width: 440px;
      animation: fadeIn 0.4s ease-out;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(12px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .brand-header {
      text-align: center;
      margin-bottom: 1.5rem;

      .brand-icon-wrapper {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 64px;
        height: 64px;
        background: white;
        border-radius: 16px;
        box-shadow: 0 4px 12px rgba(30, 41, 59, 0.08);
        margin-bottom: 0.75rem;

        .brand-icon {
          font-size: 2rem;
        }
      }

      .brand-title {
        font-size: 1.6rem;
        font-weight: 700;
        color: #0f172a;
        margin: 0;
        letter-spacing: -0.02em;
      }

      .brand-subtitle {
        font-size: 0.9rem;
        color: #64748b;
        margin-top: 0.25rem;
      }
    }

    .login-card {
      border-radius: 16px;
      box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.1), 0 8px 10px -6px rgba(15, 23, 42, 0.05);
      border: 1px solid #e2e8f0;
      padding: 1rem;
      background: #ffffff;

      mat-card-header {
        padding-bottom: 1.25rem;
      }

      mat-card-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: #1e293b;
      }

      mat-card-subtitle {
        color: #64748b;
        font-size: 0.875rem;
      }
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1.25rem;
      font-size: 0.875rem;

      .error-icon {
        font-size: 1.1rem;
      }
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .full-width {
      width: 100%;
    }

    .submit-btn {
      width: 100%;
      height: 48px;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      letter-spacing: 0.01em;
      margin-top: 0.5rem;
      background: #2563eb !important;
      color: white !important;
      transition: background-color 0.2s, transform 0.1s;

      &:hover:not(:disabled) {
        background: #1d4ed8 !important;
        transform: translateY(-1px);
      }

      &:disabled {
        background: #94a3b8 !important;
        color: #f1f5f9 !important;
      }
    }

    .demo-box {
      margin-top: 1.5rem;
      padding-top: 1.25rem;
      border-top: 1px dashed #cbd5e1;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;

      .demo-title {
        font-size: 0.8rem;
        color: #64748b;
        font-weight: 500;
      }

      .demo-chip {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.4rem 0.8rem;
        background: #f1f5f9;
        border: 1px solid #cbd5e1;
        border-radius: 9999px;
        font-size: 0.8rem;
        color: #334155;
        cursor: pointer;
        transition: all 0.2s;

        &:hover {
          background: #e2e8f0;
          border-color: #94a3b8;
          color: #0f172a;
        }
      }
    }
  `]
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly hidePassword = signal(true);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  fillDemoCredentials(u: string, p: string): void {
    this.form.patchValue({ username: u, password: p });
    this.errorMessage.set(null);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(this.form.value).subscribe({
      next: (user) => {
        this.loading.set(false);
        // Navigate to appropriate landing page
        if (user.role === 'RRHH') {
          this.router.navigate(['/employees/new']);
        } else {
          this.router.navigate(['/employees']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Usuario o contraseña incorrectos.');
        } else if (err.status === 403) {
          this.errorMessage.set('Cuenta desactivada o sin permisos.');
        } else if (err.error?.detail) {
          this.errorMessage.set(err.error.detail);
        } else {
          this.errorMessage.set('Error al conectar con el servidor. Verifique que el backend esté activo.');
        }
      }
    });
  }
}
