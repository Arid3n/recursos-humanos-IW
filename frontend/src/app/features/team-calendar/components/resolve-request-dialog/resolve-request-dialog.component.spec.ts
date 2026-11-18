import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { describe, it, expect, beforeEach, vi } from 'vitest';

import {
  ResolveMode,
  ResolveRequestDialogComponent,
  REASON_MAX_LENGTH
} from './resolve-request-dialog.component';
import { TeamLeaveRequest } from '../../models/team-leave.models';

describe('ResolveRequestDialogComponent', () => {
  const request: TeamLeaveRequest = {
    id: 'req-001',
    employeeId: 'emp-001',
    employeeName: 'Laura Martínez Gómez',
    leaveType: 'VACATION',
    startDate: '2026-12-01',
    endDate: '2026-12-05',
    calculatedDays: 5,
    createdAt: '2026-11-01T10:00:00Z'
  };

  let dialogRef: { close: ReturnType<typeof vi.fn> };

  const create = async (mode: ResolveMode) => {
    dialogRef = { close: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ResolveRequestDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: { mode, request } },
        { provide: MatDialogRef, useValue: dialogRef }
      ]
    }).compileComponents();
    const fixture = TestBed.createComponent(ResolveRequestDialogComponent);
    fixture.detectChanges();
    return fixture;
  };

  const type = (fixture: Awaited<ReturnType<typeof create>>, text: string) => {
    fixture.componentInstance.textControl.setValue(text);
    fixture.componentInstance.textControl.markAsTouched();
    fixture.detectChanges();
  };

  beforeEach(() => TestBed.resetTestingModule());

  it('should show the mat-error next to the field when rejecting with an empty reason (HU-05.CA-2)', async () => {
    const fixture = await create('reject');
    type(fixture, '   ');

    const error = (fixture.nativeElement as HTMLElement).querySelector('mat-error');
    expect(error?.textContent).toContain('obligatorio');
    fixture.componentInstance.confirm();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('should require at least 5 characters after trimming when rejecting (HU-05.CA-2)', async () => {
    const fixture = await create('reject');
    type(fixture, '  abc  ');

    expect((fixture.nativeElement as HTMLElement).querySelector('mat-error')?.textContent).toContain('al menos 5');
    fixture.componentInstance.confirm();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it.each<ResolveMode>(['approve', 'reject'])(
    'should show the mat-error and block confirm when the text exceeds 500 characters in %s mode (HU-05.CA-2)',
    async (mode) => {
      const fixture = await create(mode);
      type(fixture, 'a'.repeat(REASON_MAX_LENGTH + 1));

      expect((fixture.nativeElement as HTMLElement).querySelector('mat-error')?.textContent).toContain('500');
      fixture.componentInstance.confirm();
      expect(dialogRef.close).not.toHaveBeenCalled();
    }
  );

  it('should show the character counter in both modes (HU-05.CA-2)', async () => {
    for (const mode of ['approve', 'reject'] as ResolveMode[]) {
      TestBed.resetTestingModule();
      const fixture = await create(mode);
      type(fixture, 'Buen descanso');

      expect((fixture.nativeElement as HTMLElement).querySelector('mat-hint')?.textContent).toContain('13 / 500');
    }
  });

  it('should count the limit over the trimmed text, so surrounding spaces do not block a valid reason (FR-003)', async () => {
    const fixture = await create('reject');
    type(fixture, `  ${'a'.repeat(REASON_MAX_LENGTH)}  `);

    expect(fixture.componentInstance.textControl.valid).toBe(true);
    fixture.componentInstance.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith({ text: 'a'.repeat(REASON_MAX_LENGTH) });
  });

  it('should approve without a comment returning a null text (HU-05.CA-2)', async () => {
    const fixture = await create('approve');
    type(fixture, '   ');

    fixture.componentInstance.confirm();
    expect(dialogRef.close).toHaveBeenCalledWith({ text: null });
  });
});
