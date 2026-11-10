package com.iw.rrhh.report.repository;

public interface ReportDepartmentAggregateProjection {
    String getDepartment();
    Long getHeadcount();
    Long getFullPeriodTakenDays();
}
