package com.iw.rrhh.employee.repository;

import com.iw.rrhh.employee.domain.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
    Page<Employee> findByActive(boolean active, Pageable pageable);
    List<Employee> findByManagerIdAndActiveTrue(UUID managerId);
    Optional<Employee> findByUserId(UUID userId);

    /**
     * Bloquea la fila del empleado (SELECT ... FOR UPDATE) para serializar las
     * operaciones que consumen su saldo de vacaciones (Art. 12.6, HU-05).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") UUID id);
}
