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
                LocalDate.of(2026, 7, 8),
                "Permiso debidamente acreditado"
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaveType").value("PERMIT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.justification").value("Permiso debidamente acreditado"))
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

    @Test
    @DisplayName("POST /leave/requests - Debe rechazar con 400 si el tipo es PERMIT y no se incluye justificación")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_PermitWithoutJustification_Rejected() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.PERMIT,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 8),
                ""
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("La justificación es obligatoria para ausencias de tipo permiso")));
    }

    @Test
    @DisplayName("POST /leave/requests - Debe rechazar con 400 si la fecha de inicio es anterior a la fecha actual")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_PastDate_Rejected() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 5, 10), // Anterior al reloj de test (2026-06-01)
                LocalDate.of(2026, 5, 15)
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("La fecha de inicio debe ser igual o posterior a la fecha actual")));
    }

    @Test
    @DisplayName("POST /leave/requests - Permite justificación opcional en solicitudes de vacaciones y la retorna")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_VacationWithOptionalJustification() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 10),
                "Vacaciones de verano en familia"
        );

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.justification").value("Vacaciones de verano en familia"));
    }

    // ---- HU-05 fase 9 (FR-015, FR-017): resolutor válido y elegibilidad ----

    private CreateLeaveRequest julyRequest() {
        return new CreateLeaveRequest(LeaveType.VACATION, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10));
    }

    private Employee direccionGeneralWithManager(Employee superior) {
        User dgUser = userRepository.save(new User("direccion.req@empresa.com", "$2a$10$dummyHash", UserRole.DIRECCION_GENERAL));
        Employee dg = new Employee();
        dg.setUser(dgUser);
        dg.setFirstName("Dirección");
        dg.setLastName("General");
        dg.setNationalId("55667788D");
        dg.setEmail("direccion.req@empresa.com");
        dg.setHireDate(LocalDate.of(2019, 1, 1));
        dg.setManager(superior);
        return employeeRepository.save(dg);
    }

    @Test
    @DisplayName("POST /leave/requests - Sin responsable asignado responde 422 con «no tienes un responsable asignado» y no persiste (FR-015)")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_NoManager_Returns422() throws Exception {
        employee.setManager(null);
        employeeRepository.save(employee);

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(julyRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("no tienes un responsable asignado")));

        org.junit.jupiter.api.Assertions.assertEquals(0, leaveRequestRepository.count());
    }

    @Test
    @DisplayName("POST /leave/requests - Con el responsable dado de baja responde 422 (FR-015)")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_InactiveManager_Returns422() throws Exception {
        manager.setActive(false);
        employeeRepository.save(manager);

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(julyRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail", containsString("no tienes un responsable asignado")));
    }

    @Test
    @DisplayName("POST /leave/requests - Un usuario DIRECCION_GENERAL responde 422 aunque tenga responsable válido (FR-017)")
    @WithMockUser(username = "direccion.req@empresa.com", roles = "DIRECCION_GENERAL")
    void testCreateLeaveRequest_DireccionGeneral_Returns422() throws Exception {
        direccionGeneralWithManager(manager);

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(julyRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail", containsString("Dirección General no solicita ausencias")));
    }

    @Test
    @DisplayName("GET /leave/requests/eligibility - Empleado con resolutor válido: canRequest=true sin motivo")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testEligibility_Allowed() throws Exception {
        mockMvc.perform(get("/leave/requests/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequest").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }

    @Test
    @DisplayName("GET /leave/requests/eligibility - Sin responsable: canRequest=false con el motivo")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testEligibility_NoManager() throws Exception {
        employee.setManager(null);
        employeeRepository.save(employee);

        mockMvc.perform(get("/leave/requests/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequest").value(false))
                .andExpect(jsonPath("$.reason", containsStringIgnoringCase("no tienes un responsable asignado")));
    }

    @Test
    @DisplayName("GET /leave/requests/eligibility - Dirección General: canRequest=false con su motivo")
    @WithMockUser(username = "direccion.req@empresa.com", roles = "DIRECCION_GENERAL")
    void testEligibility_DireccionGeneral() throws Exception {
        direccionGeneralWithManager(manager);

        mockMvc.perform(get("/leave/requests/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequest").value(false))
                .andExpect(jsonPath("$.reason", containsString("Dirección General no solicita ausencias")));
    }

    @Test
    @DisplayName("GET /leave/requests/eligibility - Sin sesión responde 401")
    void testEligibility_Unauthenticated() throws Exception {
        mockMvc.perform(get("/leave/requests/eligibility"))
                .andExpect(status().isUnauthorized());
    }

    // ---- HU-05 fase 10 (FR-018): un superior con rol RRHH es resolutor válido ----

    @Test
    @DisplayName("POST /leave/requests - Con un responsable RRHH se crea la solicitud (201) (FR-018)")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testCreateLeaveRequest_ManagerIsHr_Returns201() throws Exception {
        User managerUser = userRepository.findByUsername("manager.req@empresa.com").orElseThrow();
        managerUser.setRole(UserRole.RRHH);
        userRepository.save(managerUser);

        mockMvc.perform(post("/leave/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(julyRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /leave/requests/eligibility - Con un responsable RRHH: canRequest=true (FR-018)")
    @WithMockUser(username = "empleado.req@empresa.com", roles = "EMPLEADO")
    void testEligibility_ManagerIsHr_Allowed() throws Exception {
        User managerUser = userRepository.findByUsername("manager.req@empresa.com").orElseThrow();
        managerUser.setRole(UserRole.RRHH);
        userRepository.save(managerUser);

        mockMvc.perform(get("/leave/requests/eligibility"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequest").value(true))
                .andExpect(jsonPath("$.reason").doesNotExist());
    }
}
