package com.iw.rrhh.leave.controller;

import com.iw.rrhh.leave.dto.TeamCalendarResponse;
import com.iw.rrhh.leave.service.TeamCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/leave")
@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")
@Tag(name = "TeamCalendar", description = "Calendario de ausencias del equipo del responsable (HU-06)")
public class TeamCalendarController {

    private final TeamCalendarService teamCalendarService;

    public TeamCalendarController(TeamCalendarService teamCalendarService) {
        this.teamCalendarService = teamCalendarService;
    }

    @GetMapping("/team-calendar")
    @Operation(summary = "Ausencias aprobadas y pendientes del equipo en un mes (formato YYYY-MM)")
    public ResponseEntity<TeamCalendarResponse> getTeamCalendar(@RequestParam YearMonth month) {
        return ResponseEntity.ok(teamCalendarService.getTeamCalendar(month));
    }
}
