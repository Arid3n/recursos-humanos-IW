package com.iw.rrhh.employee.repository;

import com.iw.rrhh.employee.domain.ContractRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRecordRepository extends JpaRepository<ContractRecord, Long> {

    @Query("""
        SELECT c FROM ContractRecord c
        JOIN FETCH c.collectiveAgreement
        LEFT JOIN FETCH c.manager
        WHERE c.employee.id = :employeeId AND c.effectiveFrom <= :targetDate
        ORDER BY c.effectiveFrom DESC, c.createdAt DESC
        LIMIT 1
    """)
    Optional<ContractRecord> findCurrentContract(
        @Param("employeeId") UUID employeeId,
        @Param("targetDate") LocalDate targetDate
    );

    @Query("""
        SELECT c FROM ContractRecord c
        JOIN FETCH c.collectiveAgreement
        LEFT JOIN FETCH c.manager
        WHERE c.employee.id = :employeeId
        ORDER BY c.effectiveFrom ASC, c.createdAt ASC
    """)
    List<ContractRecord> findFullHistoryChronological(@Param("employeeId") UUID employeeId);
}
