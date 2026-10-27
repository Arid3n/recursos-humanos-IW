package com.iw.rrhh.employee.repository;

import com.iw.rrhh.employee.domain.CollectiveAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CollectiveAgreementRepository extends JpaRepository<CollectiveAgreement, Long> {
    Optional<CollectiveAgreement> findByCode(String code);
    boolean existsByCode(String code);
}
