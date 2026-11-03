package com.iw.rrhh.leave.controller;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.leave.dto.ApproveLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveResolutionResponse;
import com.iw.rrhh.leave.dto.RejectLeaveRequest;
import com.iw.rrhh.leave.dto.TeamLeaveRequestResponse;
import com.iw.rrhh.leave.service.LeaveResolutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/leave")
@PreAuthorize("hasRole('RESPONSABLE_EQUIPO')")
@Tag(name = "LeaveResolution", description = "Bandeja y resolución de solicitudes del equipo por su responsable (HU-05)")
public class LeaveResolutionController {

    private final LeaveResolutionService leaveResolutionService;

    public LeaveResolutionController(LeaveResolutionService leaveResolutionService) {
        this.leaveResolutionService = leaveResolutionService;
    }

    @GetMapping("/team-requests")
    @Operation(summary = "Solicitudes pendientes de los miembros del equipo del responsable en sesión")
    public ResponseEntity<PageResponse<TeamLeaveRequestResponse>> getTeamPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(leaveResolutionService.getTeamPendingRequests(PageRequest.of(page, size)));
    }

    @PostMapping("/requests/{id}/approve")
    @Operation(summary = "Aprobar una solicitud pendiente del equipo (comentario opcional)")
    public ResponseEntity<LeaveResolutionResponse> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveLeaveRequest body) {
        String comment = body != null ? body.comment() : null;
        return ResponseEntity.ok(leaveResolutionService.approve(id, comment));
    }

    @PostMapping("/requests/{id}/reject")
    @Operation(summary = "Rechazar una solicitud pendiente del equipo indicando el motivo")
    public ResponseEntity<LeaveResolutionResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectLeaveRequest body) {
        return ResponseEntity.ok(leaveResolutionService.reject(id, body.reason()));
    }
}
