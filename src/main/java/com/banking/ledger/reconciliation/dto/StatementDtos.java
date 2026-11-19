package com.banking.ledger.reconciliation.dto;

import com.banking.ledger.reconciliation.domain.BankStatementEntry;
import com.banking.ledger.reconciliation.domain.enums.EntryType;
import com.banking.ledger.reconciliation.domain.enums.StatementStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class StatementDtos {

    private StatementDtos() {
    }

    public record StatementEntryRequest(
            @NotNull LocalDate statementDate,
            LocalDate valueDate,
            @Size(max = 64) String reference,
            @Size(max = 255) String description,
            @NotNull EntryType type,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount) {
    }

    public record StatementBulkRequest(
            @NotEmpty List<@NotNull StatementEntryRequest> entries) {
    }

    public record StatementEntryResponse(
            Long id,
            Long bankAccountId,
            LocalDate statementDate,
            LocalDate valueDate,
            String reference,
            String description,
            EntryType type,
            BigDecimal amount,
            StatementStatus status,
            Long matchedLedgerEntryId,
            Long reconciliationRunId,
            LocalDateTime createdAt) {

        public static StatementEntryResponse from(BankStatementEntry entry) {
            return new StatementEntryResponse(
                    entry.getId(),
                    entry.getBankAccount().getId(),
                    entry.getStatementDate(),
                    entry.getValueDate(),
                    entry.getReference(),
                    entry.getDescription(),
                    entry.getType(),
                    entry.getAmount(),
                    entry.getStatus(),
                    entry.getMatchedLedgerEntry() == null ? null : entry.getMatchedLedgerEntry().getId(),
                    entry.getReconciliationRun() == null ? null : entry.getReconciliationRun().getId(),
                    entry.getCreatedAt());
        }
    }
}