package com.iw.rrhh.leave;

import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.service.LeaveDayAllocator;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.leave.dto.LeaveBalanceResponse;
import com.iw.rrhh.leave.repository.LeaveRequestRepository;
import com.iw.rrhh.leave.service.LeaveBalanceServiceImpl;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LeaveBalanceServiceTest {

    private EmployeeRepository employeeRepository;
    private UserRepository userRepository;
    private ContractRecordRepository contractRecordRepository;
    private LeaveRequestRepository leaveRequestRepository;
    private LeaveBalanceServiceImpl leaveBalanceService;

    private User testUser;
    private Employee testEmployee;
    private CollectiveAgreement agreement;
    private ContractRecord currentContract;

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);

    private final List<LeaveRequest> approvedRequests = new ArrayList<>();
    private final List<LeaveRequest> pendingRequests = new ArrayList<>();

    @BeforeEach
    void setUp() {
        testUser = new User("empleado1", "hash", UserRole.EMPLEADO);
        testUser.setId(UUID.randomUUID());

        testEmployee = new Employee();
        testEmployee.setId(UUID.randomUUID());
        testEmployee.setUser(testUser);
        testEmployee.setFirstName("Juan");
        testEmployee.setLastName("Pérez");
        testEmployee.setEmail("juan.perez@empresa.com");
        testEmployee.setNationalId("12345678Z");
        testEmployee.setHireDate(LocalDate.of(2023, 1, 15));

        agreement = new CollectiveAgreement("CONV-TIC", "Convenio TIC", 22);
        agreement.setId(1L);

        currentContract = new ContractRecord(
                testEmployee,
                LocalDate.of(2023, 1, 15),
                "Desarrollador",
                "IT",
                new BigDecimal("30000"),
                agreement,
                null,
                "RRHH"
        );

        userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByUsername")) {
                        return Optional.of(testUser);
                    }
                    return null;
                }
        );

        employeeRepository = (EmployeeRepository) Proxy.newProxyInstance(
                EmployeeRepository.class.getClassLoader(),
                new Class<?>[]{EmployeeRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByUserId")) {
                        return Optional.of(testEmployee);
                    }
                    if (method.getName().equals("findById")) {
                        return Optional.of(testEmployee);
                    }
                    return null;
                }
        );

        contractRecordRepository = (ContractRecordRepository) Proxy.newProxyInstance(
                ContractRecordRepository.class.getClassLoader(),
                new Class<?>[]{ContractRecordRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findCurrentContract")) {
                        return Optional.ofNullable(currentContract);
                    }
                    return null;
                }
        );

        leaveRequestRepository = (LeaveRequestRepository) Proxy.newProxyInstance(
                LeaveRequestRepository.class.getClassLoader(),
                new Class<?>[]{LeaveRequestRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findOverlappingByYear")) {
                        LeaveStatus status = (LeaveStatus) args[2];
                        LocalDate startOfYear = (LocalDate) args[3];
                        LocalDate endOfYear = (LocalDate) args[4];
                        List<LeaveRequest> source = status == LeaveStatus.APPROVED ? approvedRequests
                                : status == LeaveStatus.PENDING ? pendingRequests : List.of();
                        return source.stream()
                                .filter(r -> !r.getStartDate().isAfter(endOfYear) && !r.getEndDate().isBefore(startOfYear))
                                .toList();
                    }
                    return null;
                }
        );

        leaveBalanceService = new LeaveBalanceServiceImpl(
                employeeRepository,
                userRepository,
                contractRecordRepository,
                leaveRequestRepository,
                new LeaveDayAllocator(new HolidayCalendarService()),
                FIXED_CLOCK.withZone(ZoneId.of("Europe/Madrid"))
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLEADO"))
        );
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    private static LeaveRequest containedRequest(int days) {
        return containedRequest(days, LocalDate.now(FIXED_CLOCK).getYear());
    }

    /** Solicitud íntegramente contenida en {@code year}, con {@code days} días computados. */
    private static LeaveRequest containedRequest(int days, int year) {
        return new LeaveRequest(null, LeaveType.VACATION, LocalDate.of(year, 3, 2), LocalDate.of(year, 3, 20), days);
    }

    private static LeaveRequest crossYearRequest() {
        // 28/12/2026–08/01/2027: 4 laborables en 2026 y 4 en 2027 (01/01 y 06/01 festivos)
        return new LeaveRequest(null, LeaveType.VACATION, LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 8), 8);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU-02.CA-1: Consulta de saldo propio de año en curso con desglose exacto")
    void testGetMyLeaveBalance_FullYear() {
        int currentYear = LocalDate.now(FIXED_CLOCK).getYear();
        approvedRequests.add(containedRequest(7));
        pendingRequests.add(containedRequest(3));

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        assertNotNull(balance);
        assertEquals(currentYear, balance.year());
        assertEquals(22, balance.entitledDays());
        assertEquals(7, balance.spentDays());
        assertEquals(3, balance.pendingDays());
        assertEquals(15, balance.availableDays(), "Disponibles debe ser 22 - 7 = 15");
        assertEquals("Convenio TIC", balance.agreementName());
        assertFalse(balance.isClosedYear());
    }

    @Test
    @DisplayName("HU-02.CA-2: Recálculo automático con variación de convenio a 24 días")
    void testGetMyLeaveBalance_AgreementChangeTo24Days() {
        CollectiveAgreement updatedAgreement = new CollectiveAgreement("CONV-TIC-NEW", "Nuevo Convenio TIC", 24);
        currentContract = new ContractRecord(
                testEmployee,
                LocalDate.now(FIXED_CLOCK),
                "Senior Dev",
                "IT",
                new BigDecimal("35000"),
                updatedAgreement,
                null,
                "RRHH"
        );
        approvedRequests.add(containedRequest(4));

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        assertEquals(24, balance.entitledDays());
        assertEquals(4, balance.spentDays());
        assertEquals(20, balance.availableDays(), "Disponibles debe ser 24 - 4 = 20");
    }

    @Test
    @DisplayName("HU-02: Prorrateo para incorporación a mitad de año (1 de julio)")
    void testGetMyLeaveBalance_MidYearProrate() {
        int currentYear = LocalDate.now(FIXED_CLOCK).getYear();
        testEmployee.setHireDate(LocalDate.of(currentYear, 7, 1)); // 6 meses restantes

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        // round((22 / 12) * 6) = 11
        assertEquals(11, balance.entitledDays());
        assertEquals(11, balance.availableDays());
    }

    @Test
    @DisplayName("HU-02: Saldo agotado nunca es negativo")
    void testGetMyLeaveBalance_ZeroFloorWhenExhausted() {
        approvedRequests.add(containedRequest(25)); // más que los correspondientes por convenio

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        assertEquals(22, balance.entitledDays());
        assertEquals(25, balance.spentDays());
        assertEquals(0, balance.availableDays(), "No se permiten saldos negativos");
    }

    @Test
    @DisplayName("HU-02.CA-4: Consulta histórica de ejercicio fiscal anterior (solo lectura)")
    void testGetHistoricalLeaveBalance_ClosedYear() {
        int pastYear = LocalDate.now(FIXED_CLOCK).getYear() - 1;
        approvedRequests.add(containedRequest(20, pastYear));

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(pastYear);

        assertEquals(pastYear, balance.year());
        assertTrue(balance.isClosedYear(), "Debe marcarse como ejercicio cerrado");
        assertEquals(22, balance.entitledDays());
        assertEquals(20, balance.spentDays());
        assertEquals(2, balance.availableDays());
    }

    @Test
    @DisplayName("HU-05.CA-5: una vacación APPROVED que cruza años cuenta su parte como disfrutada en AMBOS años")
    void testCrossYearApprovedCountsInBothYears() {
        approvedRequests.add(crossYearRequest());

        assertEquals(4, leaveBalanceService.getMyLeaveBalance(2026).spentDays());
        assertEquals(4, leaveBalanceService.getMyLeaveBalance(2027).spentDays());
        assertEquals(18, leaveBalanceService.getMyLeaveBalance(2026).availableDays());
        assertEquals(18, leaveBalanceService.getMyLeaveBalance(2027).availableDays());
    }

    @Test
    @DisplayName("HU-05.CA-5: una vacación PENDING que cruza años cuenta su parte como pendiente en ambos años")
    void testCrossYearPendingCountsInBothYears() {
        pendingRequests.add(crossYearRequest());

        assertEquals(4, leaveBalanceService.getMyLeaveBalance(2026).pendingDays());
        assertEquals(4, leaveBalanceService.getMyLeaveBalance(2027).pendingDays());
        assertEquals(0, leaveBalanceService.getMyLeaveBalance(2026).spentDays());
    }

    @Test
    @DisplayName("HU-05.CA-5: una solicitud contenida en un año da el mismo resultado que antes (días computados íntegros)")
    void testContainedRequestUnchanged() {
        approvedRequests.add(containedRequest(9, 2026));

        assertEquals(9, leaveBalanceService.getMyLeaveBalance(2026).spentDays());
        assertEquals(0, leaveBalanceService.getMyLeaveBalance(2027).spentDays());
    }

    @Test
    @DisplayName("HU-05.CA-5: un año futuro usa el saldo normal del convenio vigente")
    void testFutureYearUsesNormalBalance() {
        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(2027);

        assertEquals(22, balance.entitledDays());
        assertEquals(22, balance.availableDays());
        assertFalse(balance.isClosedYear());
    }

    @Test
    @DisplayName("HU-05.CA-5: un año futuro posterior a la incorporación del empleado no tiene días correspondientes")
    void testFutureYearBeforeHireHasNoEntitledDays() {
        testEmployee.setHireDate(LocalDate.of(2028, 3, 1));

        assertEquals(0, leaveBalanceService.getMyLeaveBalance(2027).entitledDays());
    }

    @Test
    @DisplayName("Art. 22.5 / 14.2: el año en curso sale del Clock inyectado en Europe/Madrid, no de la fecha real")
    void testCurrentYearComesFromInjectedClockInBusinessZone() {
        // 31/12/2026 23:30 UTC ya es 01/01/2027 en Madrid
        Clock newYearEveUtc = Clock.fixed(Instant.parse("2026-12-31T23:30:00Z"), ZoneOffset.UTC);
        LeaveBalanceServiceImpl service = new LeaveBalanceServiceImpl(
                employeeRepository, userRepository, contractRecordRepository, leaveRequestRepository,
                new LeaveDayAllocator(new HolidayCalendarService()),
                newYearEveUtc.withZone(ZoneId.of("Europe/Madrid")));

        assertEquals(2027, service.getMyLeaveBalance(null).year());
        assertTrue(service.getMyLeaveBalance(2026).isClosedYear());
    }

    // ---- HU-05 fase 10 (FR-017 / spec-002 FR-008): Dirección General no tiene saldo propio ----

    @Test
    @DisplayName("FR-017 / HU-02 FR-008: Dirección General recibe AccessDeniedException al consultar su saldo propio")
    void direccionGeneral_ownBalance_isDenied() {
        testUser.setRole(UserRole.DIRECCION_GENERAL);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> leaveBalanceService.getMyLeaveBalance(null));
    }

    @Test
    @DisplayName("FR-017 / HU-02 FR-008: Dirección General recibe AccessDeniedException al consultar su saldo por su propio id")
    void direccionGeneral_ownBalanceById_isDenied() {
        testUser.setRole(UserRole.DIRECCION_GENERAL);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> leaveBalanceService.getEmployeeLeaveBalance(testEmployee.getId(), null));
    }

    @Test
    @DisplayName("FR-017: el resto de roles sigue consultando su saldo propio sin cambios")
    void otherRoles_ownBalance_unchanged() {
        for (UserRole role : List.of(UserRole.EMPLEADO, UserRole.RESPONSABLE_EQUIPO, UserRole.RRHH)) {
            testUser.setRole(role);
            assertNotNull(leaveBalanceService.getMyLeaveBalance(null), "rol " + role);
        }
    }
}
