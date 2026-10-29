package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveSummaryItemResponse(
    UUID id,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    int calculatedDays,
    LeaveStatus status,
    String rejectionReason
) {}

