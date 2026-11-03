package com.iw.rrhh.leave;

import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.CalendarDayType;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.TeamCalendarDayResponse;
import com.iw.rrhh.leave.dto.TeamCalendarEventResponse;
import com.iw.rrhh.leave.dto.TeamCalendarResponse;
import com.iw.rrhh.leave.dto.TeamMemberResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.TeamCalendarServiceImpl;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamCalendarServiceTest {

    private static final YearMonth DECEMBER = YearMonth.of(2026, 12);

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    private TeamCalendarServiceImpl service;

    private Employee manager;
    private Employee laura;
    private Employee pedro;

    @BeforeEach
    void setUp() {
        service = new TeamCalendarServiceImpl(
                leaveRequestRepository,
                employeeRepository,
                userRepository,
                new HolidayCalendarService()
        );

        User managerUser = new User("responsable@empresa.com", "hash", UserRole.RESPONSABLE_EQUIPO);
        manager = employee("Carlos", "Ruiz", null);
        manager.setUser(managerUser);
        laura = employee("Laura", "Martínez", manager);
        pedro = employee("Pedro", "Gómez", manager);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(managerUser.getUsername(), "token", Collections.emptyList())
        );
        lenient().when(userRepository.findByUsername(managerUser.getUsername())).thenReturn(Optional.of(managerUser));
        lenient().when(employeeRepository.findByUserId(managerUser.getId())).thenReturn(Optional.of(manager));
        lenient().when(employeeRepository.findByManagerIdAndActiveTrue(manager.getId())).thenReturn(List.of(laura, pedro));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Employee employee(String firstName, String lastName, Employee managerOf) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setManager(managerOf);
        return employee;
    }

    private LeaveRequest request(Employee employee, LeaveStatus status, LocalDate start, LocalDate end) {
        LeaveRequest request = new LeaveRequest(employee, LeaveType.VACATION, start, end, 1);
        request.setId(UUID.randomUUID());
        request.setStatus(status);
        return request;
    }

    private void givenRequests(LeaveRequest... requests) {
        when(leaveRequestRepository.findTeamCalendarRequests(any(), any(), any(), any())).thenReturn(List.of(requests));
    }

    private TeamCalendarDayResponse day(TeamCalendarResponse calendar, int dayOfMonth) {
        return calendar.days().get(dayOfMonth - 1);
    }

    @Test
    @DisplayName("HU-06.CA-1: devuelve un día por cada día del mes, en orden, y los miembros ordenados por apellidos")
    void returnsEveryDayOfMonthInOrder() {
        givenRequests();

        TeamCalendarResponse calendar = service.getTeamCalendar(DECEMBER);

        assertThat(calendar.month()).isEqualTo(DECEMBER);
        assertThat(calendar.days()).hasSize(31);
        for (int i = 0; i < 31; i++) {
            assertThat(calendar.days().get(i).date()).isEqualTo(LocalDate.of(2026, 12, i + 1));
        }
        assertThat(calendar.members()).extracting(TeamMemberResponse::name)
                .containsExactly("Pedro Gómez", "Laura Martínez");
    }

    @Test
    @DisplayName("HU-06.CA-1 / HU-06.CA-3: los eventos solo ocupan días laborables y fines de semana y festivos no cuentan")
    void eventsOnlyOnWorkingDays() {
        // Jueves 3 a miércoles 9 de diciembre: 5-6 fin de semana, 8 festivo
        LeaveRequest approved = request(laura, LeaveStatus.APPROVED, LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 9));
        givenRequests(approved);

        TeamCalendarResponse calendar = service.getTeamCalendar(DECEMBER);

        assertThat(day(calendar, 2).events()).isEmpty();
        assertThat(day(calendar, 3).events()).hasSize(1);
        assertThat(day(calendar, 4).events()).hasSize(1);
        assertThat(day(calendar, 7).events()).hasSize(1);
        assertThat(day(calendar, 9).events()).hasSize(1);
        assertThat(day(calendar, 10).events()).isEmpty();

        TeamCalendarDayResponse saturday = day(calendar, 5);
        assertThat(saturday.dayType()).isEqualTo(CalendarDayType.WEEKEND);
        assertThat(saturday.events()).isEmpty();
        assertThat(saturday.absentCount()).isZero();

        TeamCalendarDayResponse holiday = day(calendar, 8);
        assertThat(holiday.dayType()).isEqualTo(CalendarDayType.HOLIDAY);
        assertThat(holiday.holidayName()).isEqualTo("Inmaculada Concepción");
        assertThat(holiday.events()).isEmpty();
        assertThat(holiday.absentCount()).isZero();

        // Domingo 6: fin de semana con el nombre del festivo
        TeamCalendarDayResponse sundayHoliday = day(calendar, 6);
        assertThat(sundayHoliday.dayType()).isEqualTo(CalendarDayType.WEEKEND);
        assertThat(sundayHoliday.holidayName()).isEqualTo("Día de la Constitución");

        assertThat(day(calendar, 7).dayType()).isEqualTo(CalendarDayType.WORKING);
        assertThat(day(calendar, 7).holidayName()).isNull();
    }

    @Test
    @DisplayName("HU-06.CA-1: una ausencia que cruza el cambio de mes aparece en sus días del mes con el rango completo")
    void requestCrossingMonthBoundary() {
        LeaveRequest crossing = request(laura, LeaveStatus.PENDING, LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 4));
        givenRequests(crossing);

        TeamCalendarResponse calendar = service.getTeamCalendar(DECEMBER);

        assertThat(day(calendar, 29).events()).isEmpty();
        TeamCalendarEventResponse event = day(calendar, 31).events().getFirst();
        assertThat(day(calendar, 30).events()).hasSize(1);
        assertThat(event.requestId()).isEqualTo(crossing.getId());
        assertThat(event.employeeId()).isEqualTo(laura.getId());
        assertThat(event.employeeName()).isEqualTo("Laura Martínez");
        assertThat(event.leaveType()).isEqualTo(LeaveType.VACATION);
        assertThat(event.startDate()).isEqualTo(LocalDate.of(2026, 12, 30));
        assertThat(event.endDate()).isEqualTo(LocalDate.of(2027, 1, 4));
    }

    @Test
    @DisplayName("HU-06.CA-3: dos empleados distintos el mismo día laborable generan coincidencia")
    void twoEmployeesSameDayIsConflict() {
        givenRequests(
                request(laura, LeaveStatus.APPROVED, LocalDate.of(2026, 12, 9), LocalDate.of(2026, 12, 10)),
                request(pedro, LeaveStatus.PENDING, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 11))
        );

        TeamCalendarResponse calendar = service.getTeamCalendar(DECEMBER);

        TeamCalendarDayResponse common = day(calendar, 10);
        assertThat(common.absentCount()).isEqualTo(2);
        assertThat(common.hasConflict()).isTrue();
        assertThat(common.events()).hasSize(2);

        TeamCalendarDayResponse single = day(calendar, 9);
        assertThat(single.absentCount()).isEqualTo(1);
        assertThat(single.hasConflict()).isFalse();
    }

    @Test
    @DisplayName("HU-06.CA-3: un mismo empleado con dos solicitudes el mismo día cuenta una sola vez")
    void sameEmployeeTwiceCountsOnce() {
        givenRequests(
                request(laura, LeaveStatus.APPROVED, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 14)),
                request(laura, LeaveStatus.PENDING, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 15))
        );

        TeamCalendarDayResponse monday = day(service.getTeamCalendar(DECEMBER), 14);

        assertThat(monday.events()).hasSize(2);
        assertThat(monday.absentCount()).isEqualTo(1);
        assertThat(monday.hasConflict()).isFalse();
    }

    @Test
    @DisplayName("HU-06.CA-2: los eventos conservan el estado aprobada o pendiente")
    void eventsKeepStatus() {
        givenRequests(
                request(laura, LeaveStatus.APPROVED, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1)),
                request(pedro, LeaveStatus.PENDING, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1))
        );

        TeamCalendarDayResponse tuesday = day(service.getTeamCalendar(DECEMBER), 1);

        assertThat(tuesday.events()).extracting(TeamCalendarEventResponse::status)
                .containsExactly(LeaveStatus.APPROVED, LeaveStatus.PENDING);
    }

    @Test
    @DisplayName("HU-06.CA-4: consulta solo el equipo del responsable en sesión, estados pendiente y aprobada y el mes visible")
    void queriesRepositoryWithSessionManagerAndMonth() {
        givenRequests();

        service.getTeamCalendar(DECEMBER);

        verify(leaveRequestRepository).findTeamCalendarRequests(
                manager.getId(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 31)
        );
        verify(employeeRepository).findByManagerIdAndActiveTrue(manager.getId());
    }
}
