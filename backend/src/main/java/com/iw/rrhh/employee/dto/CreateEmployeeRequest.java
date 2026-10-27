package com.iw.rrhh.employee.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateEmployeeRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    String firstName,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 150, message = "Los apellidos no pueden exceder 150 caracteres")
    String lastName,

    @NotBlank(message = "El NIF/DNI es obligatorio")
    @Pattern(regexp = "^[0-9]{8}[A-Za-z]$|^[XYZxyz][0-9]{7}[A-Za-z]$", message = "El formato de NIF/NIE no es válido")
    String nationalId,

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    String email,

    UUID managerId,

    @NotNull(message = "La fecha de incorporación es obligatoria")
    LocalDate hireDate,

    @NotBlank(message = "El puesto es obligatorio")
    @Size(max = 150, message = "El puesto no puede exceder 150 caracteres")
    String position,

    @NotBlank(message = "El departamento es obligatorio")
    @Size(max = 100, message = "El departamento no puede exceder 100 caracteres")
    String department,

    @NotNull(message = "El salario es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El salario no puede ser negativo")
    BigDecimal salary,

    @NotNull(message = "El convenio colectivo es obligatorio")
    Long collectiveAgreementId
) {}
