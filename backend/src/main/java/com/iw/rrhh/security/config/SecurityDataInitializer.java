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
import java.util.List;

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
            if (!passwordEncoder.matches(defaultPassword, rrhhUser.getPasswordHash())) {
                rrhhUser.setPasswordHash(passwordEncoder.encode(defaultPassword));
                rrhhUser = userRepository.save(rrhhUser);
            }

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

            // 2. Usuario y Empleado de Pruebas (Rol RESPONSABLE_EQUIPO) — HU-05
            String managerUsername = "responsable@empresa.com";
            User managerUser = userRepository.findByUsername(managerUsername).orElseGet(() -> {
                User u = new User(managerUsername, passwordEncoder.encode(defaultPassword), UserRole.RESPONSABLE_EQUIPO);
                return userRepository.save(u);
            });

            String managerNationalId = "11111111H";
            if (employeeRepository.findByUserId(managerUser.getId()).isEmpty()
                    && !employeeRepository.existsByEmail(managerUsername)
                    && !employeeRepository.existsByNationalId(managerNationalId)) {
                Employee teamManager = new Employee();
                teamManager.setUser(managerUser);
                teamManager.setFirstName("Carlos");
                teamManager.setLastName("Ruiz Navarro");
                teamManager.setNationalId(managerNationalId);
                teamManager.setEmail(managerUsername);
                teamManager.setHireDate(LocalDate.of(2021, 6, 1));
                teamManager = employeeRepository.save(teamManager);

                contractRecordRepository.save(new ContractRecord(
                        teamManager,
                        LocalDate.of(2021, 6, 1),
                        "Responsable de Equipo de Desarrollo",
                        "Tecnología",
                        new BigDecimal("42000.00"),
                        agreement,
                        null,
                        "SYSTEM"));
                log.info("Inicializada ficha de responsable de equipo para {}", managerUsername);
            }

            // 3. Usuario y Empleado de Pruebas (Rol EMPLEADO)
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

            // 4. Asignar el responsable de pruebas al empleado de pruebas si no tiene responsable (HU-05)
            employeeRepository.findByUserId(empUser.getId())
                    .filter(emp -> emp.getManager() == null)
                    .ifPresent(emp -> employeeRepository.findByUserId(managerUser.getId()).ifPresent(mgr -> {
                        emp.setManager(mgr);
                        employeeRepository.save(emp);
                        log.info("Asignado responsable de equipo a {}", empUsername);
                    }));

            // 5. Dirección General (HU-05 FR-017): cúspide de la jerarquía; todo el resto tiene responsable
            String direccionUsername = "direccion@empresa.com";
            User direccionUser = userRepository.findByUsername(direccionUsername).orElseGet(() -> {
                User u = new User(direccionUsername, passwordEncoder.encode(defaultPassword), UserRole.DIRECCION_GENERAL);
                return userRepository.save(u);
            });

            String direccionNationalId = "22222222J";
            if (employeeRepository.findByUserId(direccionUser.getId()).isEmpty()
                    && !employeeRepository.existsByEmail(direccionUsername)
                    && !employeeRepository.existsByNationalId(direccionNationalId)) {
                Employee direccion = new Employee();
                direccion.setUser(direccionUser);
                direccion.setFirstName("Elena");
                direccion.setLastName("Vidal Ortega");
                direccion.setNationalId(direccionNationalId);
                direccion.setEmail(direccionUsername);
                direccion.setHireDate(LocalDate.of(2019, 1, 1));
                direccion = employeeRepository.save(direccion);

                contractRecordRepository.save(new ContractRecord(
                        direccion,
                        LocalDate.of(2019, 1, 1),
                        "Dirección General",
                        "Dirección",
                        new BigDecimal("70000.00"),
                        agreement,
                        null,
                        "SYSTEM"));
                log.info("Inicializada ficha de Dirección General para {}", direccionUsername);
            }

            // 6. Dirección General es el responsable de RRHH y del responsable de equipo de pruebas (solo si faltan)
            final List<User> directReportsOfDireccion = List.of(userRepository.findByUsername(rrhhUsername).orElseThrow(), managerUser);
            employeeRepository.findByUserId(direccionUser.getId()).ifPresent(dg -> {
                for (User subordinate : directReportsOfDireccion) {
                    employeeRepository.findByUserId(subordinate.getId())
                            .filter(emp -> emp.getManager() == null)
                            .ifPresent(emp -> {
                                emp.setManager(dg);
                                employeeRepository.save(emp);
                                log.info("Asignada Dirección General como responsable de {}", subordinate.getUsername());
                            });
                }
            });
        } catch (Exception ex) {
            log.warn("Advertencia durante la inicialización de datos de seguridad: {}", ex.getMessage());
        }
    }
}
