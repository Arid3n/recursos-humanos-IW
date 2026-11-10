package com.iw.rrhh.performance.service;

import com.iw.rrhh.performance.dto.AcknowledgePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.CreatePerformanceReviewRequest;
import com.iw.rrhh.performance.dto.PerformanceReviewEmployeeResponse;
import com.iw.rrhh.performance.dto.PerformanceReviewResponse;

import java.util.List;
import java.util.UUID;

public interface PerformanceReviewService {
    List<PerformanceReviewResponse> getMyReviews();
    List<PerformanceReviewResponse> getTeamReviews();
    List<PerformanceReviewEmployeeResponse> getTeamEmployees();
    PerformanceReviewResponse createReview(UUID employeeId, CreatePerformanceReviewRequest request);
    PerformanceReviewResponse acknowledge(UUID reviewId, AcknowledgePerformanceReviewRequest request);
}
