package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LeaveRequestSummaryResponse(
        UUID id,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        Integer calculatedDays,
        LeaveStatus status,
        String rejectionReason,
        Instant createdAt,
        boolean canCancel
) {
}
