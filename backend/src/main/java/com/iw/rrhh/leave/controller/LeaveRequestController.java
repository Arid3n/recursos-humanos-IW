package com.iw.rrhh.leave.controller;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse;
import com.iw.rrhh.leave.dto.WorkingDaysCalculationResponse;
import com.iw.rrhh.leave.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/leave")
@Tag(name = "LeaveRequests", description = "Endpoints para la gestión, solicitud y seguimiento de vacaciones y permisos (HU-03 y HU-04)")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping("/requests/calculate-days")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calcular días laborables entre dos fechas según el calendario oficial")
    public ResponseEntity<WorkingDaysCalculationResponse> calculateWorkingDays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WorkingDaysCalculationResponse response = leaveRequestService.calculateWorkingDays(startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear nueva solicitud de vacaciones o permiso en estado PENDING")
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request) {
        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping({"/my-requests", "/requests/my"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener histórico de solicitudes del empleado en sesión")
    public ResponseEntity<PageResponse<LeaveRequestSummaryResponse>> getMyRequests(
            Principal principal,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        PageResponse<LeaveRequestSummaryResponse> response = leaveRequestService.getMyRequests(principal, status, pageRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar una solicitud pendiente propia")
    public ResponseEntity<Void> cancelLeaveRequest(
            Principal principal,
            @PathVariable UUID id) {
        leaveRequestService.cancelRequest(principal, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar una solicitud pendiente propia (método DELETE)")
    public ResponseEntity<Void> deleteLeaveRequest(
            Principal principal,
            @PathVariable UUID id) {
        leaveRequestService.cancelRequest(principal, id);
        return ResponseEntity.noContent().build();
    }
}
