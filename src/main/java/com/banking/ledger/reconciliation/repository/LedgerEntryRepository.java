package com.banking.ledger.reconciliation.repository;

import com.banking.ledger.reconciliation.domain.LedgerEntry;
import com.banking.ledger.reconciliation.domain.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByBankAccountId(Long bankAccountId, Pageable pageable);

    Optional<LedgerEntry> findByIdAndBankAccountId(Long id, Long bankAccountId);

    List<LedgerEntry> findByBankAccountIdAndEntryDateBetweenAndReconciledFalseOrderByEntryDateAscIdAsc(
            Long bankAccountId, LocalDate from, LocalDate to);

    List<LedgerEntry> findByBankAccountIdAndEntryDateBetweenOrderByEntryDateAscIdAsc(
            Long bankAccountId, LocalDate from, LocalDate to);

    boolean existsByBankAccountId(Long bankAccountId);

    @Query("""
            select sum(e.amount) from LedgerEntry e
            where e.bankAccount.id = :accountId
              and e.entryDate between :from and :to
              and e.type = :type
            """)
    BigDecimal sumAmountByAccountAndPeriodAndType(
            @Param("accountId") Long accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") EntryType type);
}