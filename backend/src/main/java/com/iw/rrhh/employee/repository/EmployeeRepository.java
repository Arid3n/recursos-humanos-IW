package com.iw.rrhh.employee.repository;

import com.iw.rrhh.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
