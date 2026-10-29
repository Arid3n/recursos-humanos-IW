package com.iw.rrhh.leave;

import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.leave.domain.LeaveBalance;
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
import java.time.LocalDate;
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

    private int spentDaysToReturn = 0;
    private int pendingDaysToReturn = 0;

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
                    if (method.getName().equals("sumCalculatedDays")) {
                        LeaveStatus status = (LeaveStatus) args[2];
                        if (status == LeaveStatus.APPROVED) {
                            return spentDaysToReturn;
                        }
                        if (status == LeaveStatus.PENDING) {
                            return pendingDaysToReturn;
                        }
                        return 0;
                    }
                    return null;
                }
        );

        leaveBalanceService = new LeaveBalanceServiceImpl(
                employeeRepository,
                userRepository,
                contractRecordRepository,
                leaveRequestRepository
        );

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_EMPLEADO"))
        );
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("HU-02.CA-1: Consulta de saldo propio de año en curso con desglose exacto")
    void testGetMyLeaveBalance_FullYear() {
        int currentYear = LocalDate.now().getYear();
        spentDaysToReturn = 7;
        pendingDaysToReturn = 3;

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
                LocalDate.now(),
                "Senior Dev",
                "IT",
                new BigDecimal("35000"),
                updatedAgreement,
                null,
                "RRHH"
        );
        spentDaysToReturn = 4;
        pendingDaysToReturn = 0;

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        assertEquals(24, balance.entitledDays());
        assertEquals(4, balance.spentDays());
        assertEquals(20, balance.availableDays(), "Disponibles debe ser 24 - 4 = 20");
    }

    @Test
    @DisplayName("HU-02: Prorrateo para incorporación a mitad de año (1 de julio)")
    void testGetMyLeaveBalance_MidYearProrate() {
        int currentYear = LocalDate.now().getYear();
        testEmployee.setHireDate(LocalDate.of(currentYear, 7, 1)); // 6 meses restantes
        spentDaysToReturn = 0;
        pendingDaysToReturn = 0;

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        // round((22 / 12) * 6) = 11
        assertEquals(11, balance.entitledDays());
        assertEquals(11, balance.availableDays());
    }

    @Test
    @DisplayName("HU-02: Saldo agotado nunca es negativo")
    void testGetMyLeaveBalance_ZeroFloorWhenExhausted() {
        spentDaysToReturn = 25; // más que los correspondientes por convenio

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(null);

        assertEquals(22, balance.entitledDays());
        assertEquals(25, balance.spentDays());
        assertEquals(0, balance.availableDays(), "No se permiten saldos negativos");
    }

    @Test
    @DisplayName("HU-02.CA-4: Consulta histórica de ejercicio fiscal anterior (solo lectura)")
    void testGetHistoricalLeaveBalance_ClosedYear() {
        int pastYear = LocalDate.now().getYear() - 1;
        spentDaysToReturn = 20;

        LeaveBalanceResponse balance = leaveBalanceService.getMyLeaveBalance(pastYear);

        assertEquals(pastYear, balance.year());
        assertTrue(balance.isClosedYear(), "Debe marcarse como ejercicio cerrado");
        assertEquals(22, balance.entitledDays());
        assertEquals(20, balance.spentDays());
        assertEquals(2, balance.availableDays());
    }
}
