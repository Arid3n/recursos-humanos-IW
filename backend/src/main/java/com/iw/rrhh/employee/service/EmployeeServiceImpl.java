package com.iw.rrhh.employee.service;

import com.iw.rrhh.employee.domain.CollectiveAgreement;
import com.iw.rrhh.employee.domain.ContractRecord;
import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.employee.dto.*;
import com.iw.rrhh.employee.repository.CollectiveAgreementRepository;
import com.iw.rrhh.employee.repository.ContractRecordRepository;
import com.iw.rrhh.employee.repository.EmployeeRepository;
import com.iw.rrhh.security.domain.User;
import com.iw.rrhh.security.domain.UserRole;
import com.iw.rrhh.security.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ContractRecordRepository contractRecordRepository;
    private final CollectiveAgreementRepository collectiveAgreementRepository;
    private final UserRepository userRepository;
    private final PasswordGeneratorService passwordGeneratorService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            ContractRecordRepository contractRecordRepository,
            CollectiveAgreementRepository collectiveAgreementRepository,
            UserRepository userRepository,
            PasswordGeneratorService passwordGeneratorService,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.contractRecordRepository = contractRecordRepository;
        this.collectiveAgreementRepository = collectiveAgreementRepository;
        this.userRepository = userRepository;
        this.passwordGeneratorService = passwordGeneratorService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Ya existe un empleado con el email " + request.email());
        }
        if (employeeRepository.existsByNationalId(request.nationalId())) {
            throw new IllegalArgumentException("Ya existe un empleado con el NIF/NIE " + request.nationalId());
        }

        CollectiveAgreement collectiveAgreement = collectiveAgreementRepository.findById(request.collectiveAgreementId())
                .orElseThrow(() -> new NoSuchElementException("Convenio colectivo no encontrado con ID: " + request.collectiveAgreementId()));

        Employee manager = null;
        if (request.managerId() != null) {
            manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new NoSuchElementException("Responsable de equipo no encontrado con ID: " + request.managerId()));
        } else if (!isTopManagement(request.position())) {
            throw new IllegalArgumentException("El responsable de equipo es obligatorio excepto para puestos de Dirección General / CEO");
        }

        // Generar credencial temporal segura
        String temporaryPassword = passwordGeneratorService.generateDefaultPassword();

        // Crear User
        User user = new User(
                request.email(),
                passwordEncoder.encode(temporaryPassword),
                UserRole.EMPLEADO
        );
        user = userRepository.save(user);

        // Crear Employee
        Employee employee = new Employee(
                user,
                request.firstName(),
                request.lastName(),
                request.nationalId(),
                request.email(),
                manager,
                request.hireDate()
        );
        employee = employeeRepository.save(employee);

        // Crear primer ContractRecord (inmutable)
        ContractRecord contractRecord = new ContractRecord(
                employee,
                request.hireDate(),
                request.position(),
                request.department(),
                request.salary(),
                collectiveAgreement,
                manager,
                "SYSTEM"
        );
        contractRecord = contractRecordRepository.save(contractRecord);

        return new CreateEmployeeResponse(
                employee.getId(),
                employee.getEmail(),
                employee.getFirstName() + " " + employee.getLastName(),
                employee.getNationalId(),
                temporaryPassword,
                contractRecord.getId(),
                employee.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public ContractHistoryItemResponse addContractVariation(UUID employeeId, CreateContractVariationRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employeeId));

        CollectiveAgreement collectiveAgreement = collectiveAgreementRepository.findById(request.collectiveAgreementId())
                .orElseThrow(() -> new NoSuchElementException("Convenio colectivo no encontrado con ID: " + request.collectiveAgreementId()));

        Employee manager = null;
        if (request.managerId() != null) {
            manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new NoSuchElementException("Responsable no encontrado con ID: " + request.managerId()));
        } else if (!isTopManagement(request.position())) {
            throw new IllegalArgumentException("El responsable de equipo es obligatorio excepto para Dirección General / CEO");
        }

        // Crear nuevo registro histórico (Append-Only)
        ContractRecord contractRecord = new ContractRecord(
                employee,
                request.effectiveFrom(),
                request.position(),
                request.department(),
                request.salary(),
                collectiveAgreement,
                manager,
                "RRHH"
        );
        contractRecord = contractRecordRepository.save(contractRecord);

        // Si la variación ya está vigente hoy, actualizar el responsable vigente en la cabecera
        if (!request.effectiveFrom().isAfter(LocalDate.now())) {
            employee.setManager(manager);
            employeeRepository.save(employee);
        }

        return mapToContractItem(contractRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployeeDetail(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employeeId));

        ContractHistoryItemResponse currentContract = contractRecordRepository.findCurrentContract(employeeId, LocalDate.now())
                .map(this::mapToContractItem)
                .orElse(null);

        return new EmployeeDetailResponse(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getNationalId(),
                employee.getEmail(),
                employee.isActive(),
                employee.getHireDate(),
                currentContract
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractHistoryItemResponse> getContractHistory(UUID employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new NoSuchElementException("Empleado no encontrado con ID: " + employeeId);
        }
        return contractRecordRepository.findFullHistoryChronological(employeeId)
                .stream()
                .map(this::mapToContractItem)
                .toList();
    }

    @Override
    @Transactional
    public EmployeeDetailResponse updatePersonalData(UUID employeeId, UpdatePersonalDataRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employeeId));

        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee = employeeRepository.save(employee);

        return getEmployeeDetail(employee.getId());
    }

    @Override
    @Transactional
    public void deactivateEmployee(UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NoSuchElementException("Empleado no encontrado con ID: " + employeeId));

        employee.setActive(false);
        if (employee.getUser() != null) {
            employee.getUser().setActive(false);
            userRepository.save(employee.getUser());
        }
        employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeDetailResponse> listEmployees(boolean activeOnly, Pageable pageable) {
        Page<Employee> page = activeOnly ? employeeRepository.findByActive(true, pageable) : employeeRepository.findAll(pageable);
        return page.map(emp -> {
            ContractHistoryItemResponse current = contractRecordRepository.findCurrentContract(emp.getId(), LocalDate.now())
                    .map(this::mapToContractItem)
                    .orElse(null);
            return new EmployeeDetailResponse(
                    emp.getId(),
                    emp.getFirstName(),
                    emp.getLastName(),
                    emp.getNationalId(),
                    emp.getEmail(),
                    emp.isActive(),
                    emp.getHireDate(),
                    current
            );
        });
    }

    private boolean isTopManagement(String position) {
        if (position == null) return false;
        String p = position.trim().toLowerCase();
        return p.contains("ceo") || p.contains("director general") || p.contains("directora general");
    }

    private ContractHistoryItemResponse mapToContractItem(ContractRecord record) {
        String managerName = null;
        UUID managerId = null;
        if (record.getManager() != null) {
            managerId = record.getManager().getId();
            managerName = record.getManager().getFirstName() + " " + record.getManager().getLastName();
        }

        return new ContractHistoryItemResponse(
                record.getId(),
                record.getEffectiveFrom(),
                record.getPosition(),
                record.getDepartment(),
                record.getSalary(),
                record.getCollectiveAgreement().getId(),
                record.getCollectiveAgreement().getName(),
                managerId,
                managerName,
                record.getCreatedAt(),
                record.getCreatedBy()
        );
    }
}
