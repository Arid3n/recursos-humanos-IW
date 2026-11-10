package com.iw.rrhh.performance.dto;

import jakarta.validation.constraints.Size;

public record AcknowledgePerformanceReviewRequest(
        @Size(max = 10000) String employeeComments
) {}
