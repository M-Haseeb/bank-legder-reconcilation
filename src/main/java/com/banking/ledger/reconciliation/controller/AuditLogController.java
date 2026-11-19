package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.domain.enums.AuditAction;
import com.banking.ledger.reconciliation.dto.AuditDtos.AuditLogResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Read-only access to the audit trail. Visible to administrators and auditors.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public PageResponse<AuditLogResponse> search(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        PageRequest pageable = PageRequest.of(
                safePage, safeSize, Sort.by(Sort.Direction.DESC, "occurredAt"));

        return auditService.search(
                action, actor, entityType, entityId, from, to, pageable);
    }
}