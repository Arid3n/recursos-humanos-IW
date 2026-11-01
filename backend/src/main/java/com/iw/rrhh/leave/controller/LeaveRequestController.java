package com.iw.rrhh.leave.controller;

import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.dto.WorkingDaysCalculationResponse;
import com.iw.rrhh.leave.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/leave/requests")
@Tag(name = "LeaveRequests", description = "Endpoints para la gestión y solicitud de vacaciones y permisos (HU-03)")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping("/calculate-days")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calcular días laborables entre dos fechas según el calendario oficial")
    public ResponseEntity<WorkingDaysCalculationResponse> calculateWorkingDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WorkingDaysCalculationResponse response = leaveRequestService.calculateWorkingDays(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear nueva solicitud de vacaciones o permiso en estado PENDING")
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
