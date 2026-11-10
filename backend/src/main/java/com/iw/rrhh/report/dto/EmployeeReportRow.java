package com.iw.rrhh.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeReportRow(
        UUID employeeId,
        String employeeName,
        BigDecimal totalEntitledDays,
        BigDecimal totalTakenDays,
        BigDecimal totalRemainingDays,
        BigDecimal metricPercentage
) {}
