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

