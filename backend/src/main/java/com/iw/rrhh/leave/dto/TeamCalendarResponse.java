package com.iw.rrhh.leave.dto;

import java.time.YearMonth;
import java.util.List;

public record TeamCalendarResponse(
    YearMonth month,
    List<TeamMemberResponse> members,
    List<TeamCalendarDayResponse> days
) {}
