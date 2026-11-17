package com.banking.ledger.Reconcilation.service;

import com.banking.ledger.Reconcilation.dto.PageResponse;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRequest;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRunResponse;
import org.springframework.data.domain.Pageable;

/**
 * Reconciles bank statements against the internal ledger.
 */
public interface ReconciliationService {

    /** Maximum allowed gap, in days, between a statement line and its ledger entry. */
    int DATE_TOLERANCE_DAYS = 3;

    ReconciliationRunResponse run(Long accountId, ReconciliationRequest request);

    PageResponse<ReconciliationRunResponse> list(Long accountId, Pageable pageable);

    ReconciliationRunResponse get(Long accountId, Long runId);
}