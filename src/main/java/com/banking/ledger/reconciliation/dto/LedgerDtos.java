package com.banking.ledger.reconciliation.dto;

import com.banking.ledger.reconciliation.domain.LedgerEntry;
import com.banking.ledger.reconciliation.domain.enums.EntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LedgerDtos {

    private LedgerDtos() {
    }

    public record LedgerEntryRequest(
            @NotNull LocalDate entryDate,
            LocalDate valueDate,
            @Size(max = 64) String reference,
            @Size(max = 255) String description,
            @NotNull EntryType type,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
            @Size(max = 64) String externalReference) {
    }

    public record LedgerEntryResponse(
            Long id,
            Long bankAccountId,
            LocalDate entryDate,
            LocalDate valueDate,
            String reference,
            String description,
            EntryType type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String externalReference,
            boolean reconciled,
            LocalDateTime createdAt) {

        public static LedgerEntryResponse from(LedgerEntry entry) {
            return new LedgerEntryResponse(
                    entry.getId(),
                    entry.getBankAccount().getId(),
                    entry.getEntryDate(),
                    entry.getValueDate(),
                    entry.getReference(),
                    entry.getDescription(),
                    entry.getType(),
                    entry.getAmount(),
                    entry.getBalanceAfter(),
                    entry.getExternalReference(),
                    entry.isReconciled(),
                    entry.getCreatedAt());
        }
    }
}