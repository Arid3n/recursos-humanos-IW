package com.iw.rrhh.leave.dto;

import java.util.UUID;

public record TeamMemberResponse(
    UUID id,
    String name
) {}
