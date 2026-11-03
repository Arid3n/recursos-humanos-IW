import { LeaveType } from '../../leave/models/leave.models';

export type CalendarDayType = 'WORKING' | 'WEEKEND' | 'HOLIDAY';

export type TeamCalendarStatus = 'PENDING' | 'APPROVED';

export interface TeamMember {
  id: string;
  name: string;
}

export interface TeamCalendarEvent {
  requestId: string;
  employeeId: string;
  employeeName: string;
  leaveType: LeaveType;
  status: TeamCalendarStatus;
  startDate: string;
  endDate: string;
}

export interface TeamCalendarDay {
  date: string;
  dayType: CalendarDayType;
  holidayName: string | null;
  absentCount: number;
  hasConflict: boolean;
  events: TeamCalendarEvent[];
}

export interface TeamCalendar {
  month: string;
  members: TeamMember[];
  days: TeamCalendarDay[];
}
