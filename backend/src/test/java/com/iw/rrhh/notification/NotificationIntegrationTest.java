package com.iw.rrhh.notification;

import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.notification.domain.Notification;
import com.iw.rrhh.notification.repository.NotificationRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    private static final String OWNER = "notif.propietario@empresa.com";
    private static final String OTHER = "notif.otro@empresa.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private Employee owner;
    private Employee other;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        owner = employee(OWNER, "30000007G");
        other = employee(OTHER, "30000008H");
    }

    private Employee employee(String username, String nationalId) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, "hash", UserRole.EMPLEADO)));
        return employeeRepository.findByEmail(username).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(user);
            e.setFirstName("Notificación");
            e.setLastName("Prueba");
            e.setEmail(username);
            e.setNationalId(nationalId);
            e.setHireDate(LocalDate.of(2025, 1, 1));
            return employeeRepository.save(e);
        });
    }

    private static RequestPostProcessor as(String username) {
        return user(username).roles(UserRole.EMPLEADO.name());
    }

    @Test
    @DisplayName("HU-05.CA-4: el empleado ve solo sus notificaciones, más recientes primero y paginadas")
    void listOwnNotifications() throws Exception {
        notificationRepository.save(new Notification(owner, "Solicitud aprobada", "Primera"));
        Notification latest = notificationRepository.save(new Notification(owner, "Solicitud rechazada", "Segunda"));
        notificationRepository.save(new Notification(other, "Solicitud aprobada", "Ajena"));

        mockMvc.perform(get("/notifications").param("size", "1").with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.content[0].id", is(latest.getId().toString())))
                .andExpect(jsonPath("$.content[0].title", is("Solicitud rechazada")))
                .andExpect(jsonPath("$.content[0].read", is(false)));
    }

    @Test
    @DisplayName("HU-05.CA-4: el contador de no leídas disminuye al marcar una como leída (idempotente)")
    void unreadCountAndMarkAsRead() throws Exception {
        Notification first = notificationRepository.save(new Notification(owner, "Solicitud aprobada", "Primera"));
        notificationRepository.save(new Notification(owner, "Solicitud rechazada", "Segunda"));

        mockMvc.perform(get("/notifications/unread-count").with(as(OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)));

        mockMvc.perform(post("/notifications/{id}/read", first.getId()).with(as(OWNER)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/notifications/{id}/read", first.getId()).with(as(OWNER)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/notifications/unread-count").with(as(OWNER)))
                .andExpect(jsonPath("$.count", is(1)));
        assertThat(notificationRepository.findById(first.getId()).orElseThrow().isRead()).isTrue();
    }

    @Test
    @DisplayName("HU-05.CA-4: marcar como leída una notificación ajena devuelve 403 y no la modifica")
    void markOthersNotification_forbidden() throws Exception {
        Notification foreign = notificationRepository.save(new Notification(other, "Solicitud aprobada", "Ajena"));

        mockMvc.perform(post("/notifications/{id}/read", foreign.getId()).with(as(OWNER)))
                .andExpect(status().isForbidden());

        assertThat(notificationRepository.findById(foreign.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    @DisplayName("HU-05.CA-4: notificación inexistente devuelve 404")
    void unknownNotification_notFound() throws Exception {
        mockMvc.perform(post("/notifications/{id}/read", UUID.randomUUID()).with(as(OWNER)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU-05.CA-4: sin autenticación devuelve 401")
    void unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }
}
