package com.iw.rrhh.leave.domain;

import java.util.UUID;

public record LeaveBalance(
    UUID employeeId,
    int year,
    int entitledDays,
    int spentDays,
    int pendingDays,
    int availableDays,
    boolean isClosedYear,
    String agreementCode,
    String agreementName
) {
    public LeaveBalance {
        if (availableDays < 0) {
            availableDays = 0;
        }
    }
}

