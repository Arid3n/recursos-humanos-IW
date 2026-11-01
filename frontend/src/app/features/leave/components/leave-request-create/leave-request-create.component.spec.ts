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
    status: 'PENDING',
    createdAt: '2026-11-01T12:00:00Z'
  };

  beforeEach(async () => {
    mockLeaveService = {
      getMyLeaveBalance: vi.fn().mockReturnValue(of(mockBalance)),
      calculateWorkingDays: vi.fn().mockReturnValue(of(mockCalculation)),
      createLeaveRequest: vi.fn().mockReturnValue(of(mockCreatedResponse))
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
});
