package com.banking.ledger.reconciliation.domain;

import com.banking.ledger.reconciliation.domain.enums.EntryType;
import com.banking.ledger.reconciliation.domain.enums.StatementStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * A line imported from the bank's statement, to be matched against ledger entries.
 */
@Entity
@Table(
        name = "bank_statement_entries",
        indexes = {
                @Index(name = "idx_statement_entries_account_date", columnList = "bank_account_id, statement_date"),
                @Index(name = "idx_statement_entries_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class BankStatementEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bank_account_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_statement_entries_account"))
    private BankAccount bankAccount;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "reference_no", length = 64)
    private String reference;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false, length = 10)
    private EntryType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatementStatus status = StatementStatus.UNMATCHED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_ledger_entry_id")
    private LedgerEntry matchedLedgerEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reconciliation_run_id")
    private ReconciliationRun reconciliationRun;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}