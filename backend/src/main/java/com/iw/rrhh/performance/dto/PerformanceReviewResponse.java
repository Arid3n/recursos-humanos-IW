package com.iw.rrhh.performance.dto;

import com.iw.rrhh.performance.domain.PerformanceReview;

import java.time.Instant;
import java.util.UUID;

public record PerformanceReviewResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        String evaluatorName,
        String period,
        String objectives,
        Integer rating,
        String comments,
        String employeeComments,
        String status,
        Instant createdAt,
        Instant employeeAcknowledgedAt,
        Instant finalizedAt
) {
    public static PerformanceReviewResponse from(PerformanceReview review) {
        return new PerformanceReviewResponse(
                review.getId(),
                review.getEmployee().getId(),
                fullName(review.getEmployee()),
                fullName(review.getEvaluator()),
                review.getPeriod(),
                review.getObjectives(),
                review.getRating(),
                review.getComments(),
                review.getEmployeeComments(),
                review.getStatus().name(),
                review.getCreatedAt(),
                review.getEmployeeAcknowledgedAt(),
                review.getFinalizedAt());
    }

    private static String fullName(com.iw.rrhh.employee.domain.Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }
}
