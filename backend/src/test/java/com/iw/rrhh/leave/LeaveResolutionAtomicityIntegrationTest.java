package com.iw.rrhh.leave;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU-05.CA-4 / Art. 13.7: si falla la creación de la notificación, la decisión tampoco se registra.
 * Clase aparte porque sustituye NotificationService por un mock en todo el contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class LeaveResolutionAtomicityIntegrationTest {

    private static final String MANAGER = "resp.atomicidad@empresa.com";
    private static final String MEMBER = "miembro.atomicidad@empresa.com";

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

    private Employee employee(String username, UserRole role, String nationalId, Employee manager) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, "hash", role)));
        Employee employee = employeeRepository.findByEmail(username).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(user);
            e.setFirstName("Prueba");
            e.setLastName("Atomicidad");
            e.setEmail(username);
            e.setNationalId(nationalId);
            e.setHireDate(LocalDate.of(2025, 1, 1));
            return e;
        });
        employee.setManager(manager);
        return employeeRepository.save(employee);
    }

    @Test
    @DisplayName("HU-05.CA-4: un fallo al crear la notificación revierte la resolución (todo o nada)")
    void notificationFailure_rollsBackResolution() throws Exception {
        Employee manager = employee(MANAGER, UserRole.RESPONSABLE_EQUIPO, "30000005E", null);
        Employee member = employee(MEMBER, UserRole.EMPLEADO, "30000006F", manager);
        LeaveRequest pending = leaveRequestRepository.save(
                new LeaveRequest(member, LeaveType.PERMIT, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1), 1));

        doThrow(new IllegalStateException("Fallo simulado")).when(notificationService)
                .createNotification(any(), anyString(), anyString());

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId())
                        .with(user(MANAGER).roles(UserRole.RESPONSABLE_EQUIPO.name())))
                .andExpect(status().is5xxServerError());

        LeaveRequest stored = leaveRequestRepository.findById(pending.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(stored.getResolvedAt()).isNull();
    }
}
