package com.iw.rrhh.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.notification.repository.NotificationRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import java.util.List;
import com.iw.rrhh.employee.dto.CreateContractVariationRequest;
import com.iw.rrhh.employee.dto.CreateEmployeeRequest;
import com.iw.rrhh.employee.repository.CollectiveAgreementRepository;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
public class EmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollectiveAgreementRepository collectiveAgreementRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ContractRecordRepository contractRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private CollectiveAgreement defaultAgreement;

    @BeforeEach
    void setUp() {
        if (collectiveAgreementRepository.count() == 0) {
            defaultAgreement = collectiveAgreementRepository.save(
                    new CollectiveAgreement("GENERAL", "Convenio Colectivo General", 22)
            );
        } else {
            defaultAgreement = collectiveAgreementRepository.findAll().get(0);
        }
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("Alta de empleado exitosa con generación de credencial provisional y primer contrato")
    void testCreateEmployee_Success() throws Exception {
        String uniqueDni = "12345678" + (char) ('A' + (int)(Math.random() * 26));
        String uniqueEmail = "maria." + UUID.randomUUID() + "@empresa.com";

        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "María",
                "García López",
                "12345678Z",
                uniqueEmail,
                null, // Dirección General: sin responsable (withoutManager = true)
                LocalDate.of(2026, 11, 1),
                "Directora General (CEO)",
                "Dirección",
                new BigDecimal("65000.00"),
                defaultAgreement.getId(),
                true
        );

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email", is(uniqueEmail)))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.contractRecordId").isNotEmpty());

        assertTrue(employeeRepository.existsByEmail(uniqueEmail));
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("Alta con campos obligatorios vacíos devuelve 400 Bad Request según RFC 9457")
    void testCreateEmployee_ValidationErrors() throws Exception {
        CreateEmployeeRequest invalidRequest = new CreateEmployeeRequest(
                "", // Nombre vacío
                "", // Apellidos vacíos
                "DNI_INVALIDO", // NIF inválido
                "email_invalido", // Email inválido
                null,
                null, // Fecha nula
                "",
                "",
                new BigDecimal("-500"), // Salario negativo
                null,
                null
        );

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://api.rrhh.iw/errors/validation-failed")))
                .andExpect(jsonPath("$.invalidFields.firstName").exists())
                .andExpect(jsonPath("$.invalidFields.salary").exists());
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("Añadir variación contractual append-only con fecha retroactiva sin alterar contratos previos")
    void testAddContractVariation_AppendOnly_Retroactive() throws Exception {
        String email = "carlos." + UUID.randomUUID() + "@empresa.com";
        CreateEmployeeRequest alta = new CreateEmployeeRequest(
                "Carlos",
                "Sánchez",
                "87654321A",
                email,
                null,
                LocalDate.of(2026, 1, 15),
                "CEO",
                "Dirección",
                new BigDecimal("50000.00"),
                defaultAgreement.getId(),
                true
        );

        String responseStr = mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alta)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID employeeId = UUID.fromString(objectMapper.readTree(responseStr).get("id").asText());

        // Variación retroactiva al 1 de marzo de 2026
        CreateContractVariationRequest variation = new CreateContractVariationRequest(
                LocalDate.of(2026, 3, 1),
                "CEO & Presidente",
                "Dirección",
                new BigDecimal("58000.00"),
                defaultAgreement.getId(),
                null
        );

        mockMvc.perform(post("/employees/" + employeeId + "/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(variation)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position", is("CEO & Presidente")))
                .andExpect(jsonPath("$.salary", is(58000.00)));

        // Verificar que el historial tiene 2 registros inmutables
        mockMvc.perform(get("/employees/" + employeeId + "/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].position", is("CEO")))
                .andExpect(jsonPath("$[1].position", is("CEO & Presidente")));
    }

    @Test
    @WithMockUser(username = "empleado_user", roles = {"EMPLEADO"})
    @DisplayName("Autorización negativa: Rol EMPLEADO recibe 403 Forbidden al intentar dar de alta")
    void testCreateEmployee_ForbiddenForNonHR() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "Hacker",
                "Test",
                "11112222B",
                "hacker@test.com",
                null,
                LocalDate.now(),
                "CEO",
                "IT",
                new BigDecimal("30000.00"),
                defaultAgreement.getId(),
                true
        );

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- HU-05 fase 9 (FR-017): alta sin responsable explícita => DIRECCION_GENERAL ----

    private CreateEmployeeRequest hire(String email, String nationalId, UUID managerId, String position, Boolean withoutManager) {
        return new CreateEmployeeRequest(
                "Persona", "De Pruebas", nationalId, email, managerId,
                LocalDate.of(2026, 11, 1), position, "Tecnología",
                new BigDecimal("30000.00"), defaultAgreement.getId(), withoutManager);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID() + "@empresa.com";
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-017: sin managerId y sin withoutManager el alta devuelve 400, también con puesto de CEO (no se mira el puesto)")
    void hire_withoutManagerAndWithoutFlag_returns400() throws Exception {
        for (String position : List.of("Analista", "CEO")) {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    hire(uniqueEmail("sinflag"), "40000001A", null, position, null))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-017: con withoutManager=true se crea el usuario con rol DIRECCION_GENERAL, sea cual sea el puesto")
    void hire_withoutManagerFlag_createsDireccionGeneral() throws Exception {
        String email = uniqueEmail("dg");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hire(email, "40000002B", null, "Analista", true))))
                .andExpect(status().isCreated());

        assertEquals(UserRole.DIRECCION_GENERAL, userRepository.findByUsername(email).orElseThrow().getRole());
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-017: managerId y withoutManager=true a la vez devuelve 400")
    void hire_managerAndFlag_returns400() throws Exception {
        Employee manager = savedEmployee(uniqueEmail("jefe"), UserRole.RESPONSABLE_EQUIPO, "40000003C", null);

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                hire(uniqueEmail("ambos"), "40000004D", manager.getId(), "Analista", true))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-017: con managerId el alta crea el usuario con rol EMPLEADO")
    void hire_withManager_createsEmployeeRole() throws Exception {
        Employee manager = savedEmployee(uniqueEmail("jefe"), UserRole.RESPONSABLE_EQUIPO, "40000005E", null);
        String email = uniqueEmail("emp");

        mockMvc.perform(post("/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                hire(email, "40000006F", manager.getId(), "Analista", false))))
                .andExpect(status().isCreated());

        assertEquals(UserRole.EMPLEADO, userRepository.findByUsername(email).orElseThrow().getRole());
    }

    // ---- HU-05 fase 9 (FR-016): la baja del superior cancela las pendientes de sus subordinados directos ----

    private Employee savedEmployee(String email, UserRole role, String nationalId, Employee manager) {
        User user = userRepository.save(new User(email, "hash", role));
        Employee e = new Employee();
        e.setUser(user);
        e.setFirstName("Nombre");
        e.setLastName("Apellido");
        e.setEmail(email);
        e.setNationalId(nationalId);
        e.setHireDate(LocalDate.of(2025, 1, 1));
        e.setManager(manager);
        return employeeRepository.save(e);
    }

    private LeaveRequest savedRequest(Employee owner, int day, LeaveStatus status) {
        LeaveRequest request = new LeaveRequest(owner, LeaveType.VACATION,
                LocalDate.of(2026, 12, day), LocalDate.of(2026, 12, day), 1);
        request.setStatus(status);
        return leaveRequestRepository.save(request);
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-016: dar de baja a un responsable cancela las PENDING de sus subordinados directos y notifica una vez por empleado")
    void deactivateManager_cancelsPendingOfDirectReports() throws Exception {
        Employee manager = savedEmployee(uniqueEmail("resp"), UserRole.RESPONSABLE_EQUIPO, "40000010A", null);
        Employee other = savedEmployee(uniqueEmail("otro"), UserRole.RESPONSABLE_EQUIPO, "40000011B", null);
        Employee report1 = savedEmployee(uniqueEmail("sub1"), UserRole.EMPLEADO, "40000012C", manager);
        Employee report2 = savedEmployee(uniqueEmail("sub2"), UserRole.EMPLEADO, "40000013D", manager);
        Employee stranger = savedEmployee(uniqueEmail("ajeno"), UserRole.EMPLEADO, "40000014E", other);

        LeaveRequest r1a = savedRequest(report1, 1, LeaveStatus.PENDING);
        LeaveRequest r1b = savedRequest(report1, 8, LeaveStatus.PENDING);
        LeaveRequest r1approved = savedRequest(report1, 15, LeaveStatus.APPROVED);
        LeaveRequest r2 = savedRequest(report2, 2, LeaveStatus.PENDING);
        LeaveRequest ofStranger = savedRequest(stranger, 3, LeaveStatus.PENDING);

        mockMvc.perform(delete("/employees/" + manager.getId()))
                .andExpect(status().isNoContent());

        assertEquals(LeaveStatus.CANCELLED, leaveRequestRepository.findById(r1a.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.CANCELLED, leaveRequestRepository.findById(r1b.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.CANCELLED, leaveRequestRepository.findById(r2.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.APPROVED, leaveRequestRepository.findById(r1approved.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.PENDING, leaveRequestRepository.findById(ofStranger.getId()).orElseThrow().getStatus());
        // No se borra ninguna fila (rastro de auditoría)
        assertTrue(leaveRequestRepository.existsById(r1a.getId()));

        assertEquals(1, notificationsOf(report1));
        assertEquals(1, notificationsOf(report2));
        assertEquals(0, notificationsOf(stranger));
        assertEquals(0, notificationsOf(manager));
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-015 / FR-016: tras la baja del responsable el subordinado ya no puede crear solicitudes (eligibility)")
    void afterDeactivation_reportCannotRequest() throws Exception {
        Employee manager = savedEmployee(uniqueEmail("resp"), UserRole.RESPONSABLE_EQUIPO, "40000015F", null);
        Employee report = savedEmployee(uniqueEmail("sub"), UserRole.EMPLEADO, "40000016G", manager);

        mockMvc.perform(delete("/employees/" + manager.getId())).andExpect(status().isNoContent());

        mockMvc.perform(get("/leave/requests/eligibility")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(report.getEmail()).roles("EMPLEADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRequest", is(false)));
    }

    private long notificationsOf(Employee employee) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(employee.getId()))
                .count();
    }

    // ---- HU-05 fase 10 (FR-018): un superior con rol RRHH también cuenta como resolutor ----

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-018 / FR-016: dar de baja a un superior RRHH cancela las PENDING de sus subordinados directos y notifica una vez por empleado")
    void deactivateHrManager_cancelsPendingOfDirectReports() throws Exception {
        Employee hr = savedEmployee(uniqueEmail("rrhhsup"), UserRole.RRHH, "40000020A", null);
        Employee report = savedEmployee(uniqueEmail("subrrhh"), UserRole.RESPONSABLE_EQUIPO, "40000021B", hr);
        Employee stranger = savedEmployee(uniqueEmail("ajenorrhh"), UserRole.EMPLEADO, "40000022C", null);

        LeaveRequest pendingA = savedRequest(report, 1, LeaveStatus.PENDING);
        LeaveRequest pendingB = savedRequest(report, 8, LeaveStatus.PENDING);
        LeaveRequest ofStranger = savedRequest(stranger, 3, LeaveStatus.PENDING);

        mockMvc.perform(delete("/employees/" + hr.getId()))
                .andExpect(status().isNoContent());

        assertEquals(LeaveStatus.CANCELLED, leaveRequestRepository.findById(pendingA.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.CANCELLED, leaveRequestRepository.findById(pendingB.getId()).orElseThrow().getStatus());
        assertEquals(LeaveStatus.PENDING, leaveRequestRepository.findById(ofStranger.getId()).orElseThrow().getStatus());
        assertEquals(1, notificationsOf(report));
        assertEquals(0, notificationsOf(stranger));
    }

    @Test
    @WithMockUser(username = "rrhh_user", roles = {"RRHH"})
    @DisplayName("FR-018 / FR-016: dar de baja a un empleado cuyo superior RRHH sigue activo no afecta a las solicitudes de otros")
    void deactivateEmployeeWithActiveHrSuperior_doesNotAffectOthers() throws Exception {
        Employee hr = savedEmployee(uniqueEmail("rrhhact"), UserRole.RRHH, "40000023D", null);
        Employee leaving = savedEmployee(uniqueEmail("sale"), UserRole.RESPONSABLE_EQUIPO, "40000024E", hr);
        Employee sibling = savedEmployee(uniqueEmail("hermano"), UserRole.RESPONSABLE_EQUIPO, "40000025F", hr);
        LeaveRequest ofSibling = savedRequest(sibling, 2, LeaveStatus.PENDING);

        mockMvc.perform(delete("/employees/" + leaving.getId()))
                .andExpect(status().isNoContent());

        assertEquals(LeaveStatus.PENDING, leaveRequestRepository.findById(ofSibling.getId()).orElseThrow().getStatus());
        assertEquals(0, notificationsOf(sibling));
    }
}
