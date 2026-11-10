package com.iw.rrhh.report.repository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReportEmployeeProjection {
    UUID getEmployeeId();
    String getEmployeeName();
    String getDepartment();
    LocalDate getHireDate();
    Integer getVacationDaysPerYear();
}
