package com.iw.rrhh.leave.service;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import org.springframework.stereotype.Component;

/**
 * Regla única de «resolutor válido» (HU-05 FR-015, Art. 14.3): responsable asignado, activo, con
 * cuenta de usuario activa, distinto del empleado y con rol RESPONSABLE_EQUIPO, RRHH (FR-018) o DIRECCION_GENERAL.
 */
@Component
public class LeaveResolverPolicy {

    public boolean hasValidResolver(Employee employee) {
        Employee manager = employee.getManager();
        if (manager == null || !manager.isActive()) {
            return false;
        }
        if (manager.getId() != null && manager.getId().equals(employee.getId())) {
            return false;
        }
        User managerUser = manager.getUser();
        if (managerUser == null || !managerUser.isActive()) {
            return false;
        }
        UserRole role = managerUser.getRole();
        return role == UserRole.RESPONSABLE_EQUIPO
                || role == UserRole.RRHH
                || role == UserRole.DIRECCION_GENERAL;
    }
}
