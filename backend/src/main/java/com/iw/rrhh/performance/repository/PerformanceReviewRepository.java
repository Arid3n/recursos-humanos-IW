package com.iw.rrhh.performance.repository;

import com.iw.rrhh.performance.domain.PerformanceReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    @EntityGraph(attributePaths = {"employee", "evaluator"})
    List<PerformanceReview> findByEmployeeIdOrderByPeriodDescCreatedAtDesc(UUID employeeId);

    @EntityGraph(attributePaths = {"employee", "evaluator"})
    List<PerformanceReview> findByEmployeeManagerIdOrderByCreatedAtDesc(UUID managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"employee", "evaluator"})
    @Query("SELECT review FROM PerformanceReview review WHERE review.id = :id")
    Optional<PerformanceReview> findByIdForUpdate(@Param("id") UUID id);
}
