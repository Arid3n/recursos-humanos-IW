package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.config.ClockConfig;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveSummaryItemResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveDayAllocator leaveDayAllocator;
    private final Clock clock;

    public LeaveBalanceServiceImpl(
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            ContractRecordRepository contractRecordRepository,
            LeaveRequestRepository leaveRequestRepository,
            LeaveDayAllocator leaveDayAllocator,
            Clock clock) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveDayAllocator = leaveDayAllocator;
        // El año "en curso" se evalúa en la zona de negocio, no en la del servidor (Art. 14.2)
        this.clock = clock.withZone(ClockConfig.BUSINESS_ZONE);
    }

    @Override
    public LeaveBalanceResponse getMyLeaveBalance(Integer year) {
        Employee employee = getAuthenticatedEmployee();
        return calculateBalance(employee, year);
    }

    @Override
    public LeaveBalanceResponse getEmployeeLeaveBalance(UUID employeeId, Integer year) {
        Employee caller = getAuthenticatedEmployee();
        Employee target = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employeeId));

        User callerUser = caller.getUser();
        UserRole role = callerUser.getRole();

        // Reglas de alcance (Art. 10):
        // RRHH: acceso total
        // RESPONSABLE_EQUIPO y DIRECCION_GENERAL: solo si es él mismo o de su equipo directo
        // EMPLEADO: solo si es él mismo
        if (role == UserRole.RRHH) {
            return calculateBalance(target, year);
        }

        if (caller.getId().equals(target.getId())) {
            return calculateBalance(target, year);
        }

        if (role == UserRole.RESPONSABLE_EQUIPO || role == UserRole.DIRECCION_GENERAL) {
            if (target.getManager() != null && target.getManager().getId().equals(caller.getId())) {
                return calculateBalance(target, year);
            }
        }

        throw new AccessDeniedException("No tiene autorización para consultar el saldo del empleado indicado");
    }

    @Override
    public List<LeaveSummaryItemResponse> getMyLeaveRequests(Integer year) {
        Employee employee = getAuthenticatedEmployee();
        int targetYear = (year != null) ? year : LocalDate.now(clock).getYear();
        LocalDate startOfYear = LocalDate.of(targetYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(targetYear, 12, 31);

        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeAndYear(
                employee.getId(), startOfYear, endOfYear);

        return requests.stream()
                .map(r -> new LeaveSummaryItemResponse(
                        r.getId(),
                        r.getLeaveType(),
                        r.getStartDate(),
                        r.getEndDate(),
                        r.getCalculatedDays(),
                        r.getStatus(),
                        r.getRejectionReason()
                ))
                .toList();
    }

    private LeaveBalanceResponse calculateBalance(Employee employee, Integer year) {
        LocalDate today = LocalDate.now(clock);
        int currentYear = today.getYear();
        int targetYear = (year != null) ? year : currentYear;
        boolean isClosedYear = targetYear < currentYear;

        // Fecha de referencia contractual: 31 de diciembre si el año ya cerró, o la fecha actual
        LocalDate referenceDate = isClosedYear ? LocalDate.of(targetYear, 12, 31) : today;

        Optional<ContractRecord> contractOpt = contractRecordRepository.findCurrentContract(employee.getId(), referenceDate);

        int baseAgreementDays = contractOpt
                .map(c -> c.getCollectiveAgreement().getVacationDaysPerYear())
                .orElse(22);

        String agreementName = contractOpt
                .map(c -> c.getCollectiveAgreement().getName())
                .orElse("Convenio Colectivo General");

        // Cálculo de días correspondientes con prorrateo (Art. 12, FR-006)
        int entitledDays;
        int hireYear = employee.getHireDate().getYear();
        if (hireYear == targetYear) {
            int remainingMonths = 12 - employee.getHireDate().getMonthValue() + 1;
            entitledDays = (int) Math.round((baseAgreementDays / 12.0) * remainingMonths);
        } else if (hireYear > targetYear) {
            entitledDays = 0;
        } else {
            entitledDays = baseAgreementDays;
        }

        LocalDate startOfYear = LocalDate.of(targetYear, 1, 1);
        LocalDate endOfYear = LocalDate.of(targetYear, 12, 31);

        // Una solicitud que cruza años cuenta en cada año con los días que le tocan (HU-05, aclaración C5)
        int spentDays = sumDaysInYear(employee, LeaveStatus.APPROVED, targetYear, startOfYear, endOfYear);
        int pendingDays = sumDaysInYear(employee, LeaveStatus.PENDING, targetYear, startOfYear, endOfYear);

        int availableDays = Math.max(0, entitledDays - spentDays);

        return new LeaveBalanceResponse(
                employee.getId(),
                targetYear,
                entitledDays,
                spentDays,
                pendingDays,
                availableDays,
                isClosedYear,
                agreementName
        );
    }

    private int sumDaysInYear(Employee employee, LeaveStatus status, int year, LocalDate startOfYear, LocalDate endOfYear) {
        return leaveRequestRepository
                .findOverlappingByYear(employee.getId(), LeaveType.VACATION, status, startOfYear, endOfYear)
                .stream()
                .mapToInt(request -> leaveDayAllocator.daysInYear(request, year))
                .sum();
    }

    private Employee getAuthenticatedEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado: " + username));

        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("No existe ficha de empleado asociada al usuario: " + username));
    }
}
