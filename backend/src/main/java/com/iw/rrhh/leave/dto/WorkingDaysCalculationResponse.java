package com.iw.rrhh.leave.dto;

import java.time.LocalDate;

public record WorkingDaysCalculationResponse(
        LocalDate startDate,
        LocalDate endDate,
        int workingDays,
        int calendarDays,
        int weekendDays,
        int holidayDays
) {}
