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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class LeaveBalanceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private User userEmployeeA;
    private User userEmployeeB;
    private User userManager;
    private Employee empA;
    private Employee empB;
    private Employee managerEmp;
    private CollectiveAgreement agreement22;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        contractRecordRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        agreement22 = collectiveAgreementRepository.findByCode("AGR-22")
                .orElseGet(() -> collectiveAgreementRepository.save(
                        new CollectiveAgreement("AGR-22", "Convenio 22 Dias", 22)
                ));

        // Usuario y Empleado Manager
        userManager = userRepository.save(new User("manager1", "$2a$10$xyz", UserRole.RESPONSABLE_EQUIPO));
        managerEmp = new Employee();
        managerEmp.setUser(userManager);
        managerEmp.setFirstName("Carlos");
        managerEmp.setLastName("Manager");
        managerEmp.setEmail("manager@empresa.com");
        managerEmp.setNationalId("11111111M");
        managerEmp.setHireDate(LocalDate.of(2020, 1, 1));
        managerEmp = employeeRepository.save(managerEmp);

        contractRecordRepository.save(new ContractRecord(
                managerEmp,
                LocalDate.of(2020, 1, 1),
                "Responsable de Equipo",
                "Ingeniería",
                new BigDecimal("50000"),
                agreement22,
                null,
                "RRHH"
        ));

        // Empleado A (supervisado por managerEmp)
        userEmployeeA = userRepository.save(new User("empleadoA", "$2a$10$xyz", UserRole.EMPLEADO));
        empA = new Employee();
        empA.setUser(userEmployeeA);
        empA.setFirstName("Ana");
        empA.setLastName("García");
        empA.setEmail("ana@empresa.com");
        empA.setNationalId("22222222A");
        empA.setHireDate(LocalDate.of(2022, 1, 1));
        empA.setManager(managerEmp);
        empA = employeeRepository.save(empA);

        contractRecordRepository.save(new ContractRecord(
                empA,
                LocalDate.of(2022, 1, 1),
                "Desarrolladora",
                "Ingeniería",
                new BigDecimal("32000"),
                agreement22,
                managerEmp,
                "RRHH"
        ));

        // Empleado B (otro empleado)
        userEmployeeB = userRepository.save(new User("empleadoB", "$2a$10$xyz", UserRole.EMPLEADO));
        empB = new Employee();
        empB.setUser(userEmployeeB);
        empB.setFirstName("Bernardo");
        empB.setLastName("López");
        empB.setEmail("bernardo@empresa.com");
        empB.setNationalId("33333333B");
        empB.setHireDate(LocalDate.of(2022, 1, 1));
        empB = employeeRepository.save(empB);

        contractRecordRepository.save(new ContractRecord(
                empB,
                LocalDate.of(2022, 1, 1),
                "Analista",
                "Finanzas",
                new BigDecimal("31000"),
                agreement22,
                null,
                "RRHH"
        ));
    }

    @Test
    @WithMockUser(username = "empleadoA", roles = {"EMPLEADO"})
    @DisplayName("US1: Empleado consulta su propia ficha en /employees/me")
    void testGetMyProfile_Success() throws Exception {
        mockMvc.perform(get("/employees/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(empA.getId().toString())))
                .andExpect(jsonPath("$.firstName", is("Ana")))
                .andExpect(jsonPath("$.lastName", is("García")))
                .andExpect(jsonPath("$.email", is("ana@empresa.com")))
                .andExpect(jsonPath("$.currentPosition", is("Desarrolladora")))
                .andExpect(jsonPath("$.currentDepartment", is("Ingeniería")))
                .andExpect(jsonPath("$.managerFullName", is("Carlos Manager")));
    }

    @Test
    @WithMockUser(username = "empleadoA", roles = {"EMPLEADO"})
    @DisplayName("US1: Empleado consulta su propio saldo en /leave/balances/me")
    void testGetMyLeaveBalance_Success() throws Exception {
        // Crear una solicitud aprobada de 5 días y una pendiente de 3 días
        LeaveRequest reqApproved = new LeaveRequest(empA, LeaveType.VACATION, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), 5);
        reqApproved.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(reqApproved);

        LeaveRequest reqPending = new LeaveRequest(empA, LeaveType.VACATION, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), 3);
        reqPending.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(reqPending);

        mockMvc.perform(get("/leave/balances/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is(empA.getId().toString())))
                .andExpect(jsonPath("$.entitledDays", is(22)))
                .andExpect(jsonPath("$.spentDays", is(5)))
                .andExpect(jsonPath("$.pendingDays", is(3)))
                .andExpect(jsonPath("$.availableDays", is(17)))
                .andExpect(jsonPath("$.isClosedYear", is(false)));
    }

    @Test
    @WithMockUser(username = "empleadoA", roles = {"EMPLEADO"})
    @DisplayName("US3: Empleado A intenta consultar el saldo de Empleado B -> 403 Forbidden")
    void testForbiddenAccessToOtherEmployeeBalance_NegativeTest() throws Exception {
        mockMvc.perform(get("/leave/balances/employee/" + empB.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title", notNullValue()));
    }

    @Test
    @WithMockUser(username = "manager1", roles = {"RESPONSABLE_EQUIPO"})
    @DisplayName("US3: Responsable de equipo puede consultar el saldo de su supervisado (Empleado A)")
    void testManagerCanConsultManagedEmployeeBalance() throws Exception {
        mockMvc.perform(get("/leave/balances/employee/" + empA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is(empA.getId().toString())))
                .andExpect(jsonPath("$.entitledDays", is(22)));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"RRHH"})
    @DisplayName("US3: Rol RRHH puede consultar el saldo de cualquier empleado")
    void testHRCanConsultAnyEmployeeBalance() throws Exception {
        mockMvc.perform(get("/leave/balances/employee/" + empB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId", is(empB.getId().toString())));
    }

    @Test
    @WithMockUser(username = "empleadoA", roles = {"EMPLEADO"})
    @DisplayName("US4: Consulta histórica de año cerrado devuelve isClosedYear = true")
    void testHistoricalBalance_ClosedYear() throws Exception {
        int pastYear = LocalDate.now().getYear() - 1;
        mockMvc.perform(get("/leave/balances/me?year=" + pastYear))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(pastYear)))
                .andExpect(jsonPath("$.isClosedYear", is(true)));
    }
}

