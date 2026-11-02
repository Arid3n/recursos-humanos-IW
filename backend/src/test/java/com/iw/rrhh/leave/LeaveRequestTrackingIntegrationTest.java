package com.iw.rrhh.leave;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.CollectiveAgreementRepository;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class LeaveRequestTrackingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private CollectiveAgreementRepository collectiveAgreementRepository;

    @Autowired
    private ContractRecordRepository contractRecordRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Employee employee;
    private Employee anotherEmployee;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();

        String empUsername = "empleado.tracking@empresa.com";
        User user = userRepository.findByUsername(empUsername).orElseGet(() -> {
            User u = new User(empUsername, "hash", UserRole.EMPLEADO);
            return userRepository.save(u);
        });

        employee = employeeRepository.findByEmail(empUsername).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(user);
            e.setFirstName("Empleado");
            e.setLastName("Tracking");
            e.setEmail(empUsername);
            e.setNationalId("12345678Q");
            e.setHireDate(LocalDate.of(2025, 1, 1));
            return employeeRepository.save(e);
        });

        String otherUsername = "otro.empleado@empresa.com";
        User otherUser = userRepository.findByUsername(otherUsername).orElseGet(() -> {
            User u = new User(otherUsername, "hash", UserRole.EMPLEADO);
            return userRepository.save(u);
        });

        anotherEmployee = employeeRepository.findByEmail(otherUsername).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(otherUser);
            e.setFirstName("Otro");
            e.setLastName("Empleado");
            e.setEmail(otherUsername);
            e.setNationalId("87654321Q");
            e.setHireDate(LocalDate.of(2025, 1, 1));
            return employeeRepository.save(e);
        });
    }

    @Test
    @DisplayName("HU-04.CA-1: GET /api/v1/leave/my-requests devuelve listado paginado")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_CA1_getMyRequests_success() throws Exception {
        LeaveRequest r1 = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        r1.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(r1);

        mockMvc.perform(get("/leave/my-requests")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(r1.getId().toString())))
                .andExpect(jsonPath("$.content[0].status", is("PENDING")))
                .andExpect(jsonPath("$.content[0].canCancel", is(true)))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    @DisplayName("HU-04.CA-4: GET /api/v1/leave/my-requests filtra por estado")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_CA4_getMyRequests_filterByStatus() throws Exception {
        LeaveRequest pending = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        pending.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(pending);

        LeaveRequest approved = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 15), 4);
        approved.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(approved);

        mockMvc.perform(get("/leave/my-requests")
                        .param("status", "APPROVED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(approved.getId().toString())))
                .andExpect(jsonPath("$.content[0].status", is("APPROVED")))
                .andExpect(jsonPath("$.content[0].canCancel", is(false)));
    }

    @Test
    @DisplayName("HU-04.CA-2: Visualización del motivo de rechazo si status es REJECTED")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_CA2_getMyRequests_rejectionReason() throws Exception {
        LeaveRequest rejected = new LeaveRequest(employee, LeaveType.PERMIT, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), 2);
        rejected.setStatus(LeaveStatus.REJECTED);
        rejected.setRejectionReason("Coincidencia de personal crítico");
        leaveRequestRepository.save(rejected);

        mockMvc.perform(get("/leave/my-requests")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status", is("REJECTED")))
                .andExpect(jsonPath("$.content[0].rejectionReason", is("Coincidencia de personal crítico")))
                .andExpect(jsonPath("$.content[0].canCancel", is(false)));
    }

    @Test
    @DisplayName("HU-04.CA-3: POST /api/v1/leave/requests/{id}/cancel cancela solicitud en PENDING")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_CA3_cancelRequest_success() throws Exception {
        LeaveRequest pending = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        pending.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = leaveRequestRepository.save(pending);

        mockMvc.perform(post("/leave/requests/" + saved.getId() + "/cancel"))
                .andExpect(status().isNoContent());

        assertThat(leaveRequestRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("HU-04: POST /api/v1/leave/requests/{id}/cancel devuelve 409 si ya está aprobada")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_cancelRequest_conflictWhenApproved() throws Exception {
        LeaveRequest approved = new LeaveRequest(employee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        approved.setStatus(LeaveStatus.APPROVED);
        LeaveRequest saved = leaveRequestRepository.save(approved);

        mockMvc.perform(post("/leave/requests/" + saved.getId() + "/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Solicitud no cancelable")));
    }

    @Test
    @DisplayName("HU-04.CA-5: POST /api/v1/leave/requests/{id}/cancel devuelve 403 si pertenece a otro empleado (Art. 10)")
    @WithMockUser(username = "empleado.tracking@empresa.com", roles = "EMPLEADO")
    void test_HU04_CA5_cancelRequest_forbiddenForOtherEmployee() throws Exception {
        LeaveRequest otherReq = new LeaveRequest(anotherEmployee, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), 5);
        otherReq.setStatus(LeaveStatus.PENDING);
        LeaveRequest saved = leaveRequestRepository.save(otherReq);

        mockMvc.perform(post("/leave/requests/" + saved.getId() + "/cancel"))
                .andExpect(status().isForbidden());
    }
}
