import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog } from '@angular/material/dialog';

import { TeamCalendarService } from '../../services/team-calendar.service';
import { TeamCalendar, TeamCalendarDay, TeamCalendarEvent } from '../../models/team-calendar.models';
import { AbsenceDetailDialogComponent } from '../absence-detail-dialog/absence-detail-dialog.component';

/** Casilla de la cuadrícula: null para los huecos anteriores/posteriores al mes. */
export type CalendarCell = TeamCalendarDay | null;

/** Zona horaria de negocio (Art. 14.2): el mes inicial es el mes actual en Madrid, no en la zona del navegador. */
const BUSINESS_TIME_ZONE = 'Europe/Madrid';
const BUSINESS_YEAR_MONTH_FORMAT = new Intl.DateTimeFormat('en-CA', {
  timeZone: BUSINESS_TIME_ZONE,
  year: 'numeric',
  month: '2-digit'
});
const DAY_LABEL_FORMAT = new Intl.DateTimeFormat('es-ES', { weekday: 'long', day: 'numeric' });

const MONTH_TITLE_FORMAT = new Intl.DateTimeFormat('es-ES', { month: 'long', year: 'numeric' });

@Component({
  selector: 'app-team-calendar',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './team-calendar.component.html',
  styleUrls: ['./team-calendar.component.scss']
})
export class TeamCalendarComponent implements OnInit {
  private readonly teamCalendarService = inject(TeamCalendarService);
  private readonly dialog = inject(MatDialog);
  private readonly breakpointObserver = inject(BreakpointObserver);

  /** Pantalla estrecha (< 600 px): lista de días con ausencias en lugar de la cuadrícula (FR-003b). */
  readonly isNarrow = toSignal(
    this.breakpointObserver.observe(Breakpoints.XSmall).pipe(map(state => state.matches)),
    { initialValue: false }
  );

  readonly weekDays = [
    { short: 'Lun', full: 'Lunes' },
    { short: 'Mar', full: 'Martes' },
    { short: 'Mié', full: 'Miércoles' },
    { short: 'Jue', full: 'Jueves' },
    { short: 'Vie', full: 'Viernes' },
    { short: 'Sáb', full: 'Sábado' },
    { short: 'Dom', full: 'Domingo' }
  ];

  /** Primer día del mes visible (hora local). */
  readonly visibleMonth = signal<Date>(this.currentMonthInBusinessZone());
  readonly calendar = signal<TeamCalendar | null>(null);
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  /** Empleado seleccionado en el filtro; null = todo el equipo. Solo afecta a la vista. */
  readonly selectedEmployeeId = signal<string | null>(null);

  readonly monthTitle = computed(() => MONTH_TITLE_FORMAT.format(this.visibleMonth()));

  readonly monthKey = computed(() => {
    const date = this.visibleMonth();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  });

  /**
   * Días del mes con los eventos filtrados por empleado. El recuento y la coincidencia
   * se mantienen tal como los calcula el servidor para todo el equipo (Art. 16.3).
   */
  readonly visibleDays = computed<TeamCalendarDay[]>(() => {
    const days = this.calendar()?.days ?? [];
    const employeeId = this.selectedEmployeeId();
    if (!employeeId) {
      return days;
    }
    return days.map(day => ({ ...day, events: day.events.filter(event => event.employeeId === employeeId) }));
  });

  /** Semanas de lunes a domingo con los días del mes devueltos por el servidor. */
  readonly weeks = computed<CalendarCell[][]>(() => {
    const days = this.visibleDays();
    if (days.length === 0) {
      return [];
    }
    const leading = (this.parseDate(days[0].date).getDay() + 6) % 7;
    const cells: CalendarCell[] = [...Array(leading).fill(null), ...days];
    while (cells.length % 7 !== 0) {
      cells.push(null);
    }
    const weeks: CalendarCell[][] = [];
    for (let i = 0; i < cells.length; i += 7) {
      weeks.push(cells.slice(i, i + 7));
    }
    return weeks;
  });

  /** Días laborables del mes con ausencias visibles (vista de lista); fines de semana y festivos no aparecen. */
  readonly daysWithAbsences = computed<TeamCalendarDay[]>(() =>
    this.visibleDays().filter(day => day.dayType === 'WORKING' && day.events.length > 0)
  );

  readonly hasMembers = computed(() => (this.calendar()?.members.length ?? 0) > 0);

  readonly hasAbsences = computed(() => this.visibleDays().some(day => day.events.length > 0));

  ngOnInit(): void {
    this.loadCalendar();
  }

  loadCalendar(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.teamCalendarService.getTeamCalendar(this.monthKey()).subscribe({
      next: (calendar) => {
        this.calendar.set(calendar);
        this.loading.set(false);
      },
      error: (err) => {
        this.calendar.set(null);
        this.loading.set(false);
        this.errorMessage.set(err.error?.detail || 'No se pudo cargar el calendario del equipo. Por favor, reintenta más tarde.');
      }
    });
  }

  previousMonth(): void {
    this.changeMonth(-1);
  }

  nextMonth(): void {
    this.changeMonth(1);
  }

  openDetail(event: TeamCalendarEvent): void {
    this.dialog.open<AbsenceDetailDialogComponent, TeamCalendarEvent>(AbsenceDetailDialogComponent, {
      width: '480px',
      data: event
    });
  }

  dayLabel(day: TeamCalendarDay): string {
    return DAY_LABEL_FORMAT.format(this.parseDate(day.date));
  }

  dayNumber(day: TeamCalendarDay): number {
    return this.parseDate(day.date).getDate();
  }

  leaveTypeLabel(event: TeamCalendarEvent): string {
    return event.leaveType === 'VACATION' ? 'Vacaciones' : 'Permiso';
  }

  leaveTypeIcon(event: TeamCalendarEvent): string {
    return event.leaveType === 'VACATION' ? 'beach_access' : 'event_note';
  }

  private changeMonth(offset: number): void {
    const current = this.visibleMonth();
    this.visibleMonth.set(new Date(current.getFullYear(), current.getMonth() + offset, 1));
    this.loadCalendar();
  }

  /** Primer día del mes actual según el calendario de Europe/Madrid. */
  private currentMonthInBusinessZone(): Date {
    const [year, month] = BUSINESS_YEAR_MONTH_FORMAT.format(new Date()).split('-').map(Number);
    return new Date(year, month - 1, 1);
  }

  /** Interpreta "YYYY-MM-DD" como fecha local, sin desplazamiento de zona horaria. */
  private parseDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
  }
}
