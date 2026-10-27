package com.iw.rrhh.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.CollectiveAgreement;
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
                null, // CEO / Dirección General permite null
                LocalDate.of(2026, 11, 1),
                "Directora General (CEO)",
                "Dirección",
                new BigDecimal("65000.00"),
                defaultAgreement.getId()
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
                defaultAgreement.getId()
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
                defaultAgreement.getId()
        );

        mockMvc.perform(post("/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
