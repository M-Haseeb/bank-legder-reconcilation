package com.banking.ledger.Reconcilation.service;

import com.banking.ledger.Reconcilation.domain.BankAccount;
import com.banking.ledger.Reconcilation.domain.BankStatementEntry;
import com.banking.ledger.Reconcilation.domain.LedgerEntry;
import com.banking.ledger.Reconcilation.domain.enums.AccountStatus;
import com.banking.ledger.Reconcilation.domain.enums.AccountType;
import com.banking.ledger.Reconcilation.domain.enums.EntryType;
import com.banking.ledger.Reconcilation.domain.enums.StatementStatus;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRequest;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRunResponse;
import com.banking.ledger.Reconcilation.repository.BankAccountRepository;
import com.banking.ledger.Reconcilation.repository.BankStatementEntryRepository;
import com.banking.ledger.Reconcilation.repository.LedgerEntryRepository;
import com.banking.ledger.Reconcilation.repository.ReconciliationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReconciliationServiceTest {

    @Autowired
    private ReconciliationService reconciliationService;
    @Autowired
    private BankAccountRepository accountRepository;
    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;
    @Autowired
    private BankStatementEntryRepository statementRepository;
    @Autowired
    private ReconciliationRunRepository runRepository;

    private BankAccount account;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        statementRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        runRepository.deleteAll();
        accountRepository.deleteAll();

        today = LocalDate.of(2026, 1, 15);

        account = new BankAccount();
        account.setAccountNumber("TEST-0001");
        account.setAccountName("Test Account");
        account.setBankName("Test Bank");
        account.setCurrency("USD");
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setOpeningBalance(new BigDecimal("1000.0000"));
        account.setCurrentBalance(new BigDecimal("1000.0000"));
        account = accountRepository.save(account);
    }

    @Test
    void matchesStatementEntryToLedgerEntryWithSameAmountAndNearbyDate() {
        ledgerEntry(today, EntryType.DEBIT, new BigDecimal("250.00"));
        statementEntry(today.plusDays(1), EntryType.DEBIT, new BigDecimal("250.00"));

        ReconciliationRunResponse run = reconciliationService.run(
                account.getId(), new ReconciliationRequest(today.minusDays(1), today.plusDays(5), null));

        assertThat(run.matchedCount()).isEqualTo(1);
        assertThat(run.unmatchedCount()).isZero();
        assertThat(run.netDifference()).isEqualByComparingTo("0");

        BankStatementEntry statement = statementRepository.findAll().get(0);
        assertThat(statement.getStatus()).isEqualTo(StatementStatus.MATCHED);
        assertThat(statement.getMatchedLedgerEntry()).isNotNull();
        assertThat(ledgerEntryRepository.findAll().get(0).isReconciled()).isTrue();
    }

    @Test
    void doesNotMatchWhenAmountsDiffer() {
        ledgerEntry(today, EntryType.DEBIT, new BigDecimal("250.00"));
        statementEntry(today, EntryType.DEBIT, new BigDecimal("251.00"));

        ReconciliationRunResponse run = reconciliationService.run(
                account.getId(), new ReconciliationRequest(today.minusDays(1), today.plusDays(5), null));

        assertThat(run.matchedCount()).isZero();
        assertThat(run.unmatchedCount()).isEqualTo(1);
        assertThat(statementRepository.findAll().get(0).getStatus()).isEqualTo(StatementStatus.UNMATCHED);
    }

    @Test
    void doesNotMatchWhenDatesAreOutsideTolerance() {
        ledgerEntry(today, EntryType.DEBIT, new BigDecimal("250.00"));
        statementEntry(today.plusDays(ReconciliationService.DATE_TOLERANCE_DAYS + 1), EntryType.DEBIT,
                new BigDecimal("250.00"));

        ReconciliationRunResponse run = reconciliationService.run(
                account.getId(), new ReconciliationRequest(today.minusDays(1), today.plusDays(30), null));

        assertThat(run.matchedCount()).isZero();
        assertThat(run.unmatchedCount()).isEqualTo(1);
    }

    private void ledgerEntry(LocalDate date, EntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setBankAccount(account);
        entry.setEntryDate(date);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(new BigDecimal("1000.0000"));
        ledgerEntryRepository.save(entry);
    }

    private void statementEntry(LocalDate date, EntryType type, BigDecimal amount) {
        BankStatementEntry entry = new BankStatementEntry();
        entry.setBankAccount(account);
        entry.setStatementDate(date);
        entry.setType(type);
        entry.setAmount(amount);
        entry.setStatus(StatementStatus.UNMATCHED);
        statementRepository.save(entry);
    }
}