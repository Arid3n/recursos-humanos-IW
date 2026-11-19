package com.iw.rrhh.leave.service;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Cancelación automática de las solicitudes PENDING de un empleado cuyo superior deja de ser
 * válido (HU-05 FR-016). Participa en la transacción del llamante (Art. 13.7): no borra filas.
 */
@Service
@Transactional
public class LeaveRequestAutoCancelService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestAutoCancelService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public LeaveRequestAutoCancelService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService,
            Clock clock) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /** Cancela las PENDING del empleado y le envía una única notificación; devuelve cuántas canceló. */
    public int cancelPendingOf(Employee employee, String reason) {
        // Mismo bloqueo que la resolución: no se cruza con una aprobación simultánea (research §1.18)
        Employee locked = employeeRepository.findByIdForUpdate(employee.getId())
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employee.getId()));

        List<LeaveRequest> pending = leaveRequestRepository.findByEmployeeIdAndStatus(locked.getId(), LeaveStatus.PENDING);
        if (pending.isEmpty()) {
            return 0;
        }

        Instant now = Instant.now(clock);
        for (LeaveRequest request : pending) {
            request.cancel(now);
            leaveRequestRepository.save(request);
        }

        notificationService.createNotification(
                locked,
                "Solicitudes pendientes canceladas",
                String.format("Se han cancelado %d solicitud(es) pendiente(s): %s.", pending.size(), reason));

        log.info("Cancelación automática de solicitudes pendientes: empleado={}, canceladas={}", locked.getId(), pending.size());
        return pending.size();
    }
}
