package com.iw.rrhh.security.dto;

import com.iw.rrhh.security.domain.UserRole;

import java.util.UUID;

public record UserSummaryResponse(
    UUID id,
    String username,
    UserRole role
) {
}
