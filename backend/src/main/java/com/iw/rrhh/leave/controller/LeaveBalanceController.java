package com.iw.rrhh.leave.controller;

import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveSummaryItemResponse;
import com.iw.rrhh.leave.service.LeaveBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leave")
@Tag(name = "LeaveBalance", description = "Consulta de saldo de vacaciones y solicitudes (HU-02)")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService) {
        this.leaveBalanceService = leaveBalanceService;
    }

    @GetMapping("/balances/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar saldo de vacaciones del empleado autenticado")
    public ResponseEntity<LeaveBalanceResponse> getMyLeaveBalance(
            @RequestParam(required = false) Integer year) {
        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(year);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/balances/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consultar saldo de vacaciones de un empleado por ID")
    public ResponseEntity<LeaveBalanceResponse> getEmployeeLeaveBalance(
            @PathVariable UUID employeeId,
            @RequestParam(required = false) Integer year) {
        LeaveBalanceResponse balance = leaveBalanceService.getEmployeeLeaveBalance(employeeId, year);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/requests/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listado de solicitudes del empleado autenticado para el año fiscal")
    public ResponseEntity<List<LeaveSummaryItemResponse>> getMyLeaveRequests(
            @RequestParam(required = false) Integer year) {
        List<LeaveSummaryItemResponse> requests = leaveBalanceService.getMyLeaveRequests(year);
        return ResponseEntity.ok(requests);
    }
}

