import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { LeaveService } from '../../services/leave.service';
import { CreateLeaveRequest, LeaveBalanceResponse, LeaveType, WorkingDaysCalculationResponse } from '../../models/leave.models';

@Component({
  selector: 'app-leave-request-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './leave-request-create.component.html',
  styleUrls: ['./leave-request-create.component.scss']
})
export class LeaveRequestCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly leaveService = inject(LeaveService);

  readonly balance = signal<LeaveBalanceResponse | null>(null);
  readonly loadingBalance = signal<boolean>(false);
  readonly workingDaysInfo = signal<WorkingDaysCalculationResponse | null>(null);
  readonly calculatingDays = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly shortNoticeWarning = signal<boolean>(false);

  readonly minDate = new Date();

  form: FormGroup = this.fb.group({
    leaveType: ['VACATION' as LeaveType, Validators.required],
    startDate: [null as Date | null, Validators.required],
    endDate: [null as Date | null, Validators.required]
  });

  ngOnInit(): void {
    this.loadCurrentBalance();
    this.setupDateListeners();
  }

  loadCurrentBalance(): void {
    this.loadingBalance.set(true);
    this.leaveService.getMyLeaveBalance().subscribe({
      next: (b) => {
        this.balance.set(b);
        this.loadingBalance.set(false);
      },
      error: () => {
        this.loadingBalance.set(false);
      }
    });
  }

  private setupDateListeners(): void {
    this.form.valueChanges.pipe(
      debounceTime(250),
      distinctUntilChanged((prev, curr) =>
        prev.leaveType === curr.leaveType &&
        prev.startDate?.getTime() === curr.startDate?.getTime() &&
        prev.endDate?.getTime() === curr.endDate?.getTime()
      )
    ).subscribe((val) => {
      this.onValuesChanged(val);
    });
  }

  private onValuesChanged(val: { leaveType: LeaveType; startDate: Date | null; endDate: Date | null }): void {
    this.errorMessage.set(null);

    const { startDate, endDate } = val;
    if (!startDate || !endDate) {
      this.workingDaysInfo.set(null);
      this.shortNoticeWarning.set(false);
      return;
    }

    if (endDate < startDate) {
      this.workingDaysInfo.set(null);
      this.errorMessage.set('La fecha de fin no puede ser anterior a la de inicio.');
      return;
    }

    // Comprobar aviso de antelación (< 7 días desde hoy)
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const diffTime = startDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    this.shortNoticeWarning.set(diffDays < 7);

    // Invocar endpoint de cálculo interactivo en servidor (Art. 14)
    const startStr = this.formatDate(startDate);
    const endStr = this.formatDate(endDate);

    this.calculatingDays.set(true);
    this.leaveService.calculateWorkingDays(startStr, endStr).subscribe({
      next: (info) => {
        this.workingDaysInfo.set(info);
        this.calculatingDays.set(false);
        this.validateBalanceSufficient(val.leaveType, info.workingDays);
      },
      error: (err) => {
        this.calculatingDays.set(false);
        this.workingDaysInfo.set(null);
        this.errorMessage.set(err.error?.detail || 'Error al calcular los días laborables en el servidor.');
      }
    });
  }

  private validateBalanceSufficient(leaveType: LeaveType, workingDays: number): void {
    if (workingDays === 0) {
      this.errorMessage.set('El rango seleccionado no contiene ningún día laborable.');
      return;
    }

    if (leaveType === 'VACATION') {
      const avail = this.balance()?.availableDays ?? 0;
      if (workingDays > avail) {
        this.errorMessage.set(
          `Saldo insuficiente: dispones de ${avail} día(s) de vacaciones y solicitas ${workingDays} día(s) laborables.`
        );
      }
    }
  }

  onSubmit(): void {
    if (this.form.invalid || this.isSubmitDisabled()) {
      return;
    }

    const { leaveType, startDate, endDate } = this.form.value;
    const payload: CreateLeaveRequest = {
      leaveType,
      startDate: this.formatDate(startDate),
      endDate: this.formatDate(endDate)
    };

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.leaveService.createLeaveRequest(payload).subscribe({
      next: (res) => {
        this.submitting.set(false);
        this.successMessage.set(
          `¡Solicitud registrada correctamente en estado Pendiente! Días computados: ${res.calculatedDays}.`
        );
        this.form.patchValue({ startDate: null, endDate: null });
        this.workingDaysInfo.set(null);
        this.shortNoticeWarning.set(false);
        this.loadCurrentBalance();
      },
      error: (err) => {
        this.submitting.set(false);
        this.errorMessage.set(
          err.error?.detail || err.error?.title || 'No se pudo registrar la solicitud. Comprueba que no haya solapamientos ni saldo insuficiente.'
        );
      }
    });
  }

  isSubmitDisabled(): boolean {
    if (this.submitting() || this.calculatingDays() || this.form.invalid) {
      return true;
    }
    const info = this.workingDaysInfo();
    if (!info || info.workingDays <= 0) {
      return true;
    }
    if (this.form.value.leaveType === 'VACATION') {
      const avail = this.balance()?.availableDays ?? 0;
      if (info.workingDays > avail) {
        return true;
      }
    }
    return false;
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
