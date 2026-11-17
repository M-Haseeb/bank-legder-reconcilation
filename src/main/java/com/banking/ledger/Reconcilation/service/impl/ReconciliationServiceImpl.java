package com.banking.ledger.Reconcilation.service.impl;

import com.banking.ledger.Reconcilation.domain.BankAccount;
import com.banking.ledger.Reconcilation.domain.BankStatementEntry;
import com.banking.ledger.Reconcilation.domain.LedgerEntry;
import com.banking.ledger.Reconcilation.domain.ReconciliationRun;
import com.banking.ledger.Reconcilation.domain.enums.EntryType;
import com.banking.ledger.Reconcilation.domain.enums.RunStatus;
import com.banking.ledger.Reconcilation.domain.enums.StatementStatus;
import com.banking.ledger.Reconcilation.dto.PageResponse;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRequest;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRunResponse;
import com.banking.ledger.Reconcilation.exception.BusinessRuleException;
import com.banking.ledger.Reconcilation.exception.ResourceNotFoundException;
import com.banking.ledger.Reconcilation.repository.BankStatementEntryRepository;
import com.banking.ledger.Reconcilation.repository.LedgerEntryRepository;
import com.banking.ledger.Reconcilation.repository.ReconciliationRunRepository;
import com.banking.ledger.Reconcilation.service.BankAccountService;
import com.banking.ledger.Reconcilation.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic statement-to-ledger matcher.
 * <p>
 * A statement line matches a ledger line when type and amount are equal and the
 * dates fall within {@link #DATE_TOLERANCE_DAYS}. The closest unused ledger line
 * wins, which makes repeated runs reproducible.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReconciliationServiceImpl implements ReconciliationService {

    private final ReconciliationRunRepository runRepository;
    private final BankStatementEntryRepository statementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankAccountService bankAccountService;

    @Override
    @Transactional
    public ReconciliationRunResponse run(Long accountId, ReconciliationRequest request) {
        if (request.periodEnd().isBefore(request.periodStart())) {
            throw new BusinessRuleException("periodEnd must not be before periodStart");
        }

        BankAccount account = bankAccountService.requireAccount(accountId);

        ReconciliationRun run = new ReconciliationRun();
        run.setBankAccount(account);
        run.setPeriodStart(request.periodStart());
        run.setPeriodEnd(request.periodEnd());
        run.setNotes(request.notes());
        run.setStatus(RunStatus.IN_PROGRESS);
        run = runRepository.save(run);

        List<BankStatementEntry> statements =
                statementRepository.findByBankAccountIdAndStatementDateBetweenAndStatusOrderByStatementDateAscIdAsc(
                        accountId, request.periodStart(), request.periodEnd(), StatementStatus.UNMATCHED);

        List<LedgerEntry> ledgerEntries =
                ledgerEntryRepository.findByBankAccountIdAndEntryDateBetweenAndReconciledFalseOrderByEntryDateAscIdAsc(
                        accountId, request.periodStart(), request.periodEnd());

        Map<MatchKey, Deque<LedgerEntry>> ledgerIndex = indexLedgerEntries(ledgerEntries);

        int matched = 0;
        int unmatched = 0;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (BankStatementEntry statement : statements) {
            if (statement.getType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(statement.getAmount());
            } else {
                totalCredits = totalCredits.add(statement.getAmount());
            }

            LedgerEntry match = findClosestMatch(ledgerIndex, statement);
            if (match == null) {
                unmatched++;
                continue;
            }

            match.setReconciled(true);
            statement.setMatchedLedgerEntry(match);
            statement.setStatus(StatementStatus.MATCHED);
            statement.setReconciliationRun(run);
            matched++;
        }

        BigDecimal ledgerNet = ledgerEntries.stream()
                .filter(LedgerEntry::isReconciled)
                .map(entry -> entry.getType().applySign(entry.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal statementNet = totalDebits.subtract(totalCredits);

        run.setMatchedCount(matched);
        run.setUnmatchedCount(unmatched);
        run.setDiscrepancyCount(0);
        run.setTotalStatementDebits(totalDebits);
        run.setTotalStatementCredits(totalCredits);
        run.setNetDifference(statementNet.subtract(ledgerNet).setScale(4, RoundingMode.HALF_UP));
        run.setStatus(RunStatus.COMPLETED);
        run.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));

        log.info("Reconciliation run {} for account {}: {} matched, {} unmatched",
                run.getId(), accountId, matched, unmatched);

        return ReconciliationRunResponse.from(run);
    }

    @Override
    public PageResponse<ReconciliationRunResponse> list(Long accountId, Pageable pageable) {
        bankAccountService.requireAccount(accountId);
        return PageResponse.from(
                runRepository.findByBankAccountIdOrderByStartedAtDesc(accountId, pageable),
                ReconciliationRunResponse::from);
    }

    @Override
    public ReconciliationRunResponse get(Long accountId, Long runId) {
        return ReconciliationRunResponse.from(runRepository.findByIdAndBankAccountId(runId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reconciliation run " + runId + " for account " + accountId)));
    }

    private Map<MatchKey, Deque<LedgerEntry>> indexLedgerEntries(List<LedgerEntry> entries) {
        Map<MatchKey, Deque<LedgerEntry>> index = new HashMap<>();
        for (LedgerEntry entry : entries) {
            index.computeIfAbsent(MatchKey.of(entry), key -> new ArrayDeque<>()).add(entry);
        }
        return index;
    }

    private LedgerEntry findClosestMatch(Map<MatchKey, Deque<LedgerEntry>> index, BankStatementEntry statement) {
        Deque<LedgerEntry> candidates = index.get(MatchKey.of(statement));
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        LedgerEntry closest = null;
        long closestDays = Long.MAX_VALUE;
        for (LedgerEntry candidate : candidates) {
            long days = Math.abs(ChronoUnit.DAYS.between(candidate.getEntryDate(), statement.getStatementDate()));
            if (days <= DATE_TOLERANCE_DAYS && days < closestDays) {
                closest = candidate;
                closestDays = days;
            }
        }

        if (closest != null) {
            candidates.remove(closest);
            if (candidates.isEmpty()) {
                index.remove(MatchKey.of(statement));
            }
        }
        return closest;
    }

    private record MatchKey(EntryType type, BigDecimal amount) {

        static MatchKey of(LedgerEntry entry) {
            return new MatchKey(entry.getType(), normalise(entry.getAmount()));
        }

        static MatchKey of(BankStatementEntry entry) {
            return new MatchKey(entry.getType(), normalise(entry.getAmount()));
        }

        private static BigDecimal normalise(BigDecimal amount) {
            return amount.setScale(4, RoundingMode.HALF_UP);
        }
    }
}