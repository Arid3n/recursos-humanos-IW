package com.iw.rrhh.leave.dto;

/** Si el usuario en sesión puede crear solicitudes de ausencia y, si no, por qué (HU-05 FR-015, FR-017). */
public record LeaveEligibilityResponse(boolean canRequest, String reason) {}
