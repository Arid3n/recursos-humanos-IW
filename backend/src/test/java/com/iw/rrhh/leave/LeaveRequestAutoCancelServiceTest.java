package com.iw.rrhh.leave;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.LeaveRequestAutoCancelService;
import com.iw.rrhh.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU-05 FR-016: cancelación automática de solicitudes pendientes. */
@ExtendWith(MockitoExtension.class)
class LeaveRequestAutoCancelServiceTest {

    private static final String REASON = "tu responsable ya no puede resolver tus solicitudes";

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private NotificationService notificationService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);

    private LeaveRequestAutoCancelService service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new LeaveRequestAutoCancelService(leaveRequestRepository, employeeRepository, notificationService, clock);
        employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName("Nombre Reservado");
        employee.setLastName("Apellido Reservado");
        employee.setEmail("reservado@empresa.com");
        when(employeeRepository.findByIdForUpdate(employee.getId())).thenReturn(Optional.of(employee));
    }

    private LeaveRequest pending(int startDay) {
        LeaveRequest request = new LeaveRequest(employee, LeaveType.VACATION,
                LocalDate.of(2026, 12, startDay), LocalDate.of(2026, 12, startDay + 1), 2);
        request.setId(UUID.randomUUID());
        return request;
    }

    @Test
    @DisplayName("Solo las PENDING pasan a CANCELLED; se consulta únicamente ese estado y no se borra ninguna fila")
    void cancelsOnlyPending_andDeletesNothing() {
        LeaveRequest first = pending(1);
        LeaveRequest second = pending(10);
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .thenReturn(List.of(first, second));

        int cancelled = service.cancelPendingOf(employee, REASON);

        assertThat(cancelled).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        verify(leaveRequestRepository, never()).findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.APPROVED);
        verify(leaveRequestRepository, never()).findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.REJECTED);
        verify(leaveRequestRepository, never()).delete(any(LeaveRequest.class));
        verify(leaveRequestRepository, never()).deleteAll();
        verify(leaveRequestRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Una única notificación por empleado, con el motivo, aunque tenga varias solicitudes pendientes")
    void sendsOneNotificationPerEmployee() {
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .thenReturn(List.of(pending(1), pending(10), pending(20)));

        service.cancelPendingOf(employee, REASON);

        verify(notificationService, times(1)).createNotification(eq(employee), anyString(), contains(REASON));
    }

    @Test
    @DisplayName("Sin solicitudes pendientes no se notifica y devuelve 0")
    void noPending_noNotification() {
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .thenReturn(List.of());

        int cancelled = service.cancelPendingOf(employee, REASON);

        assertThat(cancelled).isZero();
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("Bloquea la fila del empleado para no cruzarse con una aprobación simultánea (research §1.18)")
    void locksEmployeeRow() {
        when(leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                .thenReturn(List.of(pending(1)));

        service.cancelPendingOf(employee, REASON);

        verify(employeeRepository).findByIdForUpdate(employee.getId());
    }

    @Test
    @DisplayName("Audita con un INFO que lleva el UUID del empleado y no incluye datos personales (Art. 20.5)")
    void auditsWithoutPersonalData() {
        Logger logger = (Logger) LoggerFactory.getLogger(LeaveRequestAutoCancelService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            when(leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.PENDING))
                    .thenReturn(List.of(pending(1), pending(10)));

            service.cancelPendingOf(employee, REASON);

            assertThat(appender.list).filteredOn(e -> e.getLevel() == Level.INFO)
                    .singleElement()
                    .satisfies(e -> {
                        assertThat(e.getFormattedMessage()).contains(employee.getId().toString());
                        assertThat(e.getFormattedMessage())
                                .doesNotContain("Nombre Reservado", "Apellido Reservado", "reservado@empresa.com");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
