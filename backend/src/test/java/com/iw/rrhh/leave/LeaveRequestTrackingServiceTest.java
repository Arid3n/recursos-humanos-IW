package com.iw.rrhh.leave;

import com.iw.rrhh.common.dto.PageResponse;
import com.iw.rrhh.common.exception.LeaveRequestNotCancellableException;
import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.LeaveRequestSummaryResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.LeaveBalanceService;
import com.iw.rrhh.leave.service.LeaveRequestServiceImpl;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestTrackingServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private HolidayCalendarService holidayCalendarService;

    @InjectMocks
    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private User user;
    private Principal principal;

    @BeforeEach
    void setUp() {
        user = new User("empleado@empresa.com", "encodedPass", UserRole.EMPLEADO);
        user.setId(UUID.randomUUID());

        employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setUser(user);
        employee.setFirstName("Empleado");
        employee.setLastName("Pruebas");
        employee.setEmail("empleado@empresa.com");

        principal = () -> "empleado@empresa.com";

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("empleado@empresa.com", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findByUsername("empleado@empresa.com")).thenReturn(Optional.of(user));
        lenient().when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
    }

    @Test
    @DisplayName("HU-04.CA-1: Consulta paginada de solicitudes del empleado sin filtro")
    void test_HU04_CA1_getMyRequests_returnsPagedList() {
        Pageable pageable = PageRequest.of(0, 10);
        LeaveRequest r1 = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        r1.setId(UUID.randomUUID());
        r1.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(r1), pageable, 1));

        PageResponse<LeaveRequestSummaryResponse> result = leaveRequestService.getMyRequests(principal, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
        LeaveRequestSummaryResponse summary = result.content().get(0);
        assertThat(summary.id()).isEqualTo(r1.getId());
        assertThat(summary.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(summary.canCancel()).isTrue();
    }

    @Test
    @DisplayName("HU-04.CA-4: Consulta filtrando por estado de solicitud")
    void test_HU04_CA4_getMyRequests_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        LeaveRequest r1 = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        r1.setId(UUID.randomUUID());
        r1.setStatus(LeaveStatus.APPROVED);

        when(leaveRequestRepository.findByEmployeeIdAndStatusOrderByCreatedAtDesc(employee.getId(), LeaveStatus.APPROVED, pageable))
                .thenReturn(new PageImpl<>(List.of(r1), pageable, 1));

        PageResponse<LeaveRequestSummaryResponse> result = leaveRequestService.getMyRequests(principal, LeaveStatus.APPROVED, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(result.content().get(0).canCancel()).isFalse();
    }

    @Test
    @DisplayName("HU-04.CA-2: El motivo de rechazo solo se expone en estado REJECTED")
    void test_HU04_CA2_rejectionReason_onlyForRejected() {
        Pageable pageable = PageRequest.of(0, 10);
        LeaveRequest rejected = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        rejected.setId(UUID.randomUUID());
        rejected.setStatus(LeaveStatus.REJECTED);
        rejected.setRejectionReason("Coincidencia de personal crítico");

        when(leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(rejected), pageable, 1));

        PageResponse<LeaveRequestSummaryResponse> result = leaveRequestService.getMyRequests(principal, null, pageable);

        assertThat(result.content().get(0).rejectionReason()).isEqualTo("Coincidencia de personal crítico");
    }

    @Test
    @DisplayName("HU-04.CA-3: Cancelación exitosa de solicitud en estado PENDING")
    void test_HU04_CA3_cancelRequest_successWhenPending() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest pending = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        pending.setId(requestId);
        pending.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(pending));

        leaveRequestService.cancelRequest(principal, requestId);

        verify(leaveRequestRepository).delete(pending);
    }

    @Test
    @DisplayName("HU-04: Intento de cancelar solicitud ya APROBADA lanza 409 Conflict")
    void test_HU04_cancelRequest_throwsConflictWhenApproved() {
        UUID requestId = UUID.randomUUID();
        LeaveRequest approved = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        approved.setId(requestId);
        approved.setStatus(LeaveStatus.APPROVED);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> leaveRequestService.cancelRequest(principal, requestId))
                .isInstanceOf(LeaveRequestNotCancellableException.class)
                .hasMessageContaining("Solo se pueden cancelar solicitudes en estado PENDIENTE");

        verify(leaveRequestRepository, never()).delete(any());
    }

    @Test
    @DisplayName("HU-04.CA-5: Intento de cancelar solicitud de otro empleado lanza AccessDeniedException (Art. 10)")
    void test_HU04_CA5_cancelRequest_throwsAccessDeniedWhenNotOwner() {
        UUID requestId = UUID.randomUUID();
        Employee anotherEmployee = new Employee();
        anotherEmployee.setId(UUID.randomUUID());

        LeaveRequest otherRequest = new LeaveRequest(anotherEmployee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        otherRequest.setId(requestId);
        otherRequest.setStatus(LeaveStatus.PENDING);

        when(leaveRequestRepository.findById(requestId)).thenReturn(Optional.of(otherRequest));

        assertThatThrownBy(() -> leaveRequestService.cancelRequest(principal, requestId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permiso para cancelar una solicitud que no le pertenece");

        verify(leaveRequestRepository, never()).delete(any());
    }
}
