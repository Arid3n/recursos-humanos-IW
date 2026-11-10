package com.iw.rrhh.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePerformanceReviewRequest(
        @NotBlank @Size(max = 50) String period,
        @NotBlank @Size(max = 10000) String objectives,
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 10000) String comments
) {}
