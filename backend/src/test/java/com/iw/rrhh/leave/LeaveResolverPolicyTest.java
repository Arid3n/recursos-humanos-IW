package com.iw.rrhh.leave;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.leave.service.LeaveResolverPolicy;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** HU-05 FR-015: regla única de «resolutor válido» (Art. 14.3). */
class LeaveResolverPolicyTest {

    private LeaveResolverPolicy policy;
    private Employee employee;

    @BeforeEach
    void setUp() {
        policy = new LeaveResolverPolicy();
        employee = new Employee();
        employee.setId(UUID.randomUUID());
    }

    private Employee manager(UserRole role) {
        Employee manager = new Employee();
        manager.setId(UUID.randomUUID());
        manager.setUser(new User("jefe@empresa.com", "hash", role));
        return manager;
    }

    @Test
    @DisplayName("Sin responsable: no hay resolutor válido")
    void noManager_isInvalid() {
        employee.setManager(null);
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("Responsable inactivo: no hay resolutor válido")
    void inactiveManager_isInvalid() {
        Employee manager = manager(UserRole.RESPONSABLE_EQUIPO);
        manager.setActive(false);
        employee.setManager(manager);
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("Responsable con cuenta de usuario desactivada: no hay resolutor válido")
    void managerWithInactiveUser_isInvalid() {
        Employee manager = manager(UserRole.RESPONSABLE_EQUIPO);
        manager.getUser().setActive(false);
        employee.setManager(manager);
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("Responsable sin cuenta de usuario: no hay resolutor válido y no falla (null-safe)")
    void managerWithoutUser_isInvalid() {
        Employee manager = new Employee();
        manager.setId(UUID.randomUUID());
        employee.setManager(manager);
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("Responsable con rol EMPLEADO: no hay resolutor válido")
    void managerWithEmployeeRole_isInvalid() {
        employee.setManager(manager(UserRole.EMPLEADO));
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("FR-018: responsable activo con rol RRHH: resolutor válido")
    void managerWithHrRole_isValid() {
        employee.setManager(manager(UserRole.RRHH));
        assertThat(policy.hasValidResolver(employee)).isTrue();
    }

    @Test
    @DisplayName("Responsable autorreferenciado (él mismo): no hay resolutor válido")
    void selfReferencingManager_isInvalid() {
        employee.setUser(new User("yo@empresa.com", "hash", UserRole.RESPONSABLE_EQUIPO));
        employee.setManager(employee);
        assertThat(policy.hasValidResolver(employee)).isFalse();
    }

    @Test
    @DisplayName("Responsable activo con rol RESPONSABLE_EQUIPO: resolutor válido")
    void managerRole_isValid() {
        employee.setManager(manager(UserRole.RESPONSABLE_EQUIPO));
        assertThat(policy.hasValidResolver(employee)).isTrue();
    }

    @Test
    @DisplayName("Responsable activo con rol DIRECCION_GENERAL: resolutor válido")
    void direccionGeneral_isValid() {
        employee.setManager(manager(UserRole.DIRECCION_GENERAL));
        assertThat(policy.hasValidResolver(employee)).isTrue();
    }
}
