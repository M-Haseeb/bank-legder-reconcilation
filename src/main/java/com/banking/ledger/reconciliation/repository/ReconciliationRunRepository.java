package com.banking.ledger.reconciliation.repository;

import com.banking.ledger.reconciliation.domain.ReconciliationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {

    Page<ReconciliationRun> findByBankAccountIdOrderByStartedAtDesc(Long bankAccountId, Pageable pageable);

    Optional<ReconciliationRun> findByIdAndBankAccountId(Long id, Long bankAccountId);

    boolean existsByBankAccountId(Long bankAccountId);
}