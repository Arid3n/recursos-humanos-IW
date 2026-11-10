package com.iw.rrhh.report.repository;

import java.time.LocalDate;
import java.util.UUID;

public interface ReportLeavePeriodProjection {
    UUID getEmployeeId();
    LocalDate getStartDate();
    LocalDate getEndDate();
}
