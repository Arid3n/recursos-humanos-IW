export type ReportType = 'ABSENTEEISM' | 'VACATION_USAGE';
export type ReportPeriodPreset = 'CURRENT_YEAR' | 'PREVIOUS_YEAR' | 'CURRENT_QUARTER' | 'CUSTOM';

export interface EmployeeReportRow {
  employeeId: string;
  employeeName: string;
  totalEntitledDays: number;
  totalTakenDays: number;
  totalRemainingDays: number;
  metricPercentage: number;
}

export interface DepartmentReportRow {
  department: string;
  headcount: number;
  totalEntitledDays: number;
  totalTakenDays: number;
  totalRemainingDays: number;
  metricPercentage: number;
  employees: EmployeeReportRow[];
}

export interface DepartmentReportResponse {
  type: ReportType;
  startDate: string;
  endDate: string;
  periodLabel: string;
  hasData: boolean;
  departments: DepartmentReportRow[];
}
