package com.iw.rrhh.leave.dto;

import java.util.UUID;

public record LeaveBalanceResponse(
    UUID employeeId,
    int year,
    int entitledDays,
    int spentDays,
    int pendingDays,
    int availableDays,
    boolean isClosedYear,
    String agreementName
) {}

