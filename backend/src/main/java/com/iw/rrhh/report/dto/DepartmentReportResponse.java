package com.iw.rrhh.report.dto;

import java.time.LocalDate;
import java.util.List;

public record DepartmentReportResponse(
        ReportType type,
        LocalDate startDate,
        LocalDate endDate,
        String periodLabel,
        boolean hasData,
        List<DepartmentReportRow> departments
) {}
