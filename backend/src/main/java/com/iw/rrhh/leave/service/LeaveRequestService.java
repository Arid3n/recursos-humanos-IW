package com.iw.rrhh.leave.service;

import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.dto.WorkingDaysCalculationResponse;

import java.time.LocalDate;

public interface LeaveRequestService {

    WorkingDaysCalculationResponse calculateWorkingDays(LocalDate startDate, LocalDate endDate);

    LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request);

    /** Si el usuario en sesión puede crear solicitudes (HU-05 FR-015, FR-017). */
    com.iw.rrhh.leave.dto.LeaveEligibilityResponse getMyEligibility();

    com.iw.rrhh.common.dto.PageResponse<com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse> getMyRequests(
            java.security.Principal principal,
            com.iw.rrhh.leave.domain.LeaveStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    void cancelRequest(java.security.Principal principal, java.util.UUID requestId);
}
