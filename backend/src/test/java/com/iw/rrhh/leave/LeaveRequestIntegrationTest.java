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
import com.iw.rrhh.leave.dto.CreateLeaveRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class LeaveRequestIntegrationTest {

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

    private User userEmployee;
    private Employee employee;
    private Employee manager;

    @BeforeEach
    void setUp() {
        leaveRequestRepository.deleteAll();
        contractRecordRepository.deleteAll();
        employeeRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Crear usuario y empleado manager
        User userManager = new User("manager.req@empresa.com", "$2a$10$dummyHash", UserRole.RESPONSABLE_EQUIPO);
        userManager = userRepository.save(userManager);

        manager = new Employee();
        manager.setUser(userManager);
        manager.setFirstName("Responsable");
        manager.setLastName("Equipo");
        manager.setNationalId("99887766M");
        manager.setEmail("manager.req@empresa.com");
        manager.setHireDate(LocalDate.of(2020, 1, 1));
        manager = employeeRepository.save(manager);

        // 2. Crear usuario y empleado solicitante
        userEmployee = new User("empleado.req@empresa.com", "$2a$10$dummyHash", UserRole.EMPLEADO);
        userEmployee = userRepository.save(userEmployee);

        employee = new Employee();
        employee.setUser(userEmployee);
        employee.setFirstName("Solicitante");
        employee.setLastName("Pruebas");
        employee.setNationalId("11223344S");
        employee.setEmail("empleado.req@empresa.com");
        employee.setHireDate(LocalDate.of(2022, 1, 1));
        employee.setManager(manager);
        employee = employeeRepository.save(employee);

        // 3. Convenio y contrato con 22 días de vacaciones
        CollectiveAgreement agreement = collectiveAgreementRepository.findByCode("TIC")
                .orElseGet(() -> {
                    CollectiveAgreement ca = new CollectiveAgreement("TIC", "Convenio TIC", 22);
                    return collectiveAgreementRepository.save(ca);
                });

        ContractRecord contract = new ContractRecord(
                employee,
                LocalDate.of(2022, 1, 1),
                "Desarrollador",
                "Tecnología",
                new BigDecimal("35000.00"),
                agreement,
                manager,
                "SYSTEM"
        );
        contractRecordRepository.save(contract);
    }

    @Test
    @DisplayName("GET /leave/requests/calculate-days - Debe calcular correctamente los días laborables descontando fin de semana")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCalculateWorkingDays_Success() throws Exception {
        // Del lunes 6 de julio de 2026 al viernes 10 de julio de 2026 (5 días laborables)
        mockMvc.perform(get("/leave/requests/calculate-days")
                        .param("startDate", "2026-07-06")
                        .param("endDate", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-07-06"))
                .andExpect(jsonPath("$.endDate").value("2026-07-10"))
                .andExpect(jsonPath("$.workingDays").value(5))
                .andExpect(jsonPath("$.calendarDays").value(5))
                .andExpect(jsonPath("$.weekendDays").value(0))
                .andExpect(jsonPath("$.holidayDays").value(0));
    }

    @Test
    @DisplayName("POST /leave/requests - Debe registrar solicitud de vacaciones en estado PENDING asignada al manager")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_VacationSuccess() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 10)
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.employeeId").value(employee.getId().toString()))
                .andExpect(jsonPath("$.managerId").value(manager.getId().toString()))
                .andExpect(jsonPath("$.leaveType").value("VACATION"))
                .andExpect(jsonPath("$.calculatedDays").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startDate").value("2026-07-06"))
                .andExpect(jsonPath("$.endDate").value("2026-07-10"));
    }

    @Test
    @DisplayName("POST /leave/requests - Debe rechazar con 409 si los días solicitados exceden el saldo disponible")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_InsufficientBalance_Rejected() throws Exception {
        // Gastar 20 días de vacaciones previamente con una solicitud aprobada
        LeaveRequest existingApproved = new LeaveRequest(
                employee,
                LeaveType.VACATION,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 1, 30),
                20
        );
        existingApproved.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(existingApproved);

        // Intenta pedir 5 días de vacaciones teniendo solo 2 disponibles (22 - 20 = 2)
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 10) // 5 días laborables
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", containsString("Saldo insuficiente")));
    }

    @Test
    @DisplayName("POST /leave/requests - Tipo PERMIT no descuenta ni se bloquea por saldo de vacaciones")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_PermitSuccessEvenWithLowBalance() throws Exception {
        // Gastar todo el saldo de vacaciones
        LeaveRequest existingApproved = new LeaveRequest(
                employee,
                LeaveType.VACATION,
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 2, 3),
                22
        );
        existingApproved.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(existingApproved);

        // Solicitar un permiso (PERMIT) de 3 días
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.PERMIT,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 8)
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveType").value("PERMIT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.calculatedDays").value(3));
    }

    @Test
    @DisplayName("POST /leave/requests - Debe rechazar con 409 si se solapa con una solicitud PENDING o APPROVED")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_Overlap_Rejected() throws Exception {
        // Solicitud previa del 10 al 15 de julio en estado PENDING
        LeaveRequest existingPending = new LeaveRequest(
                employee,
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 15),
                4
        );
        existingPending.setStatus(LeaveStatus.PENDING);
        leaveRequestRepository.save(existingPending);

        // Intenta solicitar del 13 al 17 de julio (se solapa parcialmente)
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 13),
                LocalDate.of(2026, 7, 17)
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", containsString("Solapamiento")));
    }

    @Test
    @DisplayName("POST /leave/requests - Debe rechazar con 400 si endDate es anterior a startDate")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_InvalidDateRange_Rejected() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 10)
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
