package com.iw.rrhh.leave.service;

import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.CalendarDayType;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.dto.TeamCalendarDayResponse;
import com.iw.rrhh.leave.dto.TeamCalendarEventResponse;
import com.iw.rrhh.leave.dto.TeamCalendarResponse;
import com.iw.rrhh.leave.dto.TeamMemberResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class TeamCalendarServiceImpl implements TeamCalendarService {

    private static final List<LeaveStatus> VISIBLE_STATUSES = List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private static final int CONFLICT_THRESHOLD = 2;

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final HolidayCalendarService holidayCalendarService;

    public TeamCalendarServiceImpl(
            LeaveRequestRepository leaveRequestRepository,
            EmployeeRepository employeeRepository,
            UserRepository userRepository,
            HolidayCalendarService holidayCalendarService
    ) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.holidayCalendarService = holidayCalendarService;
    }

    @Override
    public TeamCalendarResponse getTeamCalendar(YearMonth month) {
        Employee manager = getAuthenticatedEmployee();
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        List<TeamMemberResponse> members = employeeRepository.findByManagerIdAndActiveTrue(manager.getId()).stream()
                .sorted(Comparator.comparing(Employee::getLastName).thenComparing(Employee::getFirstName))
                .map(e -> new TeamMemberResponse(e.getId(), fullName(e)))
                .toList();

        // Consulta acotada al mes visible y al equipo del responsable en sesión (Art. 10.5 y 16.4)
        List<LeaveRequest> requests = leaveRequestRepository.findTeamCalendarRequests(
                manager.getId(), VISIBLE_STATUSES, firstDay, lastDay);

        List<TeamCalendarDayResponse> days = new ArrayList<>();
        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            days.add(buildDay(date, requests));
        }
        return new TeamCalendarResponse(month, members, days);
    }

    /**
     * Clasifica el día y, solo si es laborable, calcula sus ausencias y la coincidencia (Art. 16.3).
     */
    private TeamCalendarDayResponse buildDay(LocalDate date, List<LeaveRequest> requests) {
        String holidayName = holidayCalendarService.getHolidayName(date).orElse(null);
        CalendarDayType dayType;
        if (holidayCalendarService.isWeekend(date)) {
            dayType = CalendarDayType.WEEKEND;
        } else if (holidayName != null) {
            dayType = CalendarDayType.HOLIDAY;
        } else {
            dayType = CalendarDayType.WORKING;
        }

        if (dayType != CalendarDayType.WORKING) {
            return new TeamCalendarDayResponse(date, dayType, holidayName, 0, false, List.of());
        }

        List<TeamCalendarEventResponse> events = requests.stream()
                .filter(r -> !date.isBefore(r.getStartDate()) && !date.isAfter(r.getEndDate()))
                .map(this::toEvent)
                .toList();
        int absentCount = (int) events.stream().map(TeamCalendarEventResponse::employeeId).distinct().count();

        return new TeamCalendarDayResponse(date, dayType, null, absentCount, absentCount >= CONFLICT_THRESHOLD, events);
    }

    private TeamCalendarEventResponse toEvent(LeaveRequest request) {
        Employee employee = request.getEmployee();
        return new TeamCalendarEventResponse(
                request.getId(),
                employee.getId(),
                fullName(employee),
                request.getLeaveType(),
                request.getStatus(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    private String fullName(Employee employee) {
        return employee.getFirstName() + " " + employee.getLastName();
    }

    private Employee getAuthenticatedEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new NoSuchElementException("Usuario de sesión no encontrado"));

        return employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NoSuchElementException("No se encontró ficha de empleado para el usuario autenticado"));
    }
}
