package com.iw.rrhh.leave.service;

import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.dto.WorkingDaysCalculationResponse;

import java.time.LocalDate;

public interface LeaveRequestService {

    WorkingDaysCalculationResponse calculateWorkingDays(LocalDate startDate, LocalDate endDate);

    LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request);
}
