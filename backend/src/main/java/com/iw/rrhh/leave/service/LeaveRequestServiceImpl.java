package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.exception.InsufficientLeaveBalanceException;
import com.iw.rrhh.common.exception.LeaveRequestOverlapException;
import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.dto.WorkingDaysCalculationResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final HolidayCalendarService holidayCalendarService;

    public LeaveRequestServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            LeaveBalanceService leaveBalanceService,
            HolidayCalendarService holidayCalendarService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.leaveBalanceService = leaveBalanceService;
        this.holidayCalendarService = holidayCalendarService;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkingDaysCalculationResponse calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        HolidayCalendarService.WorkingDaysBreakdown breakdown = holidayCalendarService.calculateBreakdown(startDate, endDate);
        return new WorkingDaysCalculationResponse(
                breakdown.startDate(),
                breakdown.endDate(),
                breakdown.workingDays(),
                breakdown.calendarDays(),
                breakdown.weekendDays(),
                breakdown.holidayDays()
        );
    }

    @Override
    public LeaveRequestResponse createLeaveRequest(CreateLeaveRequest request) {
        if (request.startDate() == null || request.endDate() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        Employee employee = getAuthenticatedEmployee();

        // 1. Cómputo de días laborables en servidor (Art. 14)
        int workingDays = holidayCalendarService.countWorkingDays(request.startDate(), request.endDate());
        if (workingDays <= 0) {
            throw new IllegalArgumentException("El rango de fechas seleccionado no contiene ningún día laborable");
        }

        // 2. Comprobación preventiva de solapamientos (FR-007)
        boolean hasOverlap = leaveRequestRepository.hasOverlappingRequests(
                employee.getId(),
                request.startDate(),
                request.endDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED)
        );
        if (hasOverlap) {
            throw new LeaveRequestOverlapException();
        }

        // 3. Validación de saldo disponible si es VACATION (Art. 12, FR-004)
        if (request.leaveType() == LeaveType.VACATION) {
            int year = request.startDate().getYear();
            LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(year);
            if (workingDays > balance.availableDays()) {
                throw new InsufficientLeaveBalanceException(workingDays, balance.availableDays());
            }
        }

        // 4. Instanciación y persistencia en estado PENDING asignada al manager (Art. 13, FR-003, FR-006)
        LeaveRequest leaveRequest = new LeaveRequest(
                employee,
                request.leaveType(),
                request.startDate(),
                request.endDate(),
                workingDays
        );
        leaveRequest.setStatus(LeaveStatus.PENDING);
        if (employee.getManager() != null) {
            leaveRequest.setResolvedBy(employee.getManager());
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        UUID managerId = employee.getManager() != null ? employee.getManager().getId() : null;
        String employeeFullName = employee.getFirstName() + " " + employee.getLastName();

        return new LeaveRequestResponse(
                saved.getId(),
                employee.getId(),
                employeeFullName,
                managerId,
                saved.getLeaveType(),
                saved.getStartDate(),
                saved.getEndDate(),
                saved.getCalculatedDays(),
                saved.getStatus(),
                saved.getCreatedAt()
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

    @Override
    @Transactional(readOnly = true)
    public com.iw.rrhh.common.dto.PageResponse<com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse> getMyRequests(
            java.security.Principal principal,
            LeaveStatus status,
            org.springframework.data.domain.Pageable pageable
    ) {
        Employee employee = getAuthenticatedEmployee();
        org.springframework.data.domain.Page<LeaveRequest> page;
        if (status != null) {
            page = leaveRequestRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(employee.getId(), status, pageable);
        } else {
            page = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), pageable);
        }

        org.springframework.data.domain.Page<com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse> dtoPage = page.map(this::mapToSummary);
        return com.iw.rrhh.common.dto.PageResponse.from(dtoPage);
    }

    @Override
    @Transactional
    public void cancelRequest(java.security.Principal principal, UUID requestId) {
        Employee employee = getAuthenticatedEmployee();
        LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Solicitud de ausencia no encontrada con ID: " + requestId));

        if (!leaveRequest.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("No tiene permiso para cancelar una solicitud que no le pertenece");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new com.iw.rrhh.common.exception.LeaveRequestNotCancellableException("Solo se pueden cancelar solicitudes en estado PENDIENTE");
        }

        leaveRequestRepository.delete(leaveRequest);
    }

    private com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse mapToSummary(LeaveRequest lr) {
        return new com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse(
                lr.getId(),
                lr.getLeaveType(),
                lr.getStartDate(),
                lr.getEndDate(),
                lr.getCalculatedDays(),
                lr.getStatus(),
                lr.getStatus() == LeaveStatus.REJECTED ? lr.getRejectionReason() : null,
                lr.getCreatedAt(),
                lr.getStatus() == LeaveStatus.PENDING
        );
    }
}
