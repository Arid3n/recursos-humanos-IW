export type LeaveType = 'VACATION' | 'PERMIT';

export type LeaveStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface LeaveBalanceResponse {
  employeeId: string;
  year: number;
  entitledDays: number;
  spentDays: number;
  pendingDays: number;
  availableDays: number;
  isClosedYear: boolean;
  agreementName: string | null;
}

export interface LeaveSummaryItem {
  id: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  calculatedDays: number;
  status: LeaveStatus;
  rejectionReason: string | null;
}

export interface EmployeeProfile {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  nationalId: string;
  hireDate: string;
  currentPosition: string;
  currentDepartment: string;
  currentAgreementName: string;
  managerFullName: string | null;
}

export interface CreateLeaveRequest {
  leaveType: LeaveType;
  startDate: string; // ISO YYYY-MM-DD
  endDate: string;   // ISO YYYY-MM-DD
}

export interface LeaveRequestResponse {
  id: string;
  employeeId: string;
  employeeFullName: string;
  managerId: string | null;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  calculatedDays: number;
  status: LeaveStatus;
  createdAt: string;
}

export interface WorkingDaysCalculationResponse {
  startDate: string;
  endDate: string;
  workingDays: number;
  calendarDays: number;
  weekendDays: number;
  holidayDays: number;
}

export interface LeaveRequestSummary {
  id: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  calculatedDays: number;
  status: LeaveStatus;
  rejectionReason: string | null;
  createdAt: string;
  canCancel: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}


