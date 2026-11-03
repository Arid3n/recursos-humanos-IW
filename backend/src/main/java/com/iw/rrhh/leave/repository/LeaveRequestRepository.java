package com.iw.rrhh.leave.repository;

import com.iw.rrhh.leave.domain.LeaveRequest;
import com.iw.rrhh.leave.domain.LeaveStatus;
import com.iw.rrhh.leave.domain.LeaveType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    @Query("""
        SELECT COUNT(lr) > 0
        FROM LeaveRequest lr
        WHERE lr.employee.id = :employeeId
          AND lr.status IN :statuses
          AND lr.startDate <= :endDate
          AND lr.endDate >= :startDate
    """)
    boolean hasOverlappingRequests(
        @Param("employeeId") UUID employeeId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("statuses") List<LeaveStatus> statuses
    );

    org.springframework.data.domain.Page<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<LeaveRequest> findByEmployeeIdAndStatusOrderByCreatedAtDesc(UUID employeeId, LeaveStatus status, org.springframework.data.domain.Pageable pageable);

    /**
     * Bandeja del responsable (HU-05): solicitudes de sus miembros directos en un estado, más antiguas primero.
     */
    @EntityGraph(attributePaths = "employee")
    Page<LeaveRequest> findByEmployeeManagerIdAndStatusOrderByCreatedAtAsc(UUID managerId, LeaveStatus status, Pageable pageable);

    /**
     * Calendario del equipo (HU-06): solicitudes de los miembros activos del responsable que solapan el rango.
     */
    @Query("""
        SELECT lr FROM LeaveRequest lr
        JOIN FETCH lr.employee e
        WHERE e.manager.id = :managerId
          AND e.active = true
          AND lr.status IN :statuses
          AND lr.startDate <= :rangeEnd
          AND lr.endDate >= :rangeStart
        ORDER BY e.lastName, e.firstName, lr.startDate
    """)
    List<LeaveRequest> findTeamCalendarRequests(
        @Param("managerId") UUID managerId,
        @Param("statuses") List<LeaveStatus> statuses,
        @Param("rangeStart") LocalDate rangeStart,
        @Param("rangeEnd") LocalDate rangeEnd
    );

    /**
     * Carga la solicitud con SELECT ... FOR UPDATE para serializar resoluciones concurrentes (Art. 13.2).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.id = :id")
    Optional<LeaveRequest> findByIdForUpdate(@Param("id") UUID id);
}

