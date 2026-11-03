package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TeamLeaveRequestResponse(
    UUID id,
    UUID employeeId,
    String employeeName,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    Integer calculatedDays,
    Instant createdAt
) {}
