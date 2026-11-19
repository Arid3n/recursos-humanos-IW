package com.iw.rrhh.employee;

import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.notification.service.NotificationService;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-05 FR-016 / Art. 13.7: si falla la cancelación automática (aquí, la notificación), la baja de HU-01
 * tampoco se registra. Clase aparte porque sustituye NotificationService por un mock en todo el contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class EmployeeDeactivationAtomicityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @MockitoBean
    private NotificationService notificationService;

    private Employee employee(String prefix, UserRole role, String nationalId, Employee manager) {
        String email = prefix + "." + UUID.randomUUID() + "@empresa.com";
        User user = userRepository.save(new User(email, "hash", role));
        Employee e = new Employee();
        e.setUser(user);
        e.setFirstName("Prueba");
        e.setLastName("Atomicidad");
        e.setEmail(email);
        e.setNationalId(nationalId);
        e.setHireDate(LocalDate.of(2025, 1, 1));
        e.setManager(manager);
        return employeeRepository.save(e);
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-016: un fallo en la cancelación revierte la baja del responsable (todo o nada)")
    void cancellationFailure_rollsBackDeactivation() throws Exception {
        Employee manager = employee("resp.atom", UserRole.RESPONSABLE_EQUIPO, "40000020A", null);
        Employee report = employee("sub.atom", UserRole.EMPLEADO, "40000021B", manager);
        LeaveRequest pending = leaveRequestRepository.save(new LeaveRequest(
                report, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1), 1));

        doThrow(new IllegalStateException("Fallo simulado")).when(notificationService)
                .createNotification(any(), anyString(), anyString());

        mockMvc.perform(delete("/employees/" + manager.getId()))
                .andExpect(status().is5xxServerError());

        assertThat(employeeRepository.findById(manager.getId()).orElseThrow().isActive()).isTrue();
        assertThat(userRepository.findById(manager.getUser().getId()).orElseThrow().isActive()).isTrue();
        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
    }
}
