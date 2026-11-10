package com.iw.rrhh.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class PerformanceReviewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<UUID, UserIdentity> userIdentities = new HashMap<>();

    @Test
    @DisplayName("HU-08: responsable crea y empleado formaliza una evaluación inmutable")
    void managerCreatesAndEmployeeAcknowledgesReview() throws Exception {
        Employee manager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee member = employee(UserRole.EMPLEADO, manager);
        Employee otherManager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee otherMember = employee(UserRole.EMPLEADO, otherManager);

        String createResponse = mockMvc.perform(post("/performance-reviews/team/{employeeId}", member.getId())
                        .with(as(manager))
                        .contentType("application/json")
                        .content("""
                                {
                                  "period": "2026-ANUAL",
                                  "objectives": "Completar la migración del sistema",
                                  "rating": 4,
                                  "comments": "Buen trabajo en coordinación y calidad"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_ACKNOWLEDGMENT"))
                .andExpect(jsonPath("$.employeeId").value(member.getId().toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID reviewId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        mockMvc.perform(get("/performance-reviews/team").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
        mockMvc.perform(get("/performance-reviews/team").with(as(otherManager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/performance-reviews/me").with(as(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(reviewId.toString()));
        mockMvc.perform(get("/performance-reviews/me").with(as(otherMember)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/performance-reviews/{id}/acknowledge", reviewId)
                        .with(as(otherMember))
                        .contentType("application/json")
                        .content("{\"employeeComments\":\"No es mi evaluación\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/performance-reviews/{id}/acknowledge", reviewId)
                        .with(as(member))
                        .contentType("application/json")
                        .content("{\"employeeComments\":\"De acuerdo con los objetivos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZED"))
                .andExpect(jsonPath("$.employeeComments").value("De acuerdo con los objetivos"))
                .andExpect(jsonPath("$.employeeAcknowledgedAt").isNotEmpty())
                .andExpect(jsonPath("$.finalizedAt").isNotEmpty());

        mockMvc.perform(post("/performance-reviews/{id}/acknowledge", reviewId)
                        .with(as(member))
                        .contentType("application/json")
                        .content("{\"employeeComments\":\"Segundo intento\"}"))
                .andExpect(status().isConflict());
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update("UPDATE performance_reviews SET rating = 2 WHERE id = ?", reviewId));
    }

    @Test
    @DisplayName("HU-08: solo el responsable actual puede crear una evaluación para su equipo")
    void managerCannotCreateReviewForAnotherTeam() throws Exception {
        Employee manager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee otherManager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee member = employee(UserRole.EMPLEADO, otherManager);

        mockMvc.perform(post("/performance-reviews/team/{employeeId}", member.getId())
                        .with(as(manager))
                        .contentType("application/json")
                        .content("""
                                {
                                  "period": "2026-T4",
                                  "objectives": "Objetivos",
                                  "rating": 3,
                                  "comments": "Comentarios"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/performance-reviews/team/employees").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("HU-08: rechaza puntuaciones fuera de la escala 1–5")
    void rejectsRatingOutsideAllowedRange() throws Exception {
        Employee manager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee member = employee(UserRole.EMPLEADO, manager);

        mockMvc.perform(post("/performance-reviews/team/{employeeId}", member.getId())
                        .with(as(manager))
                        .contentType("application/json")
                        .content("""
                                {
                                  "period": "2026-T4",
                                  "objectives": "Objetivos",
                                  "rating": 6,
                                  "comments": "Comentarios"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-08: restringe las operaciones al rol correspondiente")
    void restrictsEndpointsByRole() throws Exception {
        Employee manager = employee(UserRole.RESPONSABLE_EQUIPO, null);
        Employee member = employee(UserRole.EMPLEADO, manager);

        mockMvc.perform(get("/performance-reviews/team").with(as(member)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/performance-reviews/me").with(as(manager)))
                .andExpect(status().isForbidden());
    }

    private Employee employee(UserRole role, Employee manager) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "review-" + suffix + "@empresa.test";
        User user = userRepository.save(new User(username, "not-used", role));
        Employee employee = new Employee();
        employee.setUser(user);
        employee.setFirstName("Test");
        employee.setLastName("Review " + suffix);
        employee.setNationalId("REV-" + suffix);
        employee.setEmail(username);
        employee.setHireDate(LocalDate.of(2024, 1, 1));
        employee.setManager(manager);
        Employee saved = employeeRepository.save(employee);
        userIdentities.put(saved.getId(), new UserIdentity(username, role));
        return saved;
    }

    private RequestPostProcessor as(Employee employee) {
        UserIdentity identity = userIdentities.get(employee.getId());
        return user(identity.username()).roles(identity.role().name());
    }

    private record UserIdentity(String username, UserRole role) {}
}
