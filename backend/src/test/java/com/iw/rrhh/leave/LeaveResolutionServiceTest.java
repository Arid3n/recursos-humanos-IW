package com.iw.rrhh.leave;

import com.iw.rrhh.common.exception.InsufficientLeaveBalanceException;
import com.iw.rrhh.common.exception.LeaveRequestAlreadyResolvedException;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveResolutionResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.LeaveBalanceService;
import com.iw.rrhh.leave.service.LeaveResolutionServiceImpl;
import com.iw.rrhh.notification.service.NotificationService;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveResolutionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private NotificationService notificationService;

    private LeaveResolutionServiceImpl service;

    private Employee manager;
    private Employee member;
    private final UUID requestId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new LeaveResolutionServiceImpl(
                leaveRequestRepository,
                employeeRepository,
                userRepository,
                leaveBalanceService,
                notificationService,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        User managerUser = new User("responsable@empresa.com", "hash", UserRole.RESPONSABLE_EQUIPO);
        manager = new Employee();
        manager.setId(UUID.randomUUID());
        manager.setUser(managerUser);
        manager.setFirstName("Carlos");
        manager.setLastName("Ruiz");

        member = new Employee();
        member.setId(UUID.randomUUID());
        member.setFirstName("Laura");
        member.setLastName("Martínez");
        member.setManager(manager);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(managerUser.getUsername(), "token", Collections.emptyList())
        );
        lenient().when(userRepository.findByUsername(managerUser.getUsername())).thenReturn(Optional.of(managerUser));
        lenient().when(employeeRepository.findByUserId(managerUser.getId())).thenReturn(Optional.of(manager));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private LeaveRequest pendingRequest(LeaveType type, int days) {
        LeaveRequest request = new LeaveRequest(member, type, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), days);
        request.setId(requestId);
        when(leaveRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
        return request;
    }

    private void balanceAvailable(int availableDays) {
        when(employeeRepository.findByIdForUpdate(member.getId())).thenReturn(Optional.of(member));
        when(leaveBalanceService.getEmployeeLeaveBalance(member.getId(), 2026)).thenReturn(
                new LeaveBalanceResponse(member.getId(), 2026, 22, 22 - availableDays, 0, availableDays, false, "General"));
    }

    @Test
    @DisplayName("HU-05.CA-2 / HU-05.CA-4: aprobar registra estado, responsable, momento y notifica al empleado")
    void approve_setsResolutionAndNotifies() {
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);
        balanceAvailable(10);

        LeaveResolutionResponse response = service.approve(requestId, "  Disfruta  ");

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(request.getResolvedBy()).isEqualTo(manager);
        assertThat(request.getResolvedAt()).isEqualTo(NOW);
        assertThat(request.getRejectionReason()).isEqualTo("Disfruta");
        assertThat(response.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(response.resolvedById()).isEqualTo(manager.getId());
        verify(notificationService).createNotification(eq(member), eq("Solicitud aprobada"), contains("Comentario: Disfruta"));
    }

    @Test
    @DisplayName("HU-05.CA-2: aprobar sin comentario no registra motivo")
    void approve_withoutComment() {
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);
        balanceAvailable(10);

        service.approve(requestId, "   ");

        assertThat(request.getRejectionReason()).isNull();
        verify(notificationService).createNotification(eq(member), eq("Solicitud aprobada"), any());
    }

    @Test
    @DisplayName("HU-05.CA-2 / HU-05.CA-4: rechazar guarda el motivo normalizado y notifica con el motivo")
    void reject_storesReasonAndNotifies() {
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);

        service.reject(requestId, "  Coincide con la entrega del proyecto  ");

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(request.getRejectionReason()).isEqualTo("Coincide con la entrega del proyecto");
        assertThat(request.getResolvedBy()).isEqualTo(manager);
        assertThat(request.getResolvedAt()).isEqualTo(NOW);
        verify(notificationService).createNotification(eq(member), eq("Solicitud rechazada"),
                contains("Motivo: Coincide con la entrega del proyecto"));
        verifyNoInteractions(leaveBalanceService);
    }

    @Test
    @DisplayName("HU-05.CA-2: rechazar con motivo de menos de 5 caracteres (tras recortar espacios) se bloquea")
    void reject_shortReason_isRejected() {
        assertThatThrownBy(() -> service.reject(requestId, "  abc   "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(leaveRequestRepository, notificationService);
    }

    @Test
    @DisplayName("HU-05.CA-3: una solicitud ya aprobada no puede volver a resolverse")
    void resolveTwice_conflict() {
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);
        request.setStatus(LeaveStatus.APPROVED);

        assertThatThrownBy(() -> service.approve(requestId, null))
                .isInstanceOf(LeaveRequestAlreadyResolvedException.class);
        assertThatThrownBy(() -> service.reject(requestId, "Motivo suficiente"))
                .isInstanceOf(LeaveRequestAlreadyResolvedException.class);

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        verifyNoInteractions(notificationService, leaveBalanceService);
    }

    @Test
    @DisplayName("HU-05.CA-1: un responsable no puede resolver solicitudes de otro equipo")
    void otherTeam_accessDenied() {
        Employee otherManager = new Employee();
        otherManager.setId(UUID.randomUUID());
        member.setManager(otherManager);
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);

        assertThatThrownBy(() -> service.approve(requestId, null))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verifyNoInteractions(notificationService, leaveBalanceService);
    }

    @Test
    @DisplayName("HU-05.CA-5: aprobar vacaciones que superan el saldo disponible se bloquea sin notificar")
    void approve_insufficientBalance() {
        LeaveRequest request = pendingRequest(LeaveType.VACATION, 5);
        balanceAvailable(3);

        assertThatThrownBy(() -> service.approve(requestId, null))
                .isInstanceOf(InsufficientLeaveBalanceException.class);

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("HU-05.CA-5: aprobar un permiso no consulta ni bloquea el saldo de vacaciones")
    void approve_permit_skipsBalance() {
        LeaveRequest request = pendingRequest(LeaveType.PERMIT, 2);

        service.approve(requestId, null);

        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        verifyNoInteractions(leaveBalanceService);
        verify(employeeRepository, never()).findByIdForUpdate(any());
        verify(notificationService).createNotification(eq(member), eq("Solicitud aprobada"), any());
    }

    @Test
    @DisplayName("Solicitud inexistente (p. ej. cancelada por el empleado) devuelve no encontrada")
    void unknownRequest_notFound() {
        when(leaveRequestRepository.findByIdForUpdate(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(requestId, null))
                .isInstanceOf(java.util.NoSuchElementException.class);
        verify(leaveBalanceService, never()).getEmployeeLeaveBalance(any(), anyInt());
    }
}
