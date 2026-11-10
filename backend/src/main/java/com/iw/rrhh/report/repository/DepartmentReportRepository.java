package com.iw.rrhh.report.repository;

import com.iw.rrhh.employee.domain.Employee;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DepartmentReportRepository extends Repository<Employee, UUID> {

    @Query(value = """
        SELECT e.id AS "employeeId",
               concat(e.first_name, ' ', e.last_name) AS "employeeName",
               current_contract.department AS "department",
               e.hire_date AS "hireDate",
               current_contract.vacation_days_per_year AS "vacationDaysPerYear"
        FROM employees e
        JOIN LATERAL (
            SELECT cr.department, ca.vacation_days_per_year
            FROM contract_records cr
            JOIN collective_agreements ca ON ca.id = cr.collective_agreement_id
            WHERE cr.employee_id = e.id
              AND cr.effective_from <= :endDate
            ORDER BY cr.effective_from DESC, cr.created_at DESC, cr.id DESC
            LIMIT 1
        ) current_contract ON TRUE
        WHERE e.active = TRUE
          AND e.hire_date <= :endDate
        ORDER BY current_contract.department, e.last_name, e.first_name
        """, nativeQuery = true)
    List<ReportEmployeeProjection> findEmployeesForPeriod(@Param("endDate") LocalDate endDate);

    @Query(value = """
        WITH eligible_employees AS (
            SELECT e.id AS employee_id, current_contract.department
            FROM employees e
            JOIN LATERAL (
                SELECT cr.department
                FROM contract_records cr
                WHERE cr.employee_id = e.id
                  AND cr.effective_from <= :endDate
                ORDER BY cr.effective_from DESC, cr.created_at DESC, cr.id DESC
                LIMIT 1
            ) current_contract ON TRUE
            WHERE e.active = TRUE
              AND e.hire_date <= :endDate
        )
        SELECT eligible.department AS "department",
               COUNT(DISTINCT eligible.employee_id) AS "headcount",
               COALESCE(SUM(lr.calculated_days), 0) AS "fullPeriodTakenDays"
        FROM eligible_employees eligible
        LEFT JOIN leave_requests lr
          ON lr.employee_id = eligible.employee_id
         AND lr.status = 'APPROVED'
         AND lr.leave_type = :leaveType
         AND lr.start_date >= :periodStart
         AND lr.end_date <= :periodEnd
        GROUP BY eligible.department
        ORDER BY eligible.department
        """, nativeQuery = true)
    List<ReportDepartmentAggregateProjection> summarizeDepartments(
            @Param("leaveType") String leaveType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT lr.employee.id AS employeeId, SUM(lr.calculatedDays) AS totalDays
        FROM LeaveRequest lr
        WHERE lr.status = :status
          AND lr.leaveType = :leaveType
          AND lr.startDate >= :periodStart
          AND lr.endDate <= :periodEnd
        GROUP BY lr.employee.id
        """)
    List<ReportLeaveDaysProjection> sumApprovedDaysWithinPeriod(
            @Param("status") LeaveStatus status,
            @Param("leaveType") LeaveType leaveType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);

    @Query("""
        SELECT lr.employee.id AS employeeId,
               lr.startDate AS startDate,
               lr.endDate AS endDate
        FROM LeaveRequest lr
        WHERE lr.status = :status
          AND lr.leaveType = :leaveType
          AND lr.startDate <= :periodEnd
          AND lr.endDate >= :periodStart
          AND (lr.startDate < :periodStart OR lr.endDate > :periodEnd)
        """)
    List<ReportLeavePeriodProjection> findApprovedRequestsCrossingPeriodBoundary(
            @Param("status") LeaveStatus status,
            @Param("leaveType") LeaveType leaveType,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd);
}
