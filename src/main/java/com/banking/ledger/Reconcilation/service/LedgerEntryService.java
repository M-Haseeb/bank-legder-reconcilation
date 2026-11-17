package com.banking.ledger.Reconcilation.service;

import com.banking.ledger.Reconcilation.dto.LedgerDtos.LedgerEntryRequest;
import com.banking.ledger.Reconcilation.dto.LedgerDtos.LedgerEntryResponse;
import com.banking.ledger.Reconcilation.dto.PageResponse;
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