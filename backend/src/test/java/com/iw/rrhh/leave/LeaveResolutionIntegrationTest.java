package com.iw.rrhh.leave;

import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
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
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración HU-05 contra PostgreSQL real. La clase NO es @Transactional para que
 * los bloqueos FOR UPDATE y el trigger de V3 actúen como en producción.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class LeaveResolutionIntegrationTest {

    private static final String MANAGER_A = "resp.equipo.a@empresa.com";
    private static final String MANAGER_B = "resp.equipo.b@empresa.com";
    private static final String MEMBER_A = "miembro.equipo.a@empresa.com";
    private static final String MEMBER_B = "miembro.equipo.b@empresa.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Employee managerA;
    private Employee managerB;
    private Employee memberA;
    private Employee memberB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        leaveRequestRepository.deleteAll();

        managerA = employee(MANAGER_A, UserRole.RESPONSABLE_EQUIPO, "Ana", "Responsable A", "30000001A", null);
        managerB = employee(MANAGER_B, UserRole.RESPONSABLE_EQUIPO, "Bruno", "Responsable B", "30000002B", null);
        memberA = employee(MEMBER_A, UserRole.EMPLEADO, "Marta", "Miembro A", "30000003C", managerA);
        memberB = employee(MEMBER_B, UserRole.EMPLEADO, "Nicolás", "Miembro B", "30000004D", managerB);
    }

    private Employee employee(String username, UserRole role, String firstName, String lastName,
                              String nationalId, Employee manager) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new User(username, "hash", role)));
        Employee employee = employeeRepository.findByEmail(username).orElseGet(() -> {
            Employee e = new Employee();
            e.setUser(user);
            e.setFirstName(firstName);
            e.setLastName(lastName);
            e.setEmail(username);
            e.setNationalId(nationalId);
            e.setHireDate(LocalDate.of(2025, 1, 1));
            return e;
        });
        employee.setManager(manager);
        return employeeRepository.save(employee);
    }

    private LeaveRequest request(Employee owner, LeaveType type, LocalDate start, LocalDate end, int days, LeaveStatus status) {
        LeaveRequest request = new LeaveRequest(owner, type, start, end, days);
        request.setStatus(status);
        if (status == LeaveStatus.REJECTED) {
            request.setRejectionReason("Motivo previo de rechazo");
        }
        return leaveRequestRepository.save(request);
    }

    private LeaveRequest pendingVacation(Employee owner, LocalDate start, int days) {
        return request(owner, LeaveType.VACATION, start, start.plusDays(days - 1L), days, LeaveStatus.PENDING);
    }

    private static RequestPostProcessor as(String username, UserRole role) {
        return user(username).roles(role.name());
    }

    private static RequestPostProcessor asManagerA() {
        return as(MANAGER_A, UserRole.RESPONSABLE_EQUIPO);
    }

    private List<Notification> notificationsOf(Employee employee) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipient().getId().equals(employee.getId()))
                .toList();
    }

    @Test
    @DisplayName("HU-05.CA-1: la bandeja muestra solo las solicitudes PENDING del equipo, de la más antigua a la más reciente")
    void teamRequests_onlyPendingOfOwnTeam() throws Exception {
        LeaveRequest first = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 2);
        LeaveRequest second = request(memberA, LeaveType.PERMIT, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10), 1, LeaveStatus.PENDING);
        request(memberA, LeaveType.VACATION, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17), 2, LeaveStatus.APPROVED);
        request(memberA, LeaveType.VACATION, LocalDate.of(2026, 11, 18), LocalDate.of(2026, 11, 19), 2, LeaveStatus.REJECTED);
        pendingVacation(memberB, LocalDate.of(2026, 12, 1), 2);

        mockMvc.perform(get("/leave/team-requests").with(asManagerA()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content[0].id", is(first.getId().toString())))
                .andExpect(jsonPath("$.content[0].employeeName", is("Marta Miembro A")))
                .andExpect(jsonPath("$.content[0].calculatedDays", is(2)))
                .andExpect(jsonPath("$.content[1].id", is(second.getId().toString())));
    }

    @Test
    @DisplayName("HU-05.CA-1: el rol EMPLEADO no puede acceder a la bandeja ni resolver (403)")
    void employeeRole_forbidden() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 2);

        mockMvc.perform(get("/leave/team-requests").with(as(MEMBER_A, UserRole.EMPLEADO)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(as(MEMBER_A, UserRole.EMPLEADO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("HU-05.CA-2 / HU-05.CA-4: aprobar devuelve APPROVED y persiste la notificación con el comentario")
    void approve_success() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 3);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Buen descanso\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.reason", is("Buen descanso")))
                .andExpect(jsonPath("$.resolvedById", is(managerA.getId().toString())))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        LeaveRequest stored = leaveRequestRepository.findById(pending.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(stored.getResolvedAt()).isNotNull();

        List<Notification> notifications = notificationsOf(memberA);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTitle()).isEqualTo("Solicitud aprobada");
        assertThat(notifications.get(0).getMessage()).contains("01/12/2026").contains("Comentario: Buen descanso");
        assertThat(notifications.get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("HU-05.CA-2: aprobar sin cuerpo es válido (comentario opcional)")
    void approve_withoutBody() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.reason", nullValue()));
    }

    @Test
    @DisplayName("HU-05.CA-2 / HU-05.CA-4: rechazar con motivo devuelve REJECTED, no altera el saldo y notifica el motivo")
    void reject_success() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 3);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Coincide con la entrega del proyecto\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")))
                .andExpect(jsonPath("$.reason", is("Coincide con la entrega del proyecto")));

        mockMvc.perform(get("/leave/balances/employee/{id}", memberA.getId()).param("year", "2026").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentDays", is(0)));

        List<Notification> notifications = notificationsOf(memberA);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTitle()).isEqualTo("Solicitud rechazada");
        assertThat(notifications.get(0).getMessage()).contains("Motivo: Coincide con la entrega del proyecto");
    }

    @Test
    @DisplayName("HU-05.CA-2: rechazar sin motivo o con menos de 5 caracteres devuelve 400 con error en el campo")
    void reject_invalidReason() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.reason", notNullValue()));

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.reason", notNullValue()));

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(notificationsOf(memberA)).isEmpty();
    }

    @Test
    @DisplayName("HU-05.CA-3: una solicitud ya resuelta devuelve 409 al intentar resolverla de nuevo")
    void resolveTwice_conflict() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title", is("Solicitud ya resuelta")));
        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Cambio de opinión\"}"))
                .andExpect(status().isConflict());

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(notificationsOf(memberA)).hasSize(1);
    }

    @Test
    @DisplayName("HU-05.CA-3: el trigger de BD impide modificar el estado de una solicitud resuelta")
    void databaseTrigger_blocksUpdateOfResolvedRequest() {
        LeaveRequest approved = request(memberA, LeaveType.VACATION, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1), 1, LeaveStatus.APPROVED);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE leave_requests SET status = 'PENDING' WHERE id = ?", approved.getId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("HU-05.CA-1: un responsable de otro equipo recibe 403 y la solicitud no cambia")
    void otherTeamManager_forbidden() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Prueba de alcance\"}"))
                .andExpect(status().isForbidden());

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(notificationsOf(memberA)).isEmpty();
    }

    @Test
    @DisplayName("HU-05.CA-1: tras cambiar de responsable, solo el nuevo puede resolver")
    void managerChange_onlyCurrentManagerResolves() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);
        memberA.setManager(managerB);
        employeeRepository.save(memberA);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-05.CA-3: una solicitud inexistente devuelve 404")
    void unknownRequest_notFound() throws Exception {
        mockMvc.perform(post("/leave/requests/{id}/approve", UUID.randomUUID()).with(asManagerA()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HU-05.CA-5: una vacación aprobada pasa a contar como disfrutada en el saldo derivado (HU-02)")
    void approve_updatesDerivedBalance() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 4);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/leave/balances/employee/{id}", memberA.getId()).param("year", "2026").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spentDays", is(4)))
                .andExpect(jsonPath("$.availableDays", is(18)));
    }

    @Test
    @DisplayName("HU-05.CA-5: aprobar vacaciones que superan el saldo devuelve 422 con año y días, y la solicitud sigue PENDING")
    void approve_insufficientBalance() throws Exception {
        request(memberA, LeaveType.VACATION, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 31), 20, LeaveStatus.APPROVED);
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 5);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.year", is(2026)))
                .andExpect(jsonPath("$.requestedDays", is(5)))
                .andExpect(jsonPath("$.availableDays", is(2)));

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(notificationsOf(memberA)).isEmpty();
    }

    @Test
    @DisplayName("HU-05.CA-5: dos aprobaciones simultáneas que superan el saldo dejan solo una aprobada (Art. 12.6)")
    void concurrentApprovals_neverExceedBalance() throws Exception {
        LeaveRequest first = pendingVacation(memberA, LocalDate.of(2026, 6, 1), 15);
        LeaveRequest second = pendingVacation(memberA, LocalDate.of(2026, 9, 1), 15);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> approveFirst = () -> {
                start.await();
                return mockMvc.perform(post("/leave/requests/{id}/approve", first.getId()).with(asManagerA()))
                        .andReturn().getResponse().getStatus();
            };
            Callable<Integer> approveSecond = () -> {
                start.await();
                return mockMvc.perform(post("/leave/requests/{id}/approve", second.getId()).with(asManagerA()))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> f1 = executor.submit(approveFirst);
            Future<Integer> f2 = executor.submit(approveSecond);
            start.countDown();

            assertThat(List.of(f1.get(), f2.get())).containsExactlyInAnyOrder(200, 422);
        } finally {
            executor.shutdownNow();
        }

        long approved = leaveRequestRepository.findAll().stream()
                .filter(r -> r.getStatus() == LeaveStatus.APPROVED)
                .count();
        assertThat(approved).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-05.CA-3: una solicitud retirada por el empleado (HU-04, CANCELLED) devuelve 404; fuera de alcance sigue siendo 403")
    void cancelledByEmployee_notFound() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 2);

        mockMvc.perform(post("/leave/requests/{id}/cancel", pending.getId()).with(as(MEMBER_A, UserRole.EMPLEADO)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Motivo suficiente\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO)))
                .andExpect(status().isForbidden());

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.CANCELLED);
        assertThat(notificationsOf(memberA)).isEmpty();
    }

    @Test
    @DisplayName("HU-05.CA-5: aprobar con fechas en un año cerrado devuelve 422 y la solicitud sigue PENDING; rechazar sí se permite")
    void closedYear_approveBlocked_rejectAllowed() throws Exception {
        LeaveRequest pending = request(memberA, LeaveType.VACATION,
                LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 5), 5, LeaveStatus.PENDING);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.year", is(2025)));
        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Fuera de plazo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));
    }

    @Test
    @DisplayName("HU-05.CA-5: vacaciones que cruzan años con saldo en ambos años se aprueban y cuentan en los dos saldos")
    void crossYear_bothYearsSufficient_approvedAndCountedInBothBalances() throws Exception {
        // 28/12/2026–08/01/2027: 4 laborables en cada año (01/01 y 06/01 son festivos)
        LeaveRequest pending = request(memberA, LeaveType.VACATION,
                LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 8, LeaveStatus.PENDING);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        mockMvc.perform(get("/leave/balances/employee/{id}", memberA.getId()).param("year", "2026").with(asManagerA()))
                .andExpect(jsonPath("$.spentDays", is(4)));
        mockMvc.perform(get("/leave/balances/employee/{id}", memberA.getId()).param("year", "2027").with(asManagerA()))
                .andExpect(jsonPath("$.spentDays", is(4)));
    }

    @Test
    @DisplayName("HU-05.CA-5: vacaciones que cruzan años sin saldo en el año siguiente devuelven 422 con ese año")
    void crossYear_secondYearInsufficient_namesTheYear() throws Exception {
        request(memberA, LeaveType.VACATION, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31), 20, LeaveStatus.APPROVED);
        LeaveRequest pending = request(memberA, LeaveType.VACATION,
                LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 8, LeaveStatus.PENDING);

        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.year", is(2027)))
                .andExpect(jsonPath("$.requestedDays", is(4)))
                .andExpect(jsonPath("$.availableDays", is(2)));

        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(notificationsOf(memberA)).isEmpty();
    }

    @Test
    @DisplayName("HU-05.CA-1: un responsable no resuelve sus propias solicitudes (403); las resuelve su superior, que las ve en su bandeja")
    void managerOwnRequest_resolvedBySuperiorOnly() throws Exception {
        managerA.setManager(managerB);
        employeeRepository.save(managerA);
        LeaveRequest own = pendingVacation(managerA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/approve", own.getId()).with(asManagerA()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/leave/requests/{id}/reject", own.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Auto rechazo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/leave/team-requests").with(asManagerA()))
                .andExpect(jsonPath("$.totalElements", is(0)));

        mockMvc.perform(get("/leave/team-requests").with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(own.getId().toString())));
        mockMvc.perform(post("/leave/requests/{id}/approve", own.getId()).with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-05.CA-1: un responsable sin superior, o con superior sin rol RESPONSABLE_EQUIPO, queda sin resolutor (límite conocido)")
    void managerWithoutValidSuperior_nobodyResolves() throws Exception {
        LeaveRequest ownWithoutSuperior = pendingVacation(managerA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/approve", ownWithoutSuperior.getId()).with(asManagerA()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/leave/requests/{id}/approve", ownWithoutSuperior.getId()).with(as(MANAGER_B, UserRole.RESPONSABLE_EQUIPO)))
                .andExpect(status().isForbidden());

        managerA.setManager(memberB);
        employeeRepository.save(managerA);
        mockMvc.perform(post("/leave/requests/{id}/approve", ownWithoutSuperior.getId()).with(as(MEMBER_B, UserRole.EMPLEADO)))
                .andExpect(status().isForbidden());

        assertThat(leaveRequestRepository.findById(ownWithoutSuperior.getId()).orElseThrow().getStatus())
                .isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    @DisplayName("HU-05.CA-3: dos resoluciones simultáneas de la MISMA solicitud dejan un 200 y un 409 (Art. 22.4)")
    void concurrentResolutionsOfSameRequest_oneWins() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 2);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> approve = () -> {
                start.await();
                return mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId()).with(asManagerA()))
                        .andReturn().getResponse().getStatus();
            };
            Callable<Integer> reject = () -> {
                start.await();
                return mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                                .with(asManagerA())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Resolución simultánea\"}"))
                        .andReturn().getResponse().getStatus();
            };
            Future<Integer> f1 = executor.submit(approve);
            Future<Integer> f2 = executor.submit(reject);
            start.countDown();

            assertThat(List.of(f1.get(), f2.get())).containsExactlyInAnyOrder(200, 409);
        } finally {
            executor.shutdownNow();
        }

        assertThat(notificationsOf(memberA)).hasSize(1);
    }

    @Test
    @DisplayName("HU-05.CA-2: motivo o comentario de 501 caracteres devuelve 400 con error en el campo; 500 se acepta")
    void textLimits_501Rejected_500Accepted() throws Exception {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.reason", notNullValue()));
        mockMvc.perform(post("/leave/requests/{id}/approve", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidFields.comment", notNullValue()));
        assertThat(leaveRequestRepository.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(LeaveStatus.PENDING);

        mockMvc.perform(post("/leave/requests/{id}/reject", pending.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + "a".repeat(500) + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("HU-05.CA-2: la BD rechaza un motivo de más de 500 caracteres (chk_leave_rejection_reason_max, Art. 8.2)")
    void databaseCheck_blocksReasonOver500() {
        LeaveRequest pending = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE leave_requests SET rejection_reason = ? WHERE id = ?", "x".repeat(501), pending.getId()))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("HU-05.CA-5: la ausencia aprobada aparece en el calendario del equipo y la rechazada no (FR-012)")
    void approvedRequest_appearsInTeamCalendar() throws Exception {
        LeaveRequest toApprove = pendingVacation(memberA, LocalDate.of(2026, 12, 1), 1);
        LeaveRequest toReject = pendingVacation(memberA, LocalDate.of(2026, 12, 10), 1);

        mockMvc.perform(post("/leave/requests/{id}/approve", toApprove.getId()).with(asManagerA()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/leave/requests/{id}/reject", toReject.getId())
                        .with(asManagerA())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"No es posible esa semana\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/leave/team-calendar").param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[?(@.date=='2026-12-01')].events[0].status", contains("APPROVED")))
                .andExpect(jsonPath("$.days[?(@.date=='2026-12-01')].events[0].employeeName", contains("Marta Miembro A")))
                .andExpect(jsonPath("$.days[?(@.date=='2026-12-10')].absentCount", contains(0)));
    }
}
