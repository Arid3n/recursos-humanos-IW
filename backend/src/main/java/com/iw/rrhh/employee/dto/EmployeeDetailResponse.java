package com.iw.rrhh.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeDetailResponse(
    UUID id,
    String firstName,
    String lastName,
    String nationalId,
    String email,
    boolean active,
    LocalDate hireDate,
    ContractHistoryItemResponse currentContract
) {}
