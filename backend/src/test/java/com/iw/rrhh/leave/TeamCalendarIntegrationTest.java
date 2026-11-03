package com.iw.rrhh.leave;

import com.iw.rrhh.TestcontainersConfiguration;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración HU-06 contra PostgreSQL real. La clase NO es @Transactional para que
 * la consulta del calendario se ejecute como en producción.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
class TeamCalendarIntegrationTest {

    private static final String URL = "/leave/team-calendar";
    private static final String MANAGER_A = "calendario.resp.a@empresa.com";
    private static final String MANAGER_B = "calendario.resp.b@empresa.com";
    private static final String MEMBER_A1 = "calendario.miembro.a1@empresa.com";
    private static final String MEMBER_A2 = "calendario.miembro.a2@empresa.com";
    private static final String INACTIVE_A = "calendario.inactivo.a@empresa.com";
    private static final String MEMBER_B = "calendario.miembro.b@empresa.com";

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

    private Employee managerA;
    private Employee managerB;
    private Employee memberA1;
    private Employee memberA2;
    private Employee inactiveA;
    private Employee memberB;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        leaveRequestRepository.deleteAll();

        managerA = employee(MANAGER_A, UserRole.RESPONSABLE_EQUIPO, "Alicia", "Calendario A", "30000009J", null, true);
        managerB = employee(MANAGER_B, UserRole.RESPONSABLE_EQUIPO, "Benito", "Calendario B", "30000010K", null, true);
        memberA1 = employee(MEMBER_A1, UserRole.EMPLEADO, "Diana", "Aguilar", "30000011L", managerA, true);
        memberA2 = employee(MEMBER_A2, UserRole.EMPLEADO, "Ernesto", "Blanco", "30000012M", managerA, true);
        inactiveA = employee(INACTIVE_A, UserRole.EMPLEADO, "Fabiola", "Cano", "30000013N", managerA, false);
        memberB = employee(MEMBER_B, UserRole.EMPLEADO, "Gonzalo", "Duarte", "30000014P", managerB, true);
    }

    private Employee employee(String username, UserRole role, String firstName, String lastName,
                              String nationalId, Employee manager, boolean active) {
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
        employee.setActive(active);
        return employeeRepository.save(employee);
    }

    private LeaveRequest request(Employee owner, LocalDate start, LocalDate end, LeaveStatus status) {
        LeaveRequest request = new LeaveRequest(owner, LeaveType.VACATION, start, end, 1);
        request.setStatus(status);
        if (status == LeaveStatus.REJECTED) {
            request.setRejectionReason("Motivo previo de rechazo");
        }
        return leaveRequestRepository.save(request);
    }

    private static RequestPostProcessor asManagerA() {
        return user(MANAGER_A).roles(UserRole.RESPONSABLE_EQUIPO.name());
    }

    @Test
    @DisplayName("HU-06.CA-1: devuelve los 31 días de diciembre y los miembros activos del equipo")
    void returnsMonthDaysAndMembers() throws Exception {
        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month", is("2026-12")))
                .andExpect(jsonPath("$.days", hasSize(31)))
                .andExpect(jsonPath("$.days[0].date", is("2026-12-01")))
                .andExpect(jsonPath("$.days[30].date", is("2026-12-31")))
                .andExpect(jsonPath("$.members[*].name", contains("Diana Aguilar", "Ernesto Blanco")));
    }

    @Test
    @DisplayName("HU-06.CA-1: el 8 de diciembre se devuelve como festivo con su nombre")
    void holidayHasName() throws Exception {
        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[7].date", is("2026-12-08")))
                .andExpect(jsonPath("$.days[7].dayType", is("HOLIDAY")))
                .andExpect(jsonPath("$.days[7].holidayName", is("Inmaculada Concepción")))
                .andExpect(jsonPath("$.days[4].dayType", is("WEEKEND")))
                .andExpect(jsonPath("$.days[0].dayType", is("WORKING")));
    }

    @Test
    @DisplayName("HU-06.CA-2: muestra aprobadas y pendientes y excluye las rechazadas")
    void approvedAndPendingShownRejectedExcluded() throws Exception {
        LeaveRequest approved = request(memberA1, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1), LeaveStatus.APPROVED);
        LeaveRequest pending = request(memberA2, LocalDate.of(2026, 12, 2), LocalDate.of(2026, 12, 2), LeaveStatus.PENDING);
        LeaveRequest rejected = request(memberA1, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 3), LeaveStatus.REJECTED);

        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].events[0].requestId", is(approved.getId().toString())))
                .andExpect(jsonPath("$.days[0].events[0].status", is("APPROVED")))
                .andExpect(jsonPath("$.days[0].events[0].employeeName", is("Diana Aguilar")))
                .andExpect(jsonPath("$.days[0].events[0].leaveType", is("VACATION")))
                .andExpect(jsonPath("$.days[1].events[0].requestId", is(pending.getId().toString())))
                .andExpect(jsonPath("$.days[1].events[0].status", is("PENDING")))
                .andExpect(jsonPath("$.days[2].events", empty()))
                .andExpect(jsonPath("$.days[*].events[*].requestId", not(hasItem(rejected.getId().toString()))));
    }

    @Test
    @DisplayName("HU-06.CA-3: el día laborable común de dos miembros del equipo es una coincidencia")
    void commonWorkingDayIsConflict() throws Exception {
        request(memberA1, LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 10), LeaveStatus.APPROVED);
        request(memberA2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11), LeaveStatus.PENDING);

        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[9].absentCount", is(2)))
                .andExpect(jsonPath("$.days[9].hasConflict", is(true)))
                .andExpect(jsonPath("$.days[8].absentCount", is(1)))
                .andExpect(jsonPath("$.days[8].hasConflict", is(false)));
    }

    @Test
    @DisplayName("HU-06.CA-4: las ausencias de otro equipo y de miembros inactivos no aparecen ni cuentan")
    void otherTeamAndInactiveExcluded() throws Exception {
        request(memberA1, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 14), LeaveStatus.APPROVED);
        LeaveRequest otherTeam = request(memberB, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 14), LeaveStatus.APPROVED);
        LeaveRequest inactive = request(inactiveA, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 14), LeaveStatus.APPROVED);

        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[13].events", hasSize(1)))
                .andExpect(jsonPath("$.days[13].absentCount", is(1)))
                .andExpect(jsonPath("$.days[13].hasConflict", is(false)))
                .andExpect(jsonPath("$.days[*].events[*].requestId", not(hasItem(otherTeam.getId().toString()))))
                .andExpect(jsonPath("$.days[*].events[*].requestId", not(hasItem(inactive.getId().toString()))))
                .andExpect(jsonPath("$.members[*].id", not(hasItem(memberB.getId().toString()))))
                .andExpect(jsonPath("$.members[*].id", not(hasItem(inactiveA.getId().toString()))));
    }

    @Test
    @DisplayName("HU-06.CA-4: al cambiar de responsable, las ausencias del miembro dejan de aparecer al anterior")
    void managerChangeRemovesMember() throws Exception {
        LeaveRequest moved = request(memberA2, LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 15), LeaveStatus.APPROVED);
        memberA2.setManager(managerB);
        employeeRepository.save(memberA2);

        mockMvc.perform(get(URL).param("month", "2026-12").with(asManagerA()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[*].events[*].requestId", not(hasItem(moved.getId().toString()))))
                .andExpect(jsonPath("$.members[*].id", not(hasItem(memberA2.getId().toString()))));
    }

    @Test
    @DisplayName("HU-06.CA-4: un usuario con rol EMPLEADO recibe 403 ProblemDetail")
    void employeeRoleForbidden() throws Exception {
        mockMvc.perform(get(URL).param("month", "2026-12").with(user(MEMBER_A1).roles(UserRole.EMPLEADO.name())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("HU-06.CA-1: un mes con formato inválido devuelve 400 ProblemDetail")
    void invalidMonthBadRequest() throws Exception {
        mockMvc.perform(get(URL).param("month", "2026-13").with(asManagerA()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("HU-06.CA-1: sin el parámetro month devuelve 400 ProblemDetail")
    void missingMonthBadRequest() throws Exception {
        mockMvc.perform(get(URL).with(asManagerA()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
