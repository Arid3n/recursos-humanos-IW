package com.iw.rrhh.employee.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateContractVariationRequest(
    @NotNull(message = "La fecha de entrada en vigor es obligatoria")
    LocalDate effectiveFrom,

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
    Long collectiveAgreementId,

    UUID managerId
) {}
