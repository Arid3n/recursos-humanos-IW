package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.common.exception.InsufficientLeaveBalanceException;
import com.iw.rrhh.common.exception.LeaveRequestAlreadyResolvedException;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveResolutionResponse;
import com.iw.rrhh.leave.dto.TeamLeaveRequestResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.notification.service.NotificationService;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class LeaveResolutionServiceImpl implements LeaveResolutionService {

    private static final Logger log = LoggerFactory.getLogger(LeaveResolutionServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MIN_REASON_LENGTH = 5;

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final NotificationService notificationService;
    private final Clock clock;

    public LeaveResolutionServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            LeaveBalanceService leaveBalanceService,
            NotificationService notificationService,
            Clock clock
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeamLeaveRequestResponse> getTeamPendingRequests(Pageable pageable) {
        Employee manager = getAuthenticatedEmployee();
        return PageResponse.from(
                leaveRequestRepository
                        .findByEmployeeManagerIdAndStatusOrderByCreatedAtAsc(manager.getId(), LeaveStatus.PENDING, pageable)
                        .map(this::toTeamResponse)
        );
    }

    @Override
    public LeaveResolutionResponse approve(UUID requestId, String comment) {
        Employee manager = getAuthenticatedEmployee();
        LeaveRequest request = loadInScope(requestId, manager);

        // Se comprueba el estado antes del saldo: una solicitud resuelta siempre devuelve 409 (Art. 13.2)
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new LeaveRequestAlreadyResolvedException();
        }
        if (request.getLeaveType() == LeaveType.VACATION) {
            ensureSufficientBalance(request);
        }

        String normalizedComment = normalize(comment);
        request.approve(manager, normalizedComment, Instant.now(clock));
        notificationService.createNotification(
                request.getEmployee(),
                "Solicitud aprobada",
                buildMessage(request, "aprobada", "Comentario", normalizedComment)
        );

        log.info("Auditoría: solicitud {} resuelta como {} por el empleado {}", request.getId(), request.getStatus(), manager.getId());
        return toResolutionResponse(request);
    }

    @Override
    public LeaveResolutionResponse reject(UUID requestId, String reason) {
        String normalizedReason = normalize(reason);
        if (normalizedReason == null || normalizedReason.length() < MIN_REASON_LENGTH) {
            throw new IllegalArgumentException("El motivo del rechazo debe tener al menos " + MIN_REASON_LENGTH + " caracteres");
        }

        Employee manager = getAuthenticatedEmployee();
        LeaveRequest request = loadInScope(requestId, manager);

        request.reject(manager, normalizedReason, Instant.now(clock));
        notificationService.createNotification(
                request.getEmployee(),
                "Solicitud rechazada",
                buildMessage(request, "rechazada", "Motivo", normalizedReason)
        );

        log.info("Auditoría: solicitud {} resuelta como {} por el empleado {}", request.getId(), request.getStatus(), manager.getId());
        return toResolutionResponse(request);
    }

    /**
     * Carga la solicitud bloqueada y verifica que pertenece a un miembro directo del responsable (Art. 10).
     */
    private LeaveRequest loadInScope(UUID requestId, Employee manager) {
        LeaveRequest request = leaveRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud de ausencia no encontrada"));

        Employee currentManager = request.getEmployee().getManager();
        if (currentManager == null || !currentManager.getId().equals(manager.getId())) {
            log.warn("Acceso denegado: el empleado {} intentó resolver la solicitud {} fuera de su equipo", manager.getId(), requestId);
            throw new AccessDeniedException("Solo puede resolver solicitudes de personas de su equipo");
        }
        return request;
    }

    /**
     * Bloquea la ficha del empleado y revalida el saldo derivado (Art. 12.6). Las aprobaciones
     * concurrentes del mismo empleado se serializan aquí y la segunda ve la primera ya confirmada.
     */
    private void ensureSufficientBalance(LeaveRequest request) {
        UUID employeeId = request.getEmployee().getId();
        employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado"));

        LeaveBalanceResponse balance = leaveBalanceService.getEmployeeLeaveBalance(employeeId, request.getStartDate().getYear());
        if (request.getCalculatedDays() > balance.availableDays()) {
            throw new InsufficientLeaveBalanceException(request.getCalculatedDays(), balance.availableDays());
        }
    }

    private String buildMessage(LeaveRequest request, String outcome, String reasonLabel, String reason) {
        String typeLabel = request.getLeaveType() == LeaveType.VACATION ? "vacaciones" : "permiso";
        String message = String.format("Tu solicitud de %s del %s al %s ha sido %s.",
                typeLabel,
                request.getStartDate().format(DATE_FORMAT),
                request.getEndDate().format(DATE_FORMAT),
                outcome);
        return reason == null ? message : message + " " + reasonLabel + ": " + reason;
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.strip();
    }

    private TeamLeaveRequestResponse toTeamResponse(LeaveRequest request) {
        Employee employee = request.getEmployee();
        return new TeamLeaveRequestResponse(
                request.getId(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                request.getLeaveType(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCalculatedDays(),
                request.getCreatedAt()
        );
    }

    private LeaveResolutionResponse toResolutionResponse(LeaveRequest request) {
        return new LeaveResolutionResponse(
                request.getId(),
                request.getStatus(),
                request.getRejectionReason(),
                request.getResolvedBy().getId(),
                request.getResolvedAt()
        );
    }

    private Employee getAuthenticatedEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuario de sesión no encontrado"));

        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("No se encontró ficha de empleado para el usuario autenticado"));
    }
}
