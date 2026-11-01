package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLeaveRequest(
        @NotNull(message = "El tipo de ausencia es obligatorio")
        LeaveType leaveType,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate startDate,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate endDate
) {}
