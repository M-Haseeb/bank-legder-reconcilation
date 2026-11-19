package com.banking.ledger.reconciliation.config;

import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.BankStatementEntry;
import com.banking.ledger.reconciliation.domain.LedgerEntry;
import com.banking.ledger.reconciliation.domain.Money;
import com.banking.ledger.reconciliation.domain.enums.AccountStatus;
import com.banking.ledger.reconciliation.domain.enums.AccountType;
import com.banking.ledger.reconciliation.domain.enums.EntryType;
import com.banking.ledger.reconciliation.domain.enums.StatementStatus;
import com.banking.ledger.reconciliation.repository.BankAccountRepository;
import com.banking.ledger.reconciliation.repository.BankStatementEntryRepository;
import com.banking.ledger.reconciliation.repository.LedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a small demo dataset on first start so the API is explorable immediately.
 * Disable with {@code APP_SEED_ENABLED=false} (or {@code app.seed.enabled=false}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final BankAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankStatementEntryRepository statementRepository;

    @Bean
    ApplicationRunner seedDemoData() {
        return args -> seed();
    }

    @Transactional
    void seed() {
        if (accountRepository.count() > 0) {
            log.info("Seed skipped: bank_accounts already contains data");
            return;
        }

        BankAccount account = new BankAccount();
        account.setAccountNumber("000123456789");
        account.setAccountName("Operating Account");
        account.setBankName("First National Bank");
        account.setBranchName("Downtown");
        account.setCurrency("USD");
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        BigDecimal opening = new BigDecimal("10000.0000");
        account.setOpeningBalance(opening);
        account.setCurrentBalance(opening);
        account = accountRepository.save(account);

        LocalDate today = LocalDate.now();
        List<LedgerEntry> ledger = new ArrayList<>();
        BigDecimal running = opening;

        running = running.add(new BigDecimal("2500.00"));
        ledger.add(ledgerEntry(account, today.minusDays(10), "INV-1001", "Customer payment", EntryType.DEBIT,
                new BigDecimal("2500.00"), running));
        running = running.subtract(new BigDecimal("450.75"));
        ledger.add(ledgerEntry(account, today.minusDays(9), "CHK-2201", "Office rent", EntryType.CREDIT,
                new BigDecimal("450.75"), running));
        running = running.subtract(new BigDecimal("120.00"));
        ledger.add(ledgerEntry(account, today.minusDays(7), "CHK-2202", "Utilities", EntryType.CREDIT,
                new BigDecimal("120.00"), running));
        running = running.add(new BigDecimal("800.00"));
        ledger.add(ledgerEntry(account, today.minusDays(5), "INV-1002", "Consulting fee", EntryType.DEBIT,
                new BigDecimal("800.00"), running));

        ledgerEntryRepository.saveAll(ledger);
        account.setCurrentBalance(running);
        accountRepository.save(account);

        List<BankStatementEntry> statements = new ArrayList<>();
        statements.add(statementEntry(account, today.minusDays(10), "INV-1001", "Customer payment", EntryType.DEBIT,
                new BigDecimal("2500.00")));
        statements.add(statementEntry(account, today.minusDays(8), "CHK-2201", "Office rent", EntryType.CREDIT,
                new BigDecimal("450.75")));
        // Deliberately unmatched to demonstrate discrepancies.
        statements.add(statementEntry(account, today.minusDays(4), "FEE-9001", "Bank service fee", EntryType.CREDIT,
                new BigDecimal("35.00")));

        statementRepository.saveAll(statements);
        log.info("Seeded 1 bank account, {} ledger entries and {} statement entries",
                ledger.size(), statements.size());
    }

    private LedgerEntry ledgerEntry(BankAccount account, LocalDate date, String reference, String description,
                                    EntryType type, BigDecimal amount, BigDecimal balanceAfter) {
        LedgerEntry entry = new LedgerEntry();
        entry.setBankAccount(account);
        entry.setEntryDate(date);
        entry.setValueDate(date);
        entry.setReference(reference);
        entry.setDescription(description);
        entry.setType(type);
        entry.setAmount(Money.of(amount));
        entry.setBalanceAfter(Money.of(balanceAfter));
        return entry;
    }

    private BankStatementEntry statementEntry(BankAccount account, LocalDate date, String reference,
                                              String description, EntryType type, BigDecimal amount) {
        BankStatementEntry entry = new BankStatementEntry();
        entry.setBankAccount(account);
        entry.setStatementDate(date);
        entry.setValueDate(date);
        entry.setReference(reference);
        entry.setDescription(description);
        entry.setType(type);
        entry.setAmount(Money.of(amount));
        entry.setStatus(StatementStatus.UNMATCHED);
        return entry;
    }
}