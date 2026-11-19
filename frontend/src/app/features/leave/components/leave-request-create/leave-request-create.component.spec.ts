import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { LeaveRequestCreateComponent } from './leave-request-create.component';
import { LeaveService } from '../../services/leave.service';
import { LeaveBalanceResponse, LeaveRequestResponse, WorkingDaysCalculationResponse } from '../../models/leave.models';

describe('LeaveRequestCreateComponent', () => {
  let mockLeaveService: Partial<LeaveService>;

  const mockBalance: LeaveBalanceResponse = {
    employeeId: '123e4567-e89b-12d3-a456-426614174000',
    year: 2026,
    entitledDays: 22,
    spentDays: 5,
    pendingDays: 2,
    availableDays: 17,
    isClosedYear: false,
    agreementName: 'Convenio TIC'
  };

  const mockCalculation: WorkingDaysCalculationResponse = {
    startDate: '2026-07-06',
    endDate: '2026-07-10',
    workingDays: 5,
    calendarDays: 5,
    weekendDays: 0,
    holidayDays: 0
  };

  const mockCreatedResponse: LeaveRequestResponse = {
    id: 'req-001-uuid',
    employeeId: '123e4567-e89b-12d3-a456-426614174000',
    employeeFullName: 'Juan Pérez',
    managerId: 'man-001-uuid',
    leaveType: 'VACATION',
    startDate: '2026-07-06',
    endDate: '2026-07-10',
    calculatedDays: 5,
    justification: null,
    status: 'PENDING',
    createdAt: '2026-11-01T12:00:00Z'
  };

  beforeEach(async () => {
    mockLeaveService = {
      getMyLeaveBalance: vi.fn().mockReturnValue(of(mockBalance)),
      calculateWorkingDays: vi.fn().mockReturnValue(of(mockCalculation)),
      createLeaveRequest: vi.fn().mockReturnValue(of(mockCreatedResponse)),
      getEligibility: vi.fn().mockReturnValue(of({ canRequest: true, reason: null }))
    };

    await TestBed.configureTestingModule({
      imports: [LeaveRequestCreateComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        { provide: LeaveService, useValue: mockLeaveService }
      ]
    }).compileComponents();
  });

  it('should create the component and load initial balance', () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(mockLeaveService.getMyLeaveBalance).toHaveBeenCalled();
    expect(component.balance()?.availableDays).toBe(17);
  });

  it('should initialize form with VACATION by default', () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.form.get('leaveType')?.value).toBe('VACATION');
    expect(component.form.get('startDate')?.value).toBeNull();
    expect(component.form.get('endDate')?.value).toBeNull();
  });

  it('should disable submit button when form is untouched or incomplete', () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isSubmitDisabled()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    const submitBtn = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(submitBtn?.disabled).toBe(true);
  });

  it('should successfully submit valid leave request', () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.form.patchValue({
      leaveType: 'VACATION',
      startDate: new Date(2026, 11, 7),
      endDate: new Date(2026, 11, 11)
    });
    component.workingDaysInfo.set(mockCalculation);

    expect(component.isSubmitDisabled()).toBe(false);

    component.onSubmit();

    expect(mockLeaveService.createLeaveRequest).toHaveBeenCalled();
    expect(component.successMessage()).toContain('¡Solicitud registrada correctamente en estado Pendiente!');
  });

  it('should require justification when leaveType is PERMIT and allow optional for VACATION', () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    // Default VACATION: justification optional
    expect(component.form.get('justification')?.valid).toBe(true);

    // Switch to PERMIT: justification required
    component.form.get('leaveType')?.setValue('PERMIT');
    expect(component.form.get('justification')?.valid).toBe(false);

    component.form.get('justification')?.setValue('Asunto médico justificado');
    expect(component.form.get('justification')?.valid).toBe(true);
  });

  // ---- HU-05 fase 9 (FR-015, FR-017): elegibilidad ----

  it('should show the reason instead of the form when the user cannot request (no responsible)', async () => {
    (mockLeaveService.getEligibility as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ canRequest: false, reason: 'No tienes un responsable asignado' })
    );
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No tienes un responsable asignado');
    expect(compiled.querySelector('form')).toBeNull();
    expect(compiled.querySelector('button[type="submit"]')).toBeNull();
  });

  it('should show the Dirección General reason instead of the form', async () => {
    (mockLeaveService.getEligibility as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ canRequest: false, reason: 'Dirección General no solicita ausencias' })
    );
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Dirección General no solicita ausencias');
    expect(compiled.querySelector('form')).toBeNull();
  });

  it('should show the form when the user can request', async () => {
    const fixture = TestBed.createComponent(LeaveRequestCreateComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(mockLeaveService.getEligibility).toHaveBeenCalled();
    expect(compiled.querySelector('form')).not.toBeNull();
  });
});
