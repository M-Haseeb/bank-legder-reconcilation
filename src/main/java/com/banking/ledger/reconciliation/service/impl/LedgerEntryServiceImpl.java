package com.banking.ledger.reconciliation.service.impl;

import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.LedgerEntry;
import com.banking.ledger.reconciliation.domain.Money;
import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryRequest;
import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.exception.BusinessRuleException;
import com.banking.ledger.reconciliation.exception.ResourceNotFoundException;
import com.banking.ledger.reconciliation.repository.LedgerEntryRepository;
import com.banking.ledger.reconciliation.service.BankAccountService;
import com.banking.ledger.reconciliation.service.LedgerEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LedgerEntryServiceImpl implements LedgerEntryService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankAccountService bankAccountService;

    @Override
    @Transactional
    public LedgerEntryResponse create(Long accountId, LedgerEntryRequest request) {
        BankAccount account = bankAccountService.requireAccount(accountId);

        LedgerEntry entry = new LedgerEntry();
        entry.setBankAccount(account);
        entry.setEntryDate(request.entryDate());
        entry.setValueDate(request.valueDate());
        entry.setReference(request.reference());
        entry.setDescription(request.description());
        entry.setType(request.type());
        entry.setAmount(Money.of(request.amount()));
        entry.setExternalReference(request.externalReference());

        BigDecimal balanceAfter = Money.of(account.getCurrentBalance()
                .add(request.type().applySign(request.amount())));
        entry.setBalanceAfter(balanceAfter);

        LedgerEntry saved = ledgerEntryRepository.save(entry);
        account.setCurrentBalance(balanceAfter);
        return LedgerEntryResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryResponse> list(Long accountId, Pageable pageable) {
        bankAccountService.requireAccount(accountId);
        return PageResponse.from(
                ledgerEntryRepository.findByBankAccountId(accountId, pageable),
                LedgerEntryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerEntryResponse get(Long accountId, Long entryId) {
        return LedgerEntryResponse.from(requireEntry(accountId, entryId));
    }

    @Override
    @Transactional
    public void delete(Long accountId, Long entryId) {
        LedgerEntry entry = requireEntry(accountId, entryId);
        if (entry.isReconciled()) {
            throw new BusinessRuleException("Reconciled ledger entries cannot be deleted");
        }
        // Reverse the balance effect of the deleted entry.
        BankAccount account = entry.getBankAccount();
        account.setCurrentBalance(
                Money.of(account.getCurrentBalance().subtract(entry.getType().applySign(entry.getAmount()))));
        ledgerEntryRepository.delete(entry);
    }

    private LedgerEntry requireEntry(Long accountId, Long entryId) {
        return ledgerEntryRepository.findByIdAndBankAccountId(entryId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ledger entry " + entryId + " for account " + accountId));
    }
}