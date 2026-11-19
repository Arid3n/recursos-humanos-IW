package com.iw.rrhh.leave;

import com.iw.rrhh.common.exception.InsufficientLeaveBalanceException;
import com.iw.rrhh.common.exception.LeaveRequestOverlapException;
import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveBalance;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.CreateLeaveRequest;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.dto.LeaveRequestResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.LeaveBalanceService;
import com.iw.rrhh.leave.service.LeaveRequestServiceImpl;
import com.iw.rrhh.leave.service.LeaveResolverPolicy;
import com.iw.rrhh.common.exception.LeaveCreationNotAllowedException;
import com.iw.rrhh.leave.dto.LeaveEligibilityResponse;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceService leaveBalanceService;

    @Mock
    private HolidayCalendarService holidayCalendarService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private Employee manager;
    private User user;
    private final UUID employeeId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        leaveRequestService = new LeaveRequestServiceImpl(
                leaveRequestRepository,
                employeeRepository,
                userRepository,
                leaveBalanceService,
                holidayCalendarService,
                clock,
                new LeaveResolverPolicy()
        );

        user = new User("empleado@empresa.com", "hash", UserRole.EMPLEADO);

        manager = new Employee();
        manager.setId(managerId);
        manager.setFirstName("Jefe");
        manager.setLastName("García");
        // Resolutor válido (HU-05 FR-015): activo y con rol de responsable
        manager.setUser(new User("jefe@empresa.com", "hash", UserRole.RESPONSABLE_EQUIPO));

        employee = new Employee();
        employee.setId(employeeId);
        employee.setUser(user);
        employee.setFirstName("Juan");
        employee.setLastName("Pérez");
        employee.setManager(manager);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "token", Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Debe crear solicitud de vacaciones con éxito en estado PENDING asignada al manager")
    void testCreateLeaveRequest_VacationSuccess() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 10)
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(5);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        when(leaveBalanceService.getMyLeaveBalance(2026)).thenReturn(
                new LeaveBalanceResponse(employeeId, 2026, 22, 0, 0, 22, false, "Convenio TIC")
        );
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest lr = invocation.getArgument(0);
            lr.setId(UUID.randomUUID());
            return lr;
        });

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.leaveType()).isEqualTo(LeaveType.VACATION);
        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.calculatedDays()).isEqualTo(5);
        assertThat(response.managerId()).isEqualTo(managerId);
        assertThat(response.employeeId()).isEqualTo(employeeId);
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar solicitud de vacaciones si los días requeridos exceden el saldo disponible")
    void testCreateLeaveRequest_InsufficientBalance_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 17) // 10 días laborables
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(10);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        // Solo 3 días disponibles
        when(leaveBalanceService.getMyLeaveBalance(2026)).thenReturn(
                new LeaveBalanceResponse(employeeId, 2026, 22, 19, 0, 3, false, "Convenio TIC")
        );

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(InsufficientLeaveBalanceException.class)
                .hasMessageContaining("3");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Solicitud de tipo PERMIT no debe consultar ni exigir saldo de vacaciones ordinarias")
    void testCreateLeaveRequest_PermitDoesNotCheckVacationBalance() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.PERMIT,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 7),
                "Permiso de mudanza debidamente documentado"
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(2);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest lr = invocation.getArgument(0);
            lr.setId(UUID.randomUUID());
            return lr;
        });

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        assertThat(response).isNotNull();
        assertThat(response.leaveType()).isEqualTo(LeaveType.PERMIT);
        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        verify(leaveBalanceService, never()).getMyLeaveBalance(anyInt());
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    @DisplayName("Debe rechazar solicitud si las fechas se solapan con otra solicitud en estado PENDING o APPROVED")
    void testCreateLeaveRequest_OverlappingDates_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 10),
                LocalDate.of(2026, 7, 15)
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(4);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(LeaveRequestOverlapException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar solicitud si la fecha de fin es anterior a la fecha de inicio")
    void testCreateLeaveRequest_EndDateBeforeStartDate_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 15),
                LocalDate.of(2026, 7, 10)
        );

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar solicitud si el intervalo no contiene días laborables (ej. fin de semana)")
    void testCreateLeaveRequest_ZeroWorkingDays_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 11), // Sábado
                LocalDate.of(2026, 7, 12)  // Domingo
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(0);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no contiene ningún día laborable");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar solicitud de tipo PERMIT si no se proporciona justificación")
    void testCreateLeaveRequest_PermitRequiresJustification_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.PERMIT,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 7),
                ""
        );

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La justificación es obligatoria para ausencias de tipo permiso");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar solicitud con fecha de inicio anterior a la fecha actual del sistema")
    void testCreateLeaveRequest_PastStartDate_ThrowsException() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 5, 20), // Anterior al reloj (2026-06-01)
                LocalDate.of(2026, 5, 25)
        );

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de inicio debe ser igual o posterior a la fecha actual");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe descontar días en estado PENDING al verificar saldo de vacaciones disponible (FR-004)")
    void testCreateLeaveRequest_DeductsPendingVacationsFromBalance() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                LeaveType.VACATION,
                LocalDate.of(2026, 7, 6),
                LocalDate.of(2026, 7, 10) // 5 días laborables
        );

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(employee));
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(5);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        // Saldo total disponible sin contar pendientes: 8 días
        when(leaveBalanceService.getMyLeaveBalance(2026)).thenReturn(
                new LeaveBalanceResponse(employeeId, 2026, 22, 14, 0, 8, false, "Convenio TIC")
        );
        // Ya tiene 4 días reservados en PENDING: disponible efectivo = 8 - 4 = 4 días
        when(leaveRequestRepository.sumCalculatedDays(eq(employeeId), eq(LeaveType.VACATION), eq(LeaveStatus.PENDING), any(), any()))
                .thenReturn(4);

        // Pide 5 días teniendo solo 4 disponibles efectivos -> debe fallar con InsufficientLeaveBalanceException
        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(request))
                .isInstanceOf(InsufficientLeaveBalanceException.class)
                .hasMessageContaining("4");

        verify(leaveRequestRepository, never()).save(any());
    }

    // ---- HU-05 fase 9 (FR-015, FR-017): resolutor válido ----

    private CreateLeaveRequest validRequest() {
        return new CreateLeaveRequest(LeaveType.VACATION, LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10));
    }

    private void sessionEmployeeIs(Employee e) {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(employeeRepository.findByUserId(user.getId())).thenReturn(Optional.of(e));
    }

    @Test
    @DisplayName("FR-015: sin responsable asignado no se crea la solicitud y se informa «no tienes un responsable asignado»")
    void testCreateLeaveRequest_NoManager_NotAllowed() {
        employee.setManager(null);
        sessionEmployeeIs(employee);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(validRequest()))
                .isInstanceOf(LeaveCreationNotAllowedException.class)
                .hasMessageContaining("no tienes un responsable asignado");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-015: con un responsable dado de baja no se crea la solicitud")
    void testCreateLeaveRequest_InactiveManager_NotAllowed() {
        manager.setActive(false);
        sessionEmployeeIs(employee);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(validRequest()))
                .isInstanceOf(LeaveCreationNotAllowedException.class)
                .hasMessageContaining("no tienes un responsable asignado");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-015: con un responsable sin rol de responsable no se crea la solicitud")
    void testCreateLeaveRequest_ManagerWithoutManagerRole_NotAllowed() {
        manager.getUser().setRole(UserRole.EMPLEADO);
        sessionEmployeeIs(employee);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(validRequest()))
                .isInstanceOf(LeaveCreationNotAllowedException.class)
                .hasMessageContaining("no tienes un responsable asignado");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-017: un usuario DIRECCION_GENERAL no crea solicitudes aunque tenga un responsable válido")
    void testCreateLeaveRequest_DireccionGeneral_NotAllowed() {
        user.setRole(UserRole.DIRECCION_GENERAL);
        sessionEmployeeIs(employee);

        assertThatThrownBy(() -> leaveRequestService.createLeaveRequest(validRequest()))
                .isInstanceOf(LeaveCreationNotAllowedException.class)
                .hasMessageContaining("Dirección General no solicita ausencias");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-015: con un responsable DIRECCION_GENERAL sí se crea la solicitud")
    void testCreateLeaveRequest_ManagerIsDireccionGeneral_Allowed() {
        manager.getUser().setRole(UserRole.DIRECCION_GENERAL);
        CreateLeaveRequest request = validRequest();
        sessionEmployeeIs(employee);
        when(holidayCalendarService.countWorkingDays(request.startDate(), request.endDate())).thenReturn(5);
        when(leaveRequestRepository.hasOverlappingRequests(eq(employeeId), eq(request.startDate()), eq(request.endDate()), any()))
                .thenReturn(false);
        when(leaveBalanceService.getMyLeaveBalance(2026)).thenReturn(
                new LeaveBalanceResponse(employeeId, 2026, 22, 0, 0, 22, false, "Convenio TIC"));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest lr = invocation.getArgument(0);
            lr.setId(UUID.randomUUID());
            return lr;
        });

        LeaveRequestResponse response = leaveRequestService.createLeaveRequest(request);

        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.managerId()).isEqualTo(managerId);
    }

    @Test
    @DisplayName("FR-015: getMyEligibility informa canRequest=false y el motivo cuando no hay resolutor válido")
    void testEligibility_NoResolver() {
        employee.setManager(null);
        sessionEmployeeIs(employee);

        LeaveEligibilityResponse response = leaveRequestService.getMyEligibility();

        assertThat(response.canRequest()).isFalse();
        assertThat(response.reason()).containsIgnoringCase("no tienes un responsable asignado");
    }

    @Test
    @DisplayName("FR-017: getMyEligibility informa el motivo de Dirección General")
    void testEligibility_DireccionGeneral() {
        user.setRole(UserRole.DIRECCION_GENERAL);
        sessionEmployeeIs(employee);

        LeaveEligibilityResponse response = leaveRequestService.getMyEligibility();

        assertThat(response.canRequest()).isFalse();
        assertThat(response.reason()).contains("Dirección General no solicita ausencias");
    }

    @Test
    @DisplayName("FR-015: getMyEligibility informa canRequest=true y sin motivo con un resolutor válido")
    void testEligibility_Allowed() {
        sessionEmployeeIs(employee);

        LeaveEligibilityResponse response = leaveRequestService.getMyEligibility();

        assertThat(response.canRequest()).isTrue();
        assertThat(response.reason()).isNull();
    }
}
