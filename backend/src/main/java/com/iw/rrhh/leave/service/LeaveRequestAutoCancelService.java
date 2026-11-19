package com.iw.rrhh.leave.service;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Cancelación automática de las solicitudes PENDING de un empleado cuyo superior deja de ser
 * válido (HU-05 FR-016). Participa en la transacción del llamante (Art. 13.7).
 */
@Service
@Transactional
public class LeaveRequestAutoCancelService {

    public LeaveRequestAutoCancelService(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            NotificationService notificationService,
            Clock clock) {
    }

    /** Cancela las PENDING del empleado y le envía una única notificación; devuelve cuántas canceló. */
    public int cancelPendingOf(Employee employee, String reason) {
        throw new UnsupportedOperationException("Pendiente de implementar (T073)");
    }
}
