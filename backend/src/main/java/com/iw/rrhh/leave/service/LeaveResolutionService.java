package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.leave.dto.LeaveResolutionResponse;
import com.iw.rrhh.leave.dto.TeamLeaveRequestResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Resolución de solicitudes de ausencia por el responsable de equipo (HU-05).
 */
public interface LeaveResolutionService {

    PageResponse<TeamLeaveRequestResponse> getTeamPendingRequests(Pageable pageable);

    LeaveResolutionResponse approve(UUID requestId, String comment);

    LeaveResolutionResponse reject(UUID requestId, String reason);
}
