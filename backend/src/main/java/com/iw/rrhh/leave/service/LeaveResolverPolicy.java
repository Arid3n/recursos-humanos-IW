package com.iw.rrhh.leave.service;

import com.iw.rrhh.employee.domain.Employee;
import org.springframework.stereotype.Component;

/**
 * Regla única de «resolutor válido» (HU-05 FR-015, Art. 14.3): responsable asignado, activo,
 * distinto del empleado y con rol RESPONSABLE_EQUIPO o DIRECCION_GENERAL.
 */
@Component
public class LeaveResolverPolicy {

    public boolean hasValidResolver(Employee employee) {
        throw new UnsupportedOperationException("Pendiente de implementar (T070)");
    }
}
