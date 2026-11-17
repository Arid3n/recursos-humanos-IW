import { Component, OnInit, inject, signal, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { EmployeeService } from '../../services/employee.service';
import {
  CreateContractVariationRequest,
  ContractHistoryItemResponse,
  EmployeeDetailResponse
} from '../../models/employee.models';

export interface ContractVariationDialogData {
  employeeId: string;
  currentContract?: ContractHistoryItemResponse;
}

@Component({
  selector: 'app-contract-variation',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatIconModule,
    MatSnackBarModule
  ],
  template: `
    <div class="variation-dialog-container">
      <h2 mat-dialog-title class="dialog-title">
        <mat-icon color="primary">history_edu</mat-icon>
        Registrar Variación Contractual (Append-Only)
      </h2>

      <mat-dialog-content>
        <p class="dialog-intro">
          Las modificaciones de condiciones laborales generan un nuevo registro inmutable en la trayectoria
          con fecha de efectos vinculante (Art. 11 de la Constitución).
        </p>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="row">
            <mat-form-field appearance="outline" class="col">
              <mat-label>Fecha de entrada en vigor (Efectos)</mat-label>
              <input matInput type="date" formControlName="effectiveFrom" required />
              <mat-hint>Admite fechas presentes, futuras o retroactivas.</mat-hint>
              <mat-error *ngIf="form.get('effectiveFrom')?.hasError('required')">La fecha de efectos es obligatoria</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="col">
              <mat-label>Salario bruto anual (€)</mat-label>
              <input matInput type="number" formControlName="salary" min="0" step="100" required />
              <mat-error *ngIf="form.get('salary')?.hasError('required')">El salario es obligatorio</mat-error>
              <mat-error *ngIf="form.get('salary')?.hasError('min')">El salario no puede ser negativo</mat-error>
            </mat-form-field>
          </div>

          <div class="row">
            <mat-form-field appearance="outline" class="col">
              <mat-label>Puesto</mat-label>
              <input matInput formControlName="position" placeholder="Ej. Senior Software Engineer" required />
              <mat-error *ngIf="form.get('position')?.hasError('required')">El puesto es obligatorio</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="col">
              <mat-label>Departamento</mat-label>
              <input matInput formControlName="department" placeholder="Ej. Tecnología" required />
              <mat-error *ngIf="form.get('department')?.hasError('required')">El departamento es obligatorio</mat-error>
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
        </form>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="onCancel()" [disabled]="isSubmitting()">
          Cancelar
        </button>
        <button mat-flat-button color="primary" type="button" (click)="onSubmit()" [disabled]="form.invalid || isSubmitting()">
          <mat-icon>save</mat-icon>
          {{ isSubmitting() ? 'Registrando...' : 'Registrar Variación' }}
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .variation-dialog-container {
      min-width: 480px;
      max-width: 650px;
      padding: 0.5rem;
    }
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.2rem;
      font-weight: 600;
      margin-bottom: 0.5rem;
    }
    .dialog-intro {
      color: #64748b;
      font-size: 0.9rem;
      margin-bottom: 1.25rem;
      line-height: 1.4;
    }
    .row {
      display: flex;
      gap: 1rem;
      margin-bottom: 0.5rem;
      flex-wrap: wrap;
    }
    .col {
      flex: 1 1 calc(50% - 0.5rem);
      min-width: 220px;
    }
  `]
})
export class ContractVariationComponent implements OnInit {
  @Input() employeeIdInput?: string;
  @Input() currentContractInput?: ContractHistoryItemResponse;
  @Output() variationCreated = new EventEmitter<ContractHistoryItemResponse>();

  private readonly fb = inject(FormBuilder);
  private readonly employeeService = inject(EmployeeService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialogRef = inject(MatDialogRef<ContractVariationComponent>, { optional: true });
  private readonly data = inject<ContractVariationDialogData | null>(MAT_DIALOG_DATA, { optional: true });

  form!: FormGroup;
  isSubmitting = signal(false);
  managers = signal<EmployeeDetailResponse[]>([]);

  get resolvedEmployeeId(): string {
    return this.employeeIdInput || this.data?.employeeId || '';
  }

  ngOnInit(): void {
    const current = this.currentContractInput || this.data?.currentContract;

    this.form = this.fb.group({
      effectiveFrom: [new Date().toISOString().substring(0, 10), [Validators.required]],
      position: [current?.position || '', [Validators.required, Validators.maxLength(150)]],
      department: [current?.department || '', [Validators.required, Validators.maxLength(100)]],
      salary: [current?.salary || null, [Validators.required, Validators.min(0)]],
      collectiveAgreementId: [current?.collectiveAgreementId || 1, [Validators.required]],
      managerId: [current?.managerId || null]
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

    const employeeId = this.resolvedEmployeeId;
    if (!employeeId) {
      this.snackBar.open('Error: identificador de empleado no disponible', 'Cerrar', { duration: 4000 });
      return;
    }

    this.isSubmitting.set(true);
    const val = this.form.value;

    const req: CreateContractVariationRequest = {
      effectiveFrom: val.effectiveFrom,
      position: val.position.trim(),
      department: val.department.trim(),
      salary: Number(val.salary),
      collectiveAgreementId: Number(val.collectiveAgreementId),
      managerId: val.managerId || null
    };

    this.employeeService.addContractVariation(employeeId, req).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.snackBar.open('Variación contractual registrada exitosamente en el histórico.', 'OK', { duration: 4000 });
        this.variationCreated.emit(res);
        if (this.dialogRef) {
          this.dialogRef.close(res);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        const detail = err?.error?.detail || 'No se pudo registrar la variación contractual';
        this.snackBar.open(`Error: ${detail}`, 'Cerrar', { duration: 5000 });
      }
    });
  }

  onCancel(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
    }
  }
}
