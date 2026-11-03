import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import { TeamPendingRequestsComponent } from './team-pending-requests.component';
import { TeamLeaveService } from '../../services/team-leave.service';
import { TeamLeaveRequest } from '../../models/team-leave.models';
import { PageResponse } from '../../../leave/models/leave.models';
import { ResolveRequestDialogComponent } from '../resolve-request-dialog/resolve-request-dialog.component';

describe('TeamPendingRequestsComponent', () => {
  let mockService: Partial<TeamLeaveService>;
  let mockDialog: Partial<MatDialog>;
  let mockSnackBar: Partial<MatSnackBar>;

  const pendingRequest: TeamLeaveRequest = {
    id: 'req-001',
    employeeId: 'emp-001',
    employeeName: 'Laura Martínez Gómez',
    leaveType: 'VACATION',
    startDate: '2026-12-01',
    endDate: '2026-12-05',
    calculatedDays: 5,
    createdAt: '2026-11-01T10:00:00Z'
  };

  const page = (content: TeamLeaveRequest[]): PageResponse<TeamLeaveRequest> => ({
    content,
    page: 0,
    size: 10,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    first: true,
    last: true
  });

  const dialogReturning = (result: unknown) =>
    vi.fn().mockReturnValue({ afterClosed: () => of(result) });

  beforeEach(async () => {
    mockService = {
      getTeamPendingRequests: vi.fn().mockReturnValue(of(page([pendingRequest]))),
      approve: vi.fn().mockReturnValue(of({})),
      reject: vi.fn().mockReturnValue(of({}))
    };
    mockDialog = { open: dialogReturning({ text: null }) };
    mockSnackBar = { open: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [TeamPendingRequestsComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TeamLeaveService, useValue: mockService },
        { provide: MatDialog, useValue: mockDialog },
        { provide: MatSnackBar, useValue: mockSnackBar }
      ]
    }).compileComponents();
  });

  it('should load and render the pending requests of the team (HU-05.CA-1)', () => {
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    expect(mockService.getTeamPendingRequests).toHaveBeenCalledWith(0, 10);
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.table-row');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Laura Martínez Gómez');
  });

  it('should show an explicit empty state when there are no pending requests (HU-05.CA-1)', () => {
    mockService.getTeamPendingRequests = vi.fn().mockReturnValue(of(page([])));
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    const empty = (fixture.nativeElement as HTMLElement).querySelector('.empty-state');
    expect(empty?.textContent).toContain('No hay solicitudes pendientes en tu equipo');
  });

  it('should approve with optional comment after confirming the dialog (HU-05.CA-2)', () => {
    mockDialog.open = dialogReturning({ text: 'Buen descanso' });
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    fixture.componentInstance.openResolveDialog(pendingRequest, 'approve');

    expect(mockDialog.open).toHaveBeenCalledWith(
      ResolveRequestDialogComponent,
      expect.objectContaining({ data: { mode: 'approve', request: pendingRequest } })
    );
    expect(mockService.approve).toHaveBeenCalledWith('req-001', 'Buen descanso');
    expect(mockSnackBar.open).toHaveBeenCalledWith(expect.stringContaining('aprobada'), 'Cerrar', expect.any(Object));
    expect(mockService.getTeamPendingRequests).toHaveBeenCalledTimes(2);
  });

  it('should reject with the reason entered in the dialog (HU-05.CA-2)', () => {
    mockDialog.open = dialogReturning({ text: 'Coincide con la entrega' });
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    fixture.componentInstance.openResolveDialog(pendingRequest, 'reject');

    expect(mockService.reject).toHaveBeenCalledWith('req-001', 'Coincide con la entrega');
  });

  it('should not resolve when the dialog is dismissed', () => {
    mockDialog.open = dialogReturning(undefined);
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    fixture.componentInstance.openResolveDialog(pendingRequest, 'approve');

    expect(mockService.approve).not.toHaveBeenCalled();
  });

  it('should show the server error and reload when the request was already resolved (HU-05.CA-3)', () => {
    mockService.approve = vi.fn().mockReturnValue(
      throwError(() => ({ error: { detail: 'La solicitud ya ha sido resuelta y no puede volver a resolverse' } }))
    );
    const fixture = TestBed.createComponent(TeamPendingRequestsComponent);
    fixture.detectChanges();

    fixture.componentInstance.openResolveDialog(pendingRequest, 'approve');

    expect(mockSnackBar.open).toHaveBeenCalledWith(
      'La solicitud ya ha sido resuelta y no puede volver a resolverse',
      'Cerrar',
      expect.any(Object)
    );
    expect(mockService.getTeamPendingRequests).toHaveBeenCalledTimes(2);
  });
});
