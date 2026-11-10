package com.iw.rrhh.report.service;

import com.iw.rrhh.common.service.HolidayCalendarService;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import com.iw.rrhh.report.dto.DepartmentReportResponse;
import com.iw.rrhh.report.dto.DepartmentReportRow;
import com.iw.rrhh.report.dto.EmployeeReportRow;
import com.iw.rrhh.report.dto.ReportType;
import com.iw.rrhh.report.repository.DepartmentReportRepository;
import com.iw.rrhh.report.repository.ReportDepartmentAggregateProjection;
import com.iw.rrhh.report.repository.ReportEmployeeProjection;
import com.iw.rrhh.report.repository.ReportLeavePeriodProjection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DepartmentReportServiceImpl implements DepartmentReportService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final DepartmentReportRepository reportRepository;
    private final HolidayCalendarService holidayCalendarService;

    public DepartmentReportServiceImpl(
            DepartmentReportRepository reportRepository,
            HolidayCalendarService holidayCalendarService) {
        this.reportRepository = reportRepository;
        this.holidayCalendarService = holidayCalendarService;
    }

    @Override
    public DepartmentReportResponse generateReport(ReportType type, LocalDate startDate, LocalDate endDate) {
        if (type == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("El tipo de informe y las fechas son obligatorios");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }

        List<ReportEmployeeProjection> employees = reportRepository.findEmployeesForPeriod(endDate);
        Map<UUID, EmployeeTotals> employeeTotals = new LinkedHashMap<>();
        employees.forEach(employee -> employeeTotals.put(
                employee.getEmployeeId(),
                new EmployeeTotals(employee)));

        LeaveType leaveType = type == ReportType.VACATION_USAGE ? LeaveType.VACATION : LeaveType.PERMIT;
        Map<String, ReportDepartmentAggregateProjection> departmentAggregates = new LinkedHashMap<>();
        reportRepository.summarizeDepartments(
                        leaveType.name(), startDate, endDate, endDate)
                .forEach(row -> departmentAggregates.put(row.getDepartment(), row));
        List<UUID> employeesWithRequests = new ArrayList<>();
        Map<String, Integer> partialDaysByDepartment = new LinkedHashMap<>();
        reportRepository.sumApprovedDaysWithinPeriod(
                        LeaveStatus.APPROVED, leaveType, startDate, endDate)
                .forEach(row -> {
                    EmployeeTotals totals = employeeTotals.get(row.getEmployeeId());
                    if (totals != null) {
                        totals.takenDays += row.getTotalDays().intValue();
                        employeesWithRequests.add(row.getEmployeeId());
                    }
                });

        for (ReportLeavePeriodProjection request : reportRepository.findApprovedRequestsCrossingPeriodBoundary(
                LeaveStatus.APPROVED, leaveType, startDate, endDate)) {
            EmployeeTotals totals = employeeTotals.get(request.getEmployeeId());
            if (totals == null) {
                continue;
            }
            LocalDate overlapStart = request.getStartDate().isBefore(startDate) ? startDate : request.getStartDate();
            LocalDate overlapEnd = request.getEndDate().isAfter(endDate) ? endDate : request.getEndDate();
            int partialDays = holidayCalendarService.countWorkingDays(overlapStart, overlapEnd);
            totals.takenDays += partialDays;
            partialDaysByDepartment.merge(totals.employee.getDepartment(), partialDays, Integer::sum);
            employeesWithRequests.add(request.getEmployeeId());
        }

        String periodLabel = startDate + " – " + endDate;
        if (employeesWithRequests.isEmpty()) {
            return new DepartmentReportResponse(type, startDate, endDate, periodLabel, false, List.of());
        }

        Map<String, List<EmployeeReportRow>> rowsByDepartment = new LinkedHashMap<>();
        for (EmployeeTotals totals : employeeTotals.values()) {
            EmployeeReportRow row = totals.toRow(type, startDate, endDate, holidayCalendarService);
            rowsByDepartment.computeIfAbsent(totals.employee.getDepartment(), ignored -> new ArrayList<>()).add(row);
        }

        List<DepartmentReportRow> departments = rowsByDepartment.entrySet().stream()
                .map(entry -> aggregateDepartment(
                        entry.getKey(),
                        entry.getValue(),
                        departmentAggregates.get(entry.getKey()),
                        partialDaysByDepartment.getOrDefault(entry.getKey(), 0)))
                .toList();
        return new DepartmentReportResponse(type, startDate, endDate, periodLabel, true, departments);
    }

    @Override
    public byte[] exportCsv(DepartmentReportResponse report) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        boolean absenteeism = report.type() == ReportType.ABSENTEEISM;
        appendCsvRow(csv, List.of(
                "Tipo de informe",
                "Periodo",
                "Departamento",
                "Nivel",
                "Empleado",
                "Plantilla",
                absenteeism ? "Días laborables teóricos" : "Días correspondientes",
                absenteeism ? "Días de ausencia" : "Días disfrutados",
                absenteeism ? "Días no ausentes" : "Saldo remanente",
                absenteeism ? "Absentismo (%)" : "Uso de vacaciones (%)"
        ));

        for (DepartmentReportRow department : report.departments()) {
            appendCsvRow(csv, List.of(
                    report.type().name(),
                    report.periodLabel(),
                    department.department(),
                    "Departamento",
                    "",
                    Integer.toString(department.headcount()),
                    department.totalEntitledDays().toPlainString(),
                    department.totalTakenDays().toPlainString(),
                    department.totalRemainingDays().toPlainString(),
                    department.metricPercentage().toPlainString()
            ));
            for (EmployeeReportRow employee : department.employees()) {
                appendCsvRow(csv, List.of(
                        report.type().name(),
                        report.periodLabel(),
                        department.department(),
                        "Empleado",
                        employee.employeeName(),
                        "",
                        employee.totalEntitledDays().toPlainString(),
                        employee.totalTakenDays().toPlainString(),
                        employee.totalRemainingDays().toPlainString(),
                        employee.metricPercentage().toPlainString()
                ));
            }
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private DepartmentReportRow aggregateDepartment(
            String department,
            List<EmployeeReportRow> employees,
            ReportDepartmentAggregateProjection aggregate,
            int partialTakenDays) {
        BigDecimal entitled = employees.stream()
                .map(EmployeeReportRow::totalEntitledDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal taken = BigDecimal.valueOf(aggregate.getFullPeriodTakenDays() + partialTakenDays);
        BigDecimal remaining = entitled.subtract(taken);
        return new DepartmentReportRow(
                department,
                aggregate.getHeadcount().intValue(),
                entitled,
                taken,
                remaining,
                percentage(taken, entitled),
                employees);
    }

    private static BigDecimal percentage(BigDecimal taken, BigDecimal entitled) {
        if (entitled.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return taken.multiply(HUNDRED).divide(entitled, 2, RoundingMode.HALF_UP);
    }

    private static void appendCsvRow(StringBuilder csv, List<String> fields) {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append('"').append(fields.get(i).replace("\"", "\"\"")).append('"');
        }
        csv.append("\r\n");
    }

    private static final class EmployeeTotals {
        private final ReportEmployeeProjection employee;
        private int takenDays;

        private EmployeeTotals(ReportEmployeeProjection employee) {
            this.employee = employee;
        }

        private EmployeeReportRow toRow(
                ReportType type,
                LocalDate startDate,
                LocalDate endDate,
                HolidayCalendarService calendar) {
            BigDecimal entitledDays;
            if (type == ReportType.ABSENTEEISM) {
                LocalDate employmentStart = employee.getHireDate().isAfter(startDate)
                        ? employee.getHireDate()
                        : startDate;
                entitledDays = BigDecimal.valueOf(calendar.countWorkingDays(employmentStart, endDate));
            } else {
                entitledDays = vacationEntitlement(employee, startDate.getYear(), endDate.getYear());
            }
            BigDecimal taken = BigDecimal.valueOf(takenDays);
            return new EmployeeReportRow(
                    employee.getEmployeeId(),
                    employee.getEmployeeName(),
                    entitledDays,
                    taken,
                    entitledDays.subtract(taken),
                    percentage(taken, entitledDays));
        }

        private static BigDecimal vacationEntitlement(
                ReportEmployeeProjection employee,
                int firstYear,
                int lastYear) {
            BigDecimal total = BigDecimal.ZERO;
            for (int year = firstYear; year <= lastYear; year++) {
                int hireYear = employee.getHireDate().getYear();
                if (hireYear > year) {
                    continue;
                }
                int annualDays = employee.getVacationDaysPerYear();
                if (hireYear == year) {
                    int remainingMonths = 12 - employee.getHireDate().getMonthValue() + 1;
                    annualDays = (int) Math.round((annualDays / 12.0) * remainingMonths);
                }
                total = total.add(BigDecimal.valueOf(annualDays));
            }
            return total;
        }
    }
}
