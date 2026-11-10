package com.iw.rrhh.report.controller;

import com.iw.rrhh.report.dto.DepartmentReportResponse;
import com.iw.rrhh.report.dto.ReportType;
import com.iw.rrhh.report.service.DepartmentReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasRole('RRHH')")
@Tag(name = "Reports", description = "Informes de absentismo y uso de vacaciones (HU-07)")
public class DepartmentReportController {

    private final DepartmentReportService reportService;

    public DepartmentReportController(DepartmentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @Operation(summary = "Generar informe departamental de absentismo o uso de vacaciones")
    public ResponseEntity<DepartmentReportResponse> getReport(
            @RequestParam ReportType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.generateReport(type, startDate, endDate));
    }

    @GetMapping("/export")
    @Operation(summary = "Exportar el mismo informe departamental en CSV UTF-8")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam ReportType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        DepartmentReportResponse report = reportService.generateReport(type, startDate, endDate);
        if (!report.hasData()) {
            return ResponseEntity.noContent().build();
        }

        String filename = "informe-" + type.name().toLowerCase() + "-" + startDate + "-" + endDate + ".csv";
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(reportService.exportCsv(report));
    }
}
