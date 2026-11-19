package com.banking.ledger.reconciliation.service;

import com.banking.ledger.reconciliation.domain.enums.AuditAction;
import com.banking.ledger.reconciliation.dto.AuditDtos.AuditLogResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Writes and queries the append-only audit trail.
 */
public interface AuditService {

    /** Records an action performed by the current authenticated principal. */
    void record(AuditAction action, String entityType, Object entityId, String details);

    /** Records an action by an explicit actor (used for login attempts). */
    void recordAs(String actor, AuditAction action, String entityType, Object entityId, String details);

    PageResponse<AuditLogResponse> search(
            AuditAction action,
            String actor,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            Pageable pageable);
}