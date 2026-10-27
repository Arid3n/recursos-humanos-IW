package com.iw.rrhh.employee.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ContractHistoryItemResponse(
    Long id,
    LocalDate effectiveFrom,
    String position,
    String department,
    BigDecimal salary,
    Long collectiveAgreementId,
    String collectiveAgreementName,
    UUID managerId,
    String managerName,
    Instant createdAt,
    String createdBy
) {}
