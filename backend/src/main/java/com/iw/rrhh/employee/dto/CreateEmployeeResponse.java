package com.iw.rrhh.employee.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateEmployeeResponse(
    UUID id,
    String email,
    String fullName,
    String nationalId,
    String temporaryPassword,
    Long contractRecordId,
    Instant createdAt
) {}
