package com.banking.ledger.reconciliation.domain;

import com.banking.ledger.reconciliation.domain.enums.AccountStatus;
import com.banking.ledger.reconciliation.domain.enums.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * A bank account whose ledger is reconciled against imported bank statements.
 */
@Entity
@Table(
        name = "bank_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bank_accounts_account_number",
                columnNames = "account_number"),
        indexes = @Index(name = "idx_bank_accounts_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 34)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 120)
    private String accountName;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "branch_name", length = 120)
    private String branchName;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType = AccountType.CHECKING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingBalance = Money.ZERO;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentBalance = Money.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}