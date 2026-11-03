import { LeaveStatus, LeaveType } from '../../leave/models/leave.models';

export interface TeamLeaveRequest {
  id: string;
  employeeId: string;
  employeeName: string;
  leaveType: LeaveType;
  startDate: string;
  endDate: string;
  calculatedDays: number;
  createdAt: string;
}

export interface LeaveResolution {
  id: string;
  status: LeaveStatus;
  reason: string | null;
  resolvedById: string;
  resolvedAt: string;
}

export interface ApproveLeaveRequest {
  comment: string | null;
}

export interface RejectLeaveRequest {
  reason: string;
}
