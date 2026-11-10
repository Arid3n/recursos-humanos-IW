package com.iw.rrhh.performance.dto;

import java.util.UUID;

public record PerformanceReviewEmployeeResponse(
        UUID id,
        String fullName
) {}
