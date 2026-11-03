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
            <mat-icon aria-hidden="true">groups</mat-icon>
          </div>
          <h1 class="brand-title">Gestión de Recursos Humanos</h1>
          <p class="brand-subtitle">Portal del empleado</p>
        </div>

        <mat-card class="login-card">
          <mat-card-header>
            <mat-card-title>Acceso al Sistema</mat-card-title>
            <mat-card-subtitle>Introduce tus credenciales para identificarte</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            @if (errorMessage()) {
              <div class="alert-banner alert-error" role="alert">
                <mat-icon aria-hidden="true">error_outline</mat-icon>
                <span class="alert-text">{{ errorMessage() }}</span>
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
                  [attr.aria-label]="hidePassword() ? 'Mostrar contraseña' : 'Ocultar contraseña'"
                >
                  <mat-icon>{{ hidePassword() ? 'visibility' : 'visibility_off' }}</mat-icon>
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
                <mat-icon aria-hidden="true">key</mat-icon>
                <span>Usar rrhh@empresa.com (admin123)</span>
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
        background: var(--app-primary);
        color: #ffffff;
        border-radius: 16px;
        margin-bottom: 0.75rem;

        mat-icon {
          font-size: 34px;
          width: 34px;
          height: 34px;
        }
      }

      .brand-title {
        font-size: 1.6rem;
        font-weight: 600;
        color: var(--app-text);
        margin: 0;
      }

      .brand-subtitle {
        font-size: 0.9rem;
        color: var(--app-text-muted);
        margin-top: 0.25rem;
      }
    }

    .login-card {
      border-radius: var(--app-radius);
      box-shadow: var(--app-shadow);
      border: 1px solid var(--app-border);
      padding: 1rem;
      background: var(--app-surface);

      mat-card-header {
        padding-bottom: 1.25rem;
      }

      mat-card-title {
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--app-text);
      }

      mat-card-subtitle {
        color: var(--app-text-muted);
        font-size: 0.875rem;
      }
    }

    .alert-banner {
      margin-bottom: 1.25rem;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .submit-btn {
      width: 100%;
      height: 48px;
      border-radius: 8px;
      font-size: 1rem;
      margin-top: 0.5rem;
    }

    .demo-box {
      margin-top: 1.5rem;
      padding-top: 1.25rem;
      border-top: 1px solid var(--app-border);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;

      .demo-title {
        font-size: 0.8rem;
        color: var(--app-text-muted);
        font-weight: 500;
      }

      .demo-chip {
        display: inline-flex;
        align-items: center;
        gap: 0.4rem;
        padding: 0.4rem 0.8rem;
        background: var(--app-surface);
        border: 1px solid var(--app-border);
        border-radius: 9999px;
        font: inherit;
        font-size: 0.8rem;
        color: var(--app-text-body);
        cursor: pointer;
        transition: all 0.2s;

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
        }

        &:hover {
          background: var(--app-primary-soft);
          border-color: var(--app-primary-border);
          color: var(--app-primary);
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
          this.router.navigate(['/profile']);
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
          this.errorMessage.set('No se pudo conectar con el servidor. Comprueba que el backend esté activo.');
        }
      }
    });
  }
}
