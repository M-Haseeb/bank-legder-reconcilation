package com.banking.ledger.Reconcilation.service;

import com.banking.ledger.Reconcilation.domain.BankAccount;
import com.banking.ledger.Reconcilation.domain.enums.AccountStatus;
import com.banking.ledger.Reconcilation.dto.AccountDtos.AccountRequest;
import com.banking.ledger.Reconcilation.dto.AccountDtos.AccountResponse;
import com.banking.ledger.Reconcilation.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Account lifecycle operations.
 */
public interface BankAccountService {

    AccountResponse create(AccountRequest request);

    PageResponse<AccountResponse> list(AccountStatus status, Pageable pageable);

    AccountResponse get(Long id);

    AccountResponse update(Long id, AccountRequest request);

    AccountResponse changeStatus(Long id, AccountStatus status);

    void delete(Long id);

    /**
     * Resolves an account or throws {@code ResourceNotFoundException}. Shared by
     * the ledger, statement and reconciliation services.
     */
    BankAccount requireAccount(Long id);
}