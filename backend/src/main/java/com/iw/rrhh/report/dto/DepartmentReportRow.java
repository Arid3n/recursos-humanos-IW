package com.iw.rrhh.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record DepartmentReportRow(
        String department,
        int headcount,
        BigDecimal totalEntitledDays,
        BigDecimal totalTakenDays,
        BigDecimal totalRemainingDays,
        BigDecimal metricPercentage,
        List<EmployeeReportRow> employees
) {}
