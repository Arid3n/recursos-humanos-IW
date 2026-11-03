import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { EmployeeService } from '../../services/employee.service';
import { EmployeeDetailResponse } from '../../models/employee.models';
import { PasswordDialogComponent } from '../password-dialog/password-dialog.component';

@Component({
  selector: 'app-employee-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  template: `
    <div class="page-container create-container">
      <div class="page-header">
        <div class="page-heading">
          <div class="page-icon">
            <mat-icon>person_add</mat-icon>
          </div>
          <div>
            <h1 class="page-title">Alta de Empleado</h1>
            <p class="page-subtitle">Introduce los datos personales y las condiciones contractuales iniciales.</p>
          </div>
        </div>
      </div>

      <mat-card class="form-card surface-card">
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            
            <h2 class="form-section-title">1. Datos personales</h2>
            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Nombre</mat-label>
                <input matInput formControlName="firstName" placeholder="Ej. Ana" required />
                <mat-error *ngIf="form.get('firstName')?.hasError('required')">El nombre es obligatorio</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Apellidos</mat-label>
                <input matInput formControlName="lastName" placeholder="Ej. Gómez Ruiz" required />
                <mat-error *ngIf="form.get('lastName')?.hasError('required')">Los apellidos son obligatorios</mat-error>
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>NIF / NIE</mat-label>
                <input matInput formControlName="nationalId" placeholder="Ej. 12345678Z" required />
                <mat-error *ngIf="form.get('nationalId')?.hasError('required')">El NIF/NIE es obligatorio</mat-error>
                <mat-error *ngIf="form.get('nationalId')?.hasError('pattern')">Formato de DNI/NIE no válido</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Correo electrónico corporativo</mat-label>
                <input matInput type="email" formControlName="email" placeholder="ana.gomez@empresa.com" required />
                <mat-error *ngIf="form.get('email')?.hasError('required')">El correo electrónico es obligatorio</mat-error>
                <mat-error *ngIf="form.get('email')?.hasError('email')">Formato de correo no válido</mat-error>
              </mat-form-field>
            </div>

            <h2 class="form-section-title">2. Condiciones contractuales iniciales</h2>
            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Fecha de incorporación</mat-label>
                <input matInput type="date" formControlName="hireDate" required />
                <mat-error *ngIf="form.get('hireDate')?.hasError('required')">La fecha es obligatoria</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Puesto</mat-label>
                <input matInput formControlName="position" placeholder="Ej. Ingeniero/a de Software" required />
                <mat-error *ngIf="form.get('position')?.hasError('required')">El puesto es obligatorio</mat-error>
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Departamento</mat-label>
                <input matInput formControlName="department" placeholder="Ej. Tecnología" required />
                <mat-error *ngIf="form.get('department')?.hasError('required')">El departamento es obligatorio</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Salario bruto anual (€)</mat-label>
                <input matInput type="number" formControlName="salary" placeholder="Ej. 36000.00" min="0" step="100" required />
                <mat-error *ngIf="form.get('salary')?.hasError('required')">El salario es obligatorio</mat-error>
                <mat-error *ngIf="form.get('salary')?.hasError('min')">El salario no puede ser negativo</mat-error>
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Convenio colectivo</mat-label>
                <mat-select formControlName="collectiveAgreementId" required>
                  <mat-option [value]="1">Convenio General (22 días/año)</mat-option>
                  <mat-option [value]="2">Convenio TIC (23 días/año)</mat-option>
                </mat-select>
                <mat-error *ngIf="form.get('collectiveAgreementId')?.hasError('required')">Selecciona un convenio</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Responsable de equipo</mat-label>
                <mat-select formControlName="managerId">
                  <mat-option [value]="null">Sin responsable (solo Dirección General)</mat-option>
                  @for (m of managers(); track m.id) {
                    <mat-option [value]="m.id">{{ m.firstName }} {{ m.lastName }} ({{ m.email }})</mat-option>
                  }
                </mat-select>
              </mat-form-field>
            </div>

            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || isSubmitting()">
                <mat-icon>person_add</mat-icon>
                {{ isSubmitting() ? 'Guardando...' : 'Dar de Alta' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .create-container {
      max-width: 900px;
      margin: 0 auto;
    }
    .form-card {
      padding: 1rem;
    }
    .form-section-title {
      font-size: 1rem;
      font-weight: 600;
      color: var(--app-text);
      margin: 1.5rem 0 1rem;
      border-bottom: 1px solid var(--app-border);
      padding-bottom: 0.5rem;

      &:first-of-type {
        margin-top: 0;
      }
    }
    .row {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }
    .col {
      flex: 1 1 calc(50% - 0.5rem);
      min-width: 260px;
    }
    .actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 1.5rem;
    }
  `]
})
export class EmployeeCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  form!: FormGroup;
  isSubmitting = signal(false);
  managers = signal<EmployeeDetailResponse[]>([]);

  ngOnInit(): void {
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(150)]],
      nationalId: ['', [Validators.required, Validators.pattern(/^[0-9]{8}[A-Za-z]$|^[XYZxyz][0-9]{7}[A-Za-z]$/)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      hireDate: [new Date().toISOString().substring(0, 10), [Validators.required]],
      position: ['', [Validators.required, Validators.maxLength(150)]],
      department: ['', [Validators.required, Validators.maxLength(100)]],
      salary: [null, [Validators.required, Validators.min(0)]],
      collectiveAgreementId: [1, [Validators.required]],
      managerId: [null]
    });

    this.employeeService.listEmployees(true, 0, 100).subscribe({
      next: (res) => this.managers.set(res?.content || []),
      error: () => this.managers.set([])
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    const val = this.form.value;

    this.employeeService.createEmployee({
      firstName: val.firstName.trim(),
      lastName: val.lastName.trim(),
      nationalId: val.nationalId.trim().toUpperCase(),
      email: val.email.trim().toLowerCase(),
      hireDate: val.hireDate,
      position: val.position.trim(),
      department: val.department.trim(),
      salary: Number(val.salary),
      collectiveAgreementId: Number(val.collectiveAgreementId),
      managerId: val.managerId ? val.managerId : null
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        const dialogRef = this.dialog.open(PasswordDialogComponent, {
          width: '480px',
          data: {
            fullName: res.fullName,
            email: res.email,
            temporaryPassword: res.temporaryPassword
          },
          disableClose: true
        });

        dialogRef.afterClosed().subscribe(() => {
          this.router.navigate(['/employees/new']);
        });
      },
      error: (err) => {
        this.isSubmitting.set(false);
        if (err.status === 400 && err.error?.invalidFields) {
          const fields = err.error.invalidFields;
          for (const key of Object.keys(fields)) {
            const ctrl = this.form.get(key);
            if (ctrl) {
              ctrl.setErrors({ serverError: fields[key] });
            }
          }
        }
        this.snackBar.open(err.error?.detail || 'No se pudo guardar el empleado', 'Cerrar', {
          duration: 4000
        });
      }
    });
  }
}
