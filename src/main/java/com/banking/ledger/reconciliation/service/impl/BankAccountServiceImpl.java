package com.banking.ledger.reconciliation.service.impl;

import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.Money;
import com.banking.ledger.reconciliation.domain.enums.AccountStatus;
import com.banking.ledger.reconciliation.dto.AccountDtos.AccountRequest;
import com.banking.ledger.reconciliation.dto.AccountDtos.AccountResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.exception.BusinessRuleException;
import com.banking.ledger.reconciliation.exception.DuplicateResourceException;
import com.banking.ledger.reconciliation.exception.ResourceNotFoundException;
import com.banking.ledger.reconciliation.repository.BankAccountRepository;
import com.banking.ledger.reconciliation.repository.BankStatementEntryRepository;
import com.banking.ledger.reconciliation.repository.LedgerEntryRepository;
import com.banking.ledger.reconciliation.repository.ReconciliationRunRepository;
import com.banking.ledger.reconciliation.service.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankStatementEntryRepository statementEntryRepository;
    private final ReconciliationRunRepository reconciliationRunRepository;

    @Override
    @Transactional
    public AccountResponse create(AccountRequest request) {
        String accountNumber = request.accountNumber().trim();
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateResourceException("Account number already exists: " + accountNumber);
        }

        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setAccountName(request.accountName().trim());
        account.setBankName(request.bankName().trim());
        account.setBranchName(request.branchName());
        account.setCurrency(request.currency().toUpperCase());
        account.setAccountType(request.accountType());
        account.setStatus(AccountStatus.ACTIVE);

        BigDecimal opening = request.openingBalance() == null
                ? Money.ZERO
                : Money.of(request.openingBalance());
        account.setOpeningBalance(opening);
        account.setCurrentBalance(opening);

        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    public PageResponse<AccountResponse> list(AccountStatus status, Pageable pageable) {
        var page = status == null
                ? accountRepository.findAll(pageable)
                : accountRepository.findByStatus(status, pageable);
        return PageResponse.from(page, AccountResponse::from);
    }

    @Override
    public AccountResponse get(Long id) {
        return AccountResponse.from(requireAccount(id));
    }

    @Override
    @Transactional
    public AccountResponse update(Long id, AccountRequest request) {
        BankAccount account = requireAccount(id);

        String accountNumber = request.accountNumber().trim();
        if (!account.getAccountNumber().equals(accountNumber)
                && accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateResourceException("Account number already exists: " + accountNumber);
        }

        account.setAccountNumber(accountNumber);
        account.setAccountName(request.accountName().trim());
        account.setBankName(request.bankName().trim());
        account.setBranchName(request.branchName());
        account.setCurrency(request.currency().toUpperCase());
        account.setAccountType(request.accountType());

        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse changeStatus(Long id, AccountStatus status) {
        BankAccount account = requireAccount(id);
        account.setStatus(status);
        return AccountResponse.from(accountRepository.save(account));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BankAccount account = requireAccount(id);
        boolean hasHistory = ledgerEntryRepository.existsByBankAccountId(id)
                || statementEntryRepository.existsByBankAccountId(id)
                || reconciliationRunRepository.existsByBankAccountId(id);
        if (hasHistory) {
            throw new BusinessRuleException(
                    "Account " + id + " has ledger, statement or reconciliation history; mark it INACTIVE instead of deleting");
        }
        accountRepository.delete(account);
    }

    @Override
    public BankAccount requireAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account", id));
    }
}