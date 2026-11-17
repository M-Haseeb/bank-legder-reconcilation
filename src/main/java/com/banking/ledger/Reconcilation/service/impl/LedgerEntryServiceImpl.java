package com.banking.ledger.Reconcilation.service.impl;

import com.banking.ledger.Reconcilation.domain.BankAccount;
import com.banking.ledger.Reconcilation.domain.LedgerEntry;
import com.banking.ledger.Reconcilation.dto.LedgerDtos.LedgerEntryRequest;
import com.banking.ledger.Reconcilation.dto.LedgerDtos.LedgerEntryResponse;
import com.banking.ledger.Reconcilation.dto.PageResponse;
import com.banking.ledger.Reconcilation.exception.BusinessRuleException;
import com.banking.ledger.Reconcilation.exception.ResourceNotFoundException;
import com.banking.ledger.Reconcilation.repository.LedgerEntryRepository;
import com.banking.ledger.Reconcilation.service.BankAccountService;
import com.banking.ledger.Reconcilation.service.LedgerEntryService;
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
        entry.setAmount(request.amount().setScale(4));
        entry.setExternalReference(request.externalReference());

        BigDecimal balanceAfter = account.getCurrentBalance()
                .add(request.type().applySign(request.amount()))
                .setScale(4);
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
                account.getCurrentBalance().subtract(entry.getType().applySign(entry.getAmount())).setScale(4));
        ledgerEntryRepository.delete(entry);
    }

    private LedgerEntry requireEntry(Long accountId, Long entryId) {
        return ledgerEntryRepository.findByIdAndBankAccountId(entryId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ledger entry " + entryId + " for account " + accountId));
    }
}