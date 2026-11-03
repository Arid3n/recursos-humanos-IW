package com.iw.rrhh.leave.dto;

import com.iw.rrhh.leave.domain.LeaveStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de la resolución. {@code reason} contiene el motivo del rechazo o el comentario de la aprobación.
 */
public record LeaveResolutionResponse(
    UUID id,
    LeaveStatus status,
    String reason,
    UUID resolvedById,
    Instant resolvedAt
) {}
