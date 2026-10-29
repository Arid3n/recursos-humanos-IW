package com.iw.rrhh.leave.repository;

import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    @Query("""
        SELECT COALESCE(SUM(lr.calculatedDays), 0)
        FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.leaveType = :leaveType
          AND lr.status = :status
          AND lr.startDate >= :startOfYear
          AND lr.endDate <= :endOfYear
    """)
    int sumCalculatedDays(
        @Param("employeeId") UUID employeeId,
        @Param("leaveType") LeaveType leaveType,
        @Param("status") LeaveStatus status,
        @Param("startOfYear") LocalDate startOfYear,
        @Param("endOfYear") LocalDate endOfYear
    );

    @Query("""
        SELECT lr FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.startDate >= :startOfYear
          AND lr.endDate <= :endOfYear
        ORDER BY lr.startDate DESC
    """)
    List<LeaveRequest> findByEmployeeAndYear(
        @Param("employeeId") UUID employeeId,
        @Param("startOfYear") LocalDate startOfYear,
        @Param("endOfYear") LocalDate endOfYear
    );
}

