import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatDialog } from '@angular/material/dialog';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { BehaviorSubject, of } from 'rxjs';
import { BreakpointObserver, BreakpointState } from '@angular/cdk/layout';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

import { TeamCalendarComponent } from './team-calendar.component';
import { TeamCalendarService } from '../../services/team-calendar.service';
import { TeamCalendar, TeamCalendarDay, TeamCalendarEvent, TeamMember } from '../../models/team-calendar.models';
import { AbsenceDetailDialogComponent } from '../absence-detail-dialog/absence-detail-dialog.component';

describe('TeamCalendarComponent', () => {
  let mockService: Partial<TeamCalendarService>;
  let mockDialog: Partial<MatDialog>;
  let viewport$: BehaviorSubject<BreakpointState>;

  const members: TeamMember[] = [
    { id: 'emp-1', name: 'Laura Martínez' },
    { id: 'emp-2', name: 'Pedro Gómez' }
  ];

  const approved: TeamCalendarEvent = {
    requestId: 'req-1',
    employeeId: 'emp-1',
    employeeName: 'Laura Martínez',
    leaveType: 'VACATION',
    status: 'APPROVED',
    startDate: '2026-12-09',
    endDate: '2026-12-10'
  };

  const pending: TeamCalendarEvent = {
    requestId: 'req-2',
    employeeId: 'emp-2',
    employeeName: 'Pedro Gómez',
    leaveType: 'PERMIT',
    status: 'PENDING',
    startDate: '2026-12-10',
    endDate: '2026-12-10'
  };

  /** Diciembre de 2026: el 9 solo Laura, el 10 Laura y Pedro (coincidencia), el 8 festivo. */
  const december = (withEvents = true, teamMembers = members): TeamCalendar => {
    const days: TeamCalendarDay[] = [];
    for (let d = 1; d <= 31; d++) {
      const date = `2026-12-${String(d).padStart(2, '0')}`;
      const weekday = new Date(2026, 11, d).getDay();
      const weekend = weekday === 0 || weekday === 6;
      const holiday = d === 8;
      let events: TeamCalendarEvent[] = [];
      if (withEvents && d === 9) events = [approved];
      if (withEvents && d === 10) events = [approved, pending];
      const absentCount = new Set(events.map(e => e.employeeId)).size;
      days.push({
        date,
        dayType: weekend ? 'WEEKEND' : holiday ? 'HOLIDAY' : 'WORKING',
        holidayName: holiday ? 'Inmaculada Concepción' : null,
        absentCount,
        hasConflict: absentCount >= 2,
        events
      });
    }
    return { month: '2026-12', members: teamMembers, days };
  };

  const create = () => {
    const fixture = TestBed.createComponent(TeamCalendarComponent);
    fixture.detectChanges();
    return { fixture, el: fixture.nativeElement as HTMLElement };
  };

  const button = (el: HTMLElement, label: string) =>
    el.querySelector(`button[aria-label="${label}"]`) as HTMLButtonElement;

  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 11, 15));

    mockService = { getTeamCalendar: vi.fn().mockReturnValue(of(december())) };
    mockDialog = { open: vi.fn() };
    viewport$ = new BehaviorSubject<BreakpointState>({ matches: false, breakpoints: {} });

    await TestBed.configureTestingModule({
      imports: [TeamCalendarComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TeamCalendarService, useValue: mockService },
        { provide: MatDialog, useValue: mockDialog },
        { provide: BreakpointObserver, useValue: { observe: () => viewport$.asObservable() } }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('should load the current month on init with a Spanish title (HU-06.CA-1)', () => {
    const { el } = create();

    expect(mockService.getTeamCalendar).toHaveBeenCalledWith('2026-12');
    expect(el.querySelector('.month-title')?.textContent).toContain('diciembre de 2026');
    expect(el.querySelectorAll('th[scope="col"]').length).toBe(7);
  });

  it('should request the previous and next month, including a year change (HU-06.CA-1)', () => {
    const { fixture, el } = create();

    button(el, 'Mes siguiente').click();
    fixture.detectChanges();
    expect(mockService.getTeamCalendar).toHaveBeenLastCalledWith('2027-01');

    button(el, 'Mes anterior').click();
    button(el, 'Mes anterior').click();
    fixture.detectChanges();
    expect(mockService.getTeamCalendar).toHaveBeenLastCalledWith('2026-11');
  });

  it('should render events with name and status text and show a legend (HU-06.CA-2)', () => {
    const { el } = create();

    const events = el.querySelectorAll('.event');
    expect(events.length).toBe(3);
    expect(events[0].textContent).toContain('Laura Martínez');
    expect(events[0].textContent).toContain('Vacaciones');
    expect(events[0].textContent).toContain('Aprobada');
    expect(events[2].textContent).toContain('Pedro Gómez');
    expect(events[2].textContent).toContain('Pendiente');

    const legend = el.querySelector('.legend')?.textContent ?? '';
    expect(legend).toContain('Aprobada');
    expect(legend).toContain('Pendiente');
    expect(legend).toContain('Coincidencia');
  });

  it('should highlight the conflict day with the number of absences (HU-06.CA-3)', () => {
    const { el } = create();

    const conflicts = el.querySelectorAll('.conflict-indicator');
    expect(conflicts.length).toBe(1);
    expect(conflicts[0].textContent).toContain('2 ausencias coincidentes');
    expect(el.querySelectorAll('td.conflict').length).toBe(1);
  });

  it('should show the holiday name on holidays', () => {
    const { el } = create();

    expect(el.querySelector('td.holiday .holiday-name')?.textContent).toContain('Inmaculada Concepción');
  });

  it('should show an empty-month message when there are no absences', () => {
    mockService.getTeamCalendar = vi.fn().mockReturnValue(of(december(false)));
    const { el } = create();

    expect(el.querySelector('.empty-state')?.textContent).toContain('No hay ausencias en este mes');
    expect(el.querySelectorAll('tbody td.day-cell').length).toBe(31);
  });

  it('should show an empty-team message when the manager has no team members', () => {
    mockService.getTeamCalendar = vi.fn().mockReturnValue(of(december(false, [])));
    const { el } = create();

    expect(el.querySelector('.empty-state')?.textContent).toContain('No tienes personas a tu cargo');
  });

  it('should open the absence detail dialog with the clicked event (HU-06.CA-1)', () => {
    const { el } = create();

    (el.querySelector('.event') as HTMLButtonElement).click();

    expect(mockDialog.open).toHaveBeenCalledWith(
      AbsenceDetailDialogComponent,
      expect.objectContaining({ data: approved })
    );
  });
  describe('employee filter (US2)', () => {
    const filter = async () => {
      const { fixture, el } = create();
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const select = await loader.getHarness(MatSelectHarness.with({ selector: '.employee-filter mat-select' }));
      return { fixture, el, select };
    };

    it('should offer "Todo el equipo" plus the team members', async () => {
      const { select } = await filter();

      await select.open();
      const options = await select.getOptions();
      const texts = await Promise.all(options.map(o => o.getText()));

      expect(texts).toEqual(['Todo el equipo', 'Laura Martínez', 'Pedro Gómez']);
    });

    it('should show only the selected employee while keeping the team conflict (HU-06.CA-3)', async () => {
      const { fixture, el, select } = await filter();

      await select.open();
      await select.clickOptions({ text: 'Pedro Gómez' });
      fixture.detectChanges();

      const events = el.querySelectorAll('.event');
      expect(events.length).toBe(1);
      expect(events[0].textContent).toContain('Pedro Gómez');
      expect(el.querySelector('.conflict-indicator')?.textContent).toContain('2 ausencias coincidentes');
    });

    it('should show every event again when "Todo el equipo" is selected', async () => {
      const { fixture, el, select } = await filter();

      await select.open();
      await select.clickOptions({ text: 'Laura Martínez' });
      fixture.detectChanges();
      expect(el.querySelectorAll('.event').length).toBe(2);

      await select.open();
      await select.clickOptions({ text: 'Todo el equipo' });
      fixture.detectChanges();
      expect(el.querySelectorAll('.event').length).toBe(3);
    });

    it('should keep the selection when changing month and never send an employee id to the API', async () => {
      const { fixture, el, select } = await filter();

      await select.open();
      await select.clickOptions({ text: 'Laura Martínez' });
      button(el, 'Mes siguiente').click();
      fixture.detectChanges();

      expect(mockService.getTeamCalendar).toHaveBeenLastCalledWith('2027-01');
      expect(vi.mocked(mockService.getTeamCalendar!).mock.calls.every(call => call.length === 1)).toBe(true);
      expect(await select.getValueText()).toBe('Laura Martínez');
    });
  });

  describe('initial month in Europe/Madrid (FR-003, Art. 14.2)', () => {
    it('should start in January 2027 at 23:30 UTC on 31 December 2026', () => {
      vi.setSystemTime(new Date('2026-12-31T23:30:00Z'));

      create();

      expect(mockService.getTeamCalendar).toHaveBeenCalledWith('2027-01');
    });

    it('should stay in December at 22:30 UTC on 31 December 2026', () => {
      vi.setSystemTime(new Date('2026-12-31T22:30:00Z'));

      create();

      expect(mockService.getTeamCalendar).toHaveBeenCalledWith('2026-12');
    });
  });

  describe('narrow viewport list view (FR-003b)', () => {
    const narrow = () => {
      viewport$.next({ matches: true, breakpoints: {} });
      const created = create();
      created.fixture.detectChanges();
      return created;
    };

    it('should render a list of working days with absences instead of the table (HU-06.CA-1)', () => {
      const { el } = narrow();

      expect(el.querySelector('table')).toBeNull();
      const items = el.querySelectorAll('.day-list > li');
      expect(items.length).toBe(2);
      expect(el.textContent).not.toContain('Inmaculada Concepción');
      expect(el.querySelector('.month-title')?.textContent).toContain('diciembre de 2026');
      expect(el.querySelector('.legend')).not.toBeNull();
    });

    it('should show name, type and status text and the conflict marker with icon and text (HU-06.CA-2, HU-06.CA-3)', () => {
      const { el } = narrow();

      const items = el.querySelectorAll('.day-list > li');
      expect(items[0].querySelector('.conflict-indicator')).toBeNull();
      expect(items[1].querySelector('.conflict-indicator mat-icon')).not.toBeNull();
      expect(items[1].querySelector('.conflict-indicator')?.textContent).toContain('2 ausencias coincidentes');
      expect(items[1].textContent).toContain('Pedro Gómez');
      expect(items[1].textContent).toContain('Pendiente');
      expect(items[0].textContent).toContain('Aprobada');
    });

    it('should open the same detail dialog from the list (HU-06.CA-1)', () => {
      const { el } = narrow();

      (el.querySelector('.day-list .event') as HTMLButtonElement).click();

      expect(mockDialog.open).toHaveBeenCalledWith(
        AbsenceDetailDialogComponent,
        expect.objectContaining({ data: approved })
      );
    });

    it('should keep month navigation and the employee filter working with the conflict text', async () => {
      const { fixture, el } = narrow();
      const loader = TestbedHarnessEnvironment.loader(fixture);
      const select = await loader.getHarness(MatSelectHarness.with({ selector: '.employee-filter mat-select' }));

      await select.open();
      await select.clickOptions({ text: 'Pedro Gómez' });
      fixture.detectChanges();

      const items = el.querySelectorAll('.day-list > li');
      expect(items.length).toBe(1);
      expect(items[0].querySelector('.conflict-indicator')?.textContent).toContain('2 ausencias coincidentes');

      button(el, 'Mes siguiente').click();
      fixture.detectChanges();
      expect(mockService.getTeamCalendar).toHaveBeenLastCalledWith('2027-01');
    });

    it('should show the empty-month message and no list items when there are no absences', () => {
      mockService.getTeamCalendar = vi.fn().mockReturnValue(of(december(false)));
      const { el } = narrow();

      expect(el.querySelector('.empty-state')?.textContent).toContain('No hay ausencias en este mes');
      expect(el.querySelectorAll('.day-list > li').length).toBe(0);
    });

    it('should render the table and no list in a wide viewport', () => {
      const { el } = create();

      expect(el.querySelector('table')).not.toBeNull();
      expect(el.querySelector('.day-list')).toBeNull();
    });
  });

  it('should never disable the month buttons after navigating several years (FR-003)', () => {
    const { fixture, el } = create();

    for (let i = 0; i < 40; i++) {
      button(el, 'Mes siguiente').click();
      fixture.detectChanges();
    }

    expect(button(el, 'Mes siguiente').disabled).toBe(false);
    expect(button(el, 'Mes anterior').disabled).toBe(false);
    expect(mockService.getTeamCalendar).toHaveBeenLastCalledWith('2030-04');
  });
});
