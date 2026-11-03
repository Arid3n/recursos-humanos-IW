package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.CalendarDayType;

import java.time.LocalDate;
import java.util.List;

public record TeamCalendarDayResponse(
    LocalDate date,
    CalendarDayType dayType,
    String holidayName,
    int absentCount,
    boolean hasConflict,
    List<TeamCalendarEventResponse> events
) {}
