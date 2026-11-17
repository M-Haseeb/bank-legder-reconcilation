package com.banking.ledger.Reconcilation.service;

import com.banking.ledger.Reconcilation.domain.enums.StatementStatus;
import com.banking.ledger.Reconcilation.dto.PageResponse;
import com.banking.ledger.Reconcilation.dto.StatementDtos.StatementEntryRequest;
import com.banking.ledger.Reconcilation.dto.StatementDtos.StatementEntryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Importing and managing bank-statement lines.
 */
public interface BankStatementService {

    List<StatementEntryResponse> importEntries(Long accountId, List<StatementEntryRequest> requests);

    PageResponse<StatementEntryResponse> list(Long accountId, StatementStatus status, Pageable pageable);

    StatementEntryResponse get(Long accountId, Long entryId);

    StatementEntryResponse exclude(Long accountId, Long entryId);

    void delete(Long accountId, Long entryId);
}