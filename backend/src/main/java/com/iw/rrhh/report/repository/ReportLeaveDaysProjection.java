package com.iw.rrhh.report.repository;

import java.util.UUID;

public interface ReportLeaveDaysProjection {
    UUID getEmployeeId();
    Long getTotalDays();
}
