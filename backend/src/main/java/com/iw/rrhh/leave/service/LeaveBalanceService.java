package com.iw.rrhh.leave.service;

import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveSummaryItemResponse;

import java.util.List;
import java.util.UUID;

public interface LeaveBalanceService {

    LeaveBalanceResponse getMyLeaveBalance(Integer year);

    LeaveBalanceResponse getEmployeeLeaveBalance(UUID employeeId, Integer year);

    List<LeaveSummaryItemResponse> getMyLeaveRequests(Integer year);
}

