package com.banking.ledger.reconciliation.dto;

import com.banking.ledger.reconciliation.domain.AuditLog;
import com.banking.ledger.reconciliation.domain.enums.AuditAction;

import java.time.Instant;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            Instant occurredAt,
            String actor,
            AuditAction action,
            String entityType,
            String entityId,
            String details,
            String ipAddress) {

        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(),
                    log.getOccurredAt(),
                    log.getActor(),
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getDetails(),
                    log.getIpAddress());
        }
    }
}