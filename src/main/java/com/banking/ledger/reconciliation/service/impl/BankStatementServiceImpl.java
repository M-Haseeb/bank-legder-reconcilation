package com.banking.ledger.reconciliation.service.impl;

import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.BankStatementEntry;
import com.banking.ledger.reconciliation.domain.Money;
import com.banking.ledger.reconciliation.domain.enums.StatementStatus;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.dto.StatementDtos.StatementEntryRequest;
import com.banking.ledger.reconciliation.dto.StatementDtos.StatementEntryResponse;
import com.banking.ledger.reconciliation.exception.ResourceNotFoundException;
import com.banking.ledger.reconciliation.repository.BankStatementEntryRepository;
import com.banking.ledger.reconciliation.service.BankAccountService;
import com.banking.ledger.reconciliation.service.BankStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankStatementServiceImpl implements BankStatementService {

    private final BankStatementEntryRepository statementRepository;
    private final BankAccountService bankAccountService;

    @Override
    @Transactional
    public List<StatementEntryResponse> importEntries(Long accountId, List<StatementEntryRequest> requests) {
        BankAccount account = bankAccountService.requireAccount(accountId);

        List<BankStatementEntry> saved = new ArrayList<>(requests.size());
        for (StatementEntryRequest request : requests) {
            BankStatementEntry entry = new BankStatementEntry();
            entry.setBankAccount(account);
            entry.setStatementDate(request.statementDate());
            entry.setValueDate(request.valueDate());
            entry.setReference(request.reference());
            entry.setDescription(request.description());
            entry.setType(request.type());
            entry.setAmount(Money.of(request.amount()));
            entry.setStatus(StatementStatus.UNMATCHED);
            saved.add(entry);
        }

        return statementRepository.saveAll(saved).stream()
                .map(StatementEntryResponse::from)
                .toList();
    }

    @Override
    public PageResponse<StatementEntryResponse> list(
            Long accountId, StatementStatus status, Pageable pageable) {
        bankAccountService.requireAccount(accountId);
        var page = status == null
                ? statementRepository.findByBankAccountId(accountId, pageable)
                : statementRepository.findByBankAccountIdAndStatus(accountId, status, pageable);
        return PageResponse.from(page, StatementEntryResponse::from);
    }

    @Override
    public StatementEntryResponse get(Long accountId, Long entryId) {
        return StatementEntryResponse.from(requireEntry(accountId, entryId));
    }

    @Override
    @Transactional
    public StatementEntryResponse exclude(Long accountId, Long entryId) {
        BankStatementEntry entry = requireEntry(accountId, entryId);
        entry.setStatus(StatementStatus.EXCLUDED);
        return StatementEntryResponse.from(statementRepository.save(entry));
    }

    @Override
    @Transactional
    public void delete(Long accountId, Long entryId) {
        statementRepository.delete(requireEntry(accountId, entryId));
    }

    private BankStatementEntry requireEntry(Long accountId, Long entryId) {
        return statementRepository.findByIdAndBankAccountId(entryId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statement entry " + entryId + " for account " + accountId));
    }
}