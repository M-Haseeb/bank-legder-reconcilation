package com.banking.ledger.reconciliation.service;

import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryRequest;
import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Posting and querying internal ledger entries.
 */
public interface LedgerEntryService {

    LedgerEntryResponse create(Long accountId, LedgerEntryRequest request);

    PageResponse<LedgerEntryResponse> list(Long accountId, Pageable pageable);

    LedgerEntryResponse get(Long accountId, Long entryId);

    void delete(Long accountId, Long entryId);
}