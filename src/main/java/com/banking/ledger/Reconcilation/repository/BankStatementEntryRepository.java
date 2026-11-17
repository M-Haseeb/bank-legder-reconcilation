package com.banking.ledger.Reconcilation.repository;

import com.banking.ledger.Reconcilation.domain.BankStatementEntry;
import com.banking.ledger.Reconcilation.domain.enums.EntryType;
import com.banking.ledger.Reconcilation.domain.enums.StatementStatus;
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
public interface BankStatementEntryRepository extends JpaRepository<BankStatementEntry, Long> {

    Page<BankStatementEntry> findByBankAccountId(Long bankAccountId, Pageable pageable);

    Page<BankStatementEntry> findByBankAccountIdAndStatus(
            Long bankAccountId, StatementStatus status, Pageable pageable);

    Optional<BankStatementEntry> findByIdAndBankAccountId(Long id, Long bankAccountId);

    List<BankStatementEntry> findByBankAccountIdAndStatementDateBetweenOrderByStatementDateAscIdAsc(
            Long bankAccountId, LocalDate from, LocalDate to);

    List<BankStatementEntry> findByBankAccountIdAndStatementDateBetweenAndStatusOrderByStatementDateAscIdAsc(
            Long bankAccountId, LocalDate from, LocalDate to, StatementStatus status);

    boolean existsByBankAccountId(Long bankAccountId);

    @Query("""
            select sum(s.amount) from BankStatementEntry s
            where s.bankAccount.id = :accountId
              and s.statementDate between :from and :to
              and s.type = :type
            """)
    BigDecimal sumAmountByAccountAndPeriodAndType(
            @Param("accountId") Long accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("type") EntryType type);
}