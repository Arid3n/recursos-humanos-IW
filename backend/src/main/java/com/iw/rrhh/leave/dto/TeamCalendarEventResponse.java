package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;

import java.time.LocalDate;
import java.util.UUID;

public record TeamCalendarEventResponse(
    UUID requestId,
    UUID employeeId,
    String employeeName,
    LeaveType leaveType,
    LeaveStatus status,
    LocalDate startDate,
    LocalDate endDate
) {}
