package com.banking.ledger.Reconcilation.domain;

import com.banking.ledger.Reconcilation.domain.enums.EntryType;
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
 * A single line in the internal ledger for a {@link BankAccount}.
 */
@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_entries_account_date", columnList = "bank_account_id, entry_date"),
                @Index(name = "idx_ledger_entries_reference", columnList = "reference_no"),
                @Index(name = "idx_ledger_entries_reconciled", columnList = "reconciled")
        })
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bank_account_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ledger_entries_account"))
    private BankAccount bankAccount;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

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

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "external_reference", length = 64)
    private String externalReference;

    /** True once this line has been matched during a reconciliation run. */
    @Column(name = "reconciled", nullable = false)
    private boolean reconciled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}