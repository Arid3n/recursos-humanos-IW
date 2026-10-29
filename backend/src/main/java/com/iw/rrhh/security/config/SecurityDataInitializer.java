package com.iw.rrhh.security.config;

import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.repository.CollectiveAgreementRepository;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class SecurityDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityDataInitializer.class);

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final CollectiveAgreementRepository collectiveAgreementRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final PasswordEncoder passwordEncoder;

    public SecurityDataInitializer(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            CollectiveAgreementRepository collectiveAgreementRepository,
            ContractRecordRepository contractRecordRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.collectiveAgreementRepository = collectiveAgreementRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // Asegurar que existe convenio por defecto
            CollectiveAgreement agreement = collectiveAgreementRepository.findByCode("GENERAL")
                    .orElseGet(() -> collectiveAgreementRepository.save(
                            new CollectiveAgreement("GENERAL", "Convenio Colectivo General de Empresa", 22)));

            // 1. Usuario y Empleado Inicial RRHH
            String rrhhUsername = "rrhh@empresa.com";
            String defaultPassword = "admin123";

            User rrhhUser = userRepository.findByUsername(rrhhUsername).orElseGet(() -> {
                User u = new User(rrhhUsername, passwordEncoder.encode(defaultPassword), UserRole.RRHH);
                return userRepository.save(u);
            });

            String rrhhNationalId = "00000000T";
            if (employeeRepository.findByUserId(rrhhUser.getId()).isEmpty()
                    && !employeeRepository.existsByEmail(rrhhUsername)
                    && !employeeRepository.existsByNationalId(rrhhNationalId)) {
                Employee rrhhEmployee = new Employee();
                rrhhEmployee.setUser(rrhhUser);
                rrhhEmployee.setFirstName("Admin");
                rrhhEmployee.setLastName("Recursos Humanos");
                rrhhEmployee.setNationalId(rrhhNationalId);
                rrhhEmployee.setEmail(rrhhUsername);
                rrhhEmployee.setHireDate(LocalDate.of(2022, 1, 1));
                rrhhEmployee = employeeRepository.save(rrhhEmployee);

                contractRecordRepository.save(new ContractRecord(
                        rrhhEmployee,
                        LocalDate.of(2022, 1, 1),
                        "Responsable de RRHH",
                        "Recursos Humanos",
                        new BigDecimal("45000.00"),
                        agreement,
                        null,
                        "SYSTEM"));
                log.info("Inicializada ficha de empleado para {}", rrhhUsername);
            }

            // 2. Usuario y Empleado de Pruebas (Rol EMPLEADO)
            String empUsername = "empleado@empresa.com";
            User empUser = userRepository.findByUsername(empUsername).orElseGet(() -> {
                User u = new User(empUsername, passwordEncoder.encode(defaultPassword), UserRole.EMPLEADO);
                return userRepository.save(u);
            });

            String empNationalId = "99999999R";
            if (employeeRepository.findByUserId(empUser.getId()).isEmpty()
                    && !employeeRepository.existsByEmail(empUsername)
                    && !employeeRepository.existsByNationalId(empNationalId)) {
                Employee regularEmployee = new Employee();
                regularEmployee.setUser(empUser);
                regularEmployee.setFirstName("Laura");
                regularEmployee.setLastName("Martínez Gómez");
                regularEmployee.setNationalId(empNationalId);
                regularEmployee.setEmail(empUsername);
                regularEmployee.setHireDate(LocalDate.of(2023, 3, 1));
                regularEmployee = employeeRepository.save(regularEmployee);

                contractRecordRepository.save(new ContractRecord(
                        regularEmployee,
                        LocalDate.of(2023, 3, 1),
                        "Desarrolladora de Software",
                        "Tecnología",
                        new BigDecimal("32000.00"),
                        agreement,
                        null,
                        "SYSTEM"));
                log.info("Inicializada ficha de empleado regular para {}", empUsername);
            }
        } catch (Exception ex) {
            log.warn("Advertencia durante la inicialización de datos de seguridad: {}", ex.getMessage());
        }
    }
}
