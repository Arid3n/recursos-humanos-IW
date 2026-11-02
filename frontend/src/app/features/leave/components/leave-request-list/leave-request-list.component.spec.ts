import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { LeaveRequestListComponent } from './leave-request-list.component';
import { LeaveService } from '../../services/leave.service';
import { LeaveRequestSummary, PageResponse } from '../../models/leave.models';
import { RejectionReasonDialogComponent } from '../rejection-reason-dialog/rejection-reason-dialog.component';
import { CancelConfirmDialogComponent } from './cancel-confirm-dialog.component';

describe('LeaveRequestListComponent', () => {
  let mockLeaveService: Partial<LeaveService>;
  let mockDialog: Partial<MatDialog>;
  let mockSnackBar: Partial<MatSnackBar>;

  const mockPendingRequest: LeaveRequestSummary = {
    id: 'req-001',
    leaveType: 'VACATION',
    startDate: '2026-07-06',
    endDate: '2026-07-10',
    calculatedDays: 5,
    status: 'PENDING',
    rejectionReason: null,
    createdAt: '2026-06-01T10:00:00Z',
    canCancel: true
  };

  const mockApprovedRequest: LeaveRequestSummary = {
    id: 'req-002',
    leaveType: 'PERMIT',
    startDate: '2026-08-10',
    endDate: '2026-08-11',
    calculatedDays: 2,
    status: 'APPROVED',
    rejectionReason: null,
    createdAt: '2026-06-15T09:30:00Z',
    canCancel: false
  };

  const mockRejectedRequest: LeaveRequestSummary = {
    id: 'req-003',
    leaveType: 'VACATION',
    startDate: '2026-09-01',
    endDate: '2026-09-05',
    calculatedDays: 5,
    status: 'REJECTED',
    rejectionReason: 'Coincidencia de personal crítico en el departamento',
    createdAt: '2026-07-01T08:00:00Z',
    canCancel: false
  };

  const mockPageResponse: PageResponse<LeaveRequestSummary> = {
    content: [mockPendingRequest, mockApprovedRequest, mockRejectedRequest],
    page: 0,
    size: 10,
    totalElements: 3,
    totalPages: 1,
    first: true,
    last: true
  };

  beforeEach(async () => {
    mockLeaveService = {
      getMyRequests: vi.fn().mockReturnValue(of(mockPageResponse)),
      cancelRequest: vi.fn().mockReturnValue(of(undefined))
    };

    mockDialog = {
      open: vi.fn().mockReturnValue({
        afterClosed: () => of(true)
      })
    };

    mockSnackBar = {
      open: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LeaveRequestListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LeaveService, useValue: mockLeaveService },
        { provide: MatDialog, useValue: mockDialog },
        { provide: MatSnackBar, useValue: mockSnackBar }
      ]
    }).compileComponents();
  });

  it('should create the component and load requests on init (HU-04.CA-1)', () => {
    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(mockLeaveService.getMyRequests).toHaveBeenCalledWith(0, 10, undefined);
    expect(component.requests().length).toBe(3);
    expect(component.totalElements()).toBe(3);

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('.table-row');
    expect(rows.length).toBe(3);
  });

  it('should filter by status when filter is selected (HU-04.CA-4)', () => {
    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.setFilter('PENDING');

    expect(component.selectedStatus()).toBe('PENDING');
    expect(component.pageIndex).toBe(0);
    expect(mockLeaveService.getMyRequests).toHaveBeenCalledWith(0, 10, 'PENDING');
  });

  it('should handle pagination changes', () => {
    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.onPageChange({ pageIndex: 2, pageSize: 25, length: 100 });

    expect(component.pageIndex).toBe(2);
    expect(component.pageSize).toBe(25);
    expect(mockLeaveService.getMyRequests).toHaveBeenCalledWith(2, 25, undefined);
  });

  it('should open rejection reason dialog when clicking "Ver Motivo" (HU-04.CA-2)', () => {
    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.openRejectionReason(mockRejectedRequest);

    expect(mockDialog.open).toHaveBeenCalledWith(
      RejectionReasonDialogComponent,
      expect.objectContaining({
        data: expect.objectContaining({
          rejectionReason: 'Coincidencia de personal crítico en el departamento',
          leaveType: 'VACATION'
        })
      })
    );
  });

  it('should dispatch cancelRequest when confirmed in dialog (HU-04.CA-3)', () => {
    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.confirmCancel(mockPendingRequest);

    expect(mockDialog.open).toHaveBeenCalledWith(
      CancelConfirmDialogComponent,
      expect.objectContaining({
        data: { request: mockPendingRequest }
      })
    );
    expect(mockLeaveService.cancelRequest).toHaveBeenCalledWith('req-001');
    expect(mockSnackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('cancelada correctamente'),
      'Cerrar',
      expect.any(Object)
    );
  });

  it('should not dispatch cancelRequest when dialog is dismissed', () => {
    mockDialog.open = vi.fn().mockReturnValue({
      afterClosed: () => of(false)
    });

    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.confirmCancel(mockPendingRequest);

    expect(mockLeaveService.cancelRequest).not.toHaveBeenCalled();
  });

  it('should show error notification when cancel fails (e.g., 409 Conflict)', () => {
    mockLeaveService.cancelRequest = vi.fn().mockReturnValue(
      throwError(() => ({ error: { detail: 'No se puede cancelar una solicitud aprobada.' } }))
    );

    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.confirmCancel(mockPendingRequest);

    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'No se puede cancelar una solicitud aprobada.',
      'Cerrar',
      expect.any(Object)
    );
  });

  it('should show empty state when there are no requests', () => {
    mockLeaveService.getMyRequests = vi.fn().mockReturnValue(of({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true
    }));

    const fixture = TestBed.createComponent(LeaveRequestListComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.requests().length).toBe(0);

    const compiled = fixture.nativeElement as HTMLElement;
    const emptyState = compiled.querySelector('.empty-state');
    expect(emptyState).toBeTruthy();
    expect(emptyState?.textContent).toContain('Aún no tienes solicitudes registradas');
  });
});
