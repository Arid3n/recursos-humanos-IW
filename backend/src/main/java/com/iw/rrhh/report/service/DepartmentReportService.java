package com.iw.rrhh.report.service;

import com.iw.rrhh.report.dto.DepartmentReportResponse;
import com.iw.rrhh.report.dto.ReportType;

import java.time.LocalDate;

public interface DepartmentReportService {
    DepartmentReportResponse generateReport(ReportType type, LocalDate startDate, LocalDate endDate);
    byte[] exportCsv(DepartmentReportResponse report);
}
