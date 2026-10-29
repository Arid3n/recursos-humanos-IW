package com.iw.rrhh.employee.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeProfileResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String nationalId,
    LocalDate hireDate,
    String currentPosition,
    String currentDepartment,
    String currentAgreementName,
    String managerFullName
) {}

