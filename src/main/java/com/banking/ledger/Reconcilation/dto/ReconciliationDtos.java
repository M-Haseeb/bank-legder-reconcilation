package com.banking.ledger.Reconcilation.dto;

import com.banking.ledger.Reconcilation.domain.ReconciliationRun;
import com.banking.ledger.Reconcilation.domain.enums.RunStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ReconciliationDtos {

    private ReconciliationDtos() {
    }

    public record ReconciliationRequest(
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd,
            @Size(max = 500) String notes) {
    }

    public record ReconciliationRunResponse(
            Long id,
            Long bankAccountId,
            LocalDate periodStart,
            LocalDate periodEnd,
            RunStatus status,
            int matchedCount,
            int unmatchedCount,
            int discrepancyCount,
            BigDecimal totalStatementDebits,
            BigDecimal totalStatementCredits,
            BigDecimal netDifference,
            String notes,
            LocalDateTime startedAt,
            LocalDateTime completedAt) {

        public static ReconciliationRunResponse from(ReconciliationRun run) {
            return new ReconciliationRunResponse(
                    run.getId(),
                    run.getBankAccount().getId(),
                    run.getPeriodStart(),
                    run.getPeriodEnd(),
                    run.getStatus(),
                    run.getMatchedCount(),
                    run.getUnmatchedCount(),
                    run.getDiscrepancyCount(),
                    run.getTotalStatementDebits(),
                    run.getTotalStatementCredits(),
                    run.getNetDifference(),
                    run.getNotes(),
                    run.getStartedAt(),
                    run.getCompletedAt());
        }
    }
}