package com.iw.rrhh.employee.controller;

import com.iw.rrhh.employee.dto.*;
import com.iw.rrhh.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@Tag(name = "Employees", description = "Gestión de empleados y trayectoria contractual (HU-01)")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Alta de nuevo empleado con credencial temporal generada")
    public ResponseEntity<CreateEmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        CreateEmployeeResponse response = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('RRHH') or hasRole('RESPONSABLE_EQUIPO')")
    @Operation(summary = "Listado paginado de empleados")
    public ResponseEntity<Page<EmployeeDetailResponse>> listEmployees(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<EmployeeDetailResponse> page = employeeService.listEmployees(activeOnly, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener los datos de la ficha personal del empleado en sesión")
    public ResponseEntity<EmployeeProfileResponse> getMyProfile() {
        EmployeeProfileResponse profile = employeeService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{id:[0-9a-fA-F-]{36}}")
    @PreAuthorize("hasRole('RRHH') or hasRole('RESPONSABLE_EQUIPO') or hasRole('EMPLEADO')")
    @Operation(summary = "Consultar ficha y condición contractual vigente de un empleado")
    public ResponseEntity<EmployeeDetailResponse> getEmployeeById(@PathVariable UUID id) {
        EmployeeDetailResponse response = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/personal-data")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Actualizar datos personales no contractuales")
    public ResponseEntity<EmployeeDetailResponse> updatePersonalData(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePersonalDataRequest request) {
        EmployeeDetailResponse response = employeeService.updatePersonalData(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/contracts")
    @PreAuthorize("hasRole('RRHH')")
    @Operation(summary = "Registrar variación contractual inmutable (Append-Only)")
    public ResponseEntity<ContractHistoryItemResponse> addContractVariation(
            @PathVariable UUID id,
            @Valid @RequestBody CreateContractVariationRequest request) {
        ContractHistoryItemResponse response = employeeService.addContractVariation(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/contracts")
    @PreAuthorize("hasRole('RRHH') or hasRole('RESPONSABLE_EQUIPO')")
    @Operation(summary = "Consultar trayectoria contractual cronológica completa")
    public ResponseEntity<List<ContractHistoryItemResponse>> getContractHistory(@PathVariable UUID id) {
        List<ContractHistoryItemResponse> history = employeeService.getContractHistory(id);
        return ResponseEntity.ok(history);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RRHH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja de empleado mediante inactivación lógica")
    public void deactivateEmployee(@PathVariable UUID id) {
        employeeService.deactivateEmployee(id);
    }
}
