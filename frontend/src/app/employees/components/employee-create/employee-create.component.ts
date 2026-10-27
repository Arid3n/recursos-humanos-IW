import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { EmployeeService } from '../../services/employee.service';
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
    MatSelectModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  template: `
    <div class="create-container">
      <mat-card class="form-card">
        <mat-card-header>
          <mat-card-title>Alta de Nuevo Empleado</mat-card-title>
          <mat-card-subtitle>Introduzca los datos personales y las condiciones contractuales iniciales</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            
            <h3>1. Datos Personales e Identificativos</h3>
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
                <mat-label>Email Corporativo</mat-label>
                <input matInput type="email" formControlName="email" placeholder="ana.gomez@empresa.com" required />
                <mat-error *ngIf="form.get('email')?.hasError('required')">El email es obligatorio</mat-error>
                <mat-error *ngIf="form.get('email')?.hasError('email')">Formato de correo no válido</mat-error>
              </mat-form-field>
            </div>

            <h3>2. Condiciones Contractuales Iniciales (Vigentes)</h3>
            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Fecha de Incorporación / Entrada en Vigor</mat-label>
                <input matInput type="date" formControlName="hireDate" required />
                <mat-error *ngIf="form.get('hireDate')?.hasError('required')">La fecha es obligatoria</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Puesto de Trabajo</mat-label>
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
                <mat-label>Salario Bruto Anual (€)</mat-label>
                <input matInput type="number" formControlName="salary" placeholder="Ej. 36000.00" min="0" step="100" required />
                <mat-error *ngIf="form.get('salary')?.hasError('required')">El salario es obligatorio</mat-error>
                <mat-error *ngIf="form.get('salary')?.hasError('min')">El salario no puede ser negativo</mat-error>
              </mat-form-field>
            </div>

            <div class="row">
              <mat-form-field appearance="outline" class="col">
                <mat-label>Convenio Colectivo</mat-label>
                <mat-select formControlName="collectiveAgreementId" required>
                  <mat-option [value]="1">Convenio General (22 días/año)</mat-option>
                  <mat-option [value]="2">Convenio TIC (23 días/año)</mat-option>
                </mat-select>
                <mat-error *ngIf="form.get('collectiveAgreementId')?.hasError('required')">Debe seleccionar un convenio</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="col">
                <mat-label>Responsable de Equipo (Opcional solo para CEO)</mat-label>
                <input matInput formControlName="managerId" placeholder="UUID del responsable (si aplica)" />
              </mat-form-field>
            </div>

            <div class="actions">
              <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || isSubmitting()">
                {{ isSubmitting() ? 'Guardando...' : 'Dar de Alta Empleado' }}
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
      margin: 2rem auto;
      padding: 0 1rem;
    }
    .form-card {
      padding: 1.5rem;
      border-radius: 12px;
    }
    h3 {
      font-size: 1rem;
      font-weight: 600;
      color: #1e293b;
      margin: 1.5rem 0 1rem;
      border-bottom: 1px solid #e2e8f0;
      padding-bottom: 0.5rem;
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
      managerId: val.managerId ? val.managerId.trim() : null
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        const dialogRef = this.dialog.open(PasswordDialogComponent, {
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
        this.snackBar.open(err.error?.detail || 'Error al guardar el empleado', 'Cerrar', {
          duration: 4000
        });
      }
    });
  }
}
