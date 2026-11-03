package com.iw.rrhh.leave.service;

import com.iw.rrhh.leave.dto.TeamCalendarResponse;

import java.time.YearMonth;

/**
 * Calendario mensual de ausencias del equipo del responsable en sesión (HU-06).
 */
public interface TeamCalendarService {

    TeamCalendarResponse getTeamCalendar(YearMonth month);
}
