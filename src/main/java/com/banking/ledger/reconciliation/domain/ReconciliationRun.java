package com.banking.ledger.reconciliation.domain;

import com.banking.ledger.reconciliation.domain.enums.RunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * An immutable audit record of one reconciliation attempt over a date range.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "reconciliation_runs",
        indexes = @Index(name = "idx_reconciliation_runs_account_started", columnList = "bank_account_id, started_at"))
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bank_account_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_reconciliation_runs_account"))
    private BankAccount bankAccount;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RunStatus status = RunStatus.IN_PROGRESS;

    @Column(name = "matched_count", nullable = false)
    private int matchedCount;

    @Column(name = "unmatched_count", nullable = false)
    private int unmatchedCount;

    @Column(name = "discrepancy_count", nullable = false)
    private int discrepancyCount;

    @Column(name = "total_statement_debits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalStatementDebits = Money.ZERO;

    @Column(name = "total_statement_credits", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalStatementCredits = Money.ZERO;

    @Column(name = "net_difference", nullable = false, precision = 19, scale = 4)
    private BigDecimal netDifference = Money.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @CreatedBy
    @Column(name = "created_by", length = 120, updatable = false)
    private String createdBy;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}