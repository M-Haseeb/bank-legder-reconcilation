package com.banking.ledger.reconciliation.service.impl;

import com.banking.ledger.reconciliation.domain.AuditLog;
import com.banking.ledger.reconciliation.domain.enums.AuditAction;
import com.banking.ledger.reconciliation.dto.AuditDtos.AuditLogResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.repository.AuditLogRepository;
import com.banking.ledger.reconciliation.security.CurrentUser;
import com.banking.ledger.reconciliation.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int MAX_DETAILS_LENGTH = 2000;

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, String entityType, Object entityId, String details) {
        write(CurrentUser.username(), action, entityType, entityId, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAs(String actor, AuditAction action, String entityType, Object entityId, String details) {
        write(actor, action, entityType, entityId, details);
    }

    private void write(String actor, AuditAction action, String entityType, Object entityId, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor == null || actor.isBlank() ? "system" : actor);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId == null ? null : String.valueOf(entityId));
        log.setDetails(truncate(details));
        log.setIpAddress(currentIpAddress());
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(
            AuditAction action,
            String actor,
            String entityType,
            String entityId,
            Instant from,
            Instant to,
            Pageable pageable) {

        Specification<AuditLog> specification = (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }
            if (hasText(actor)) {
                predicates.add(builder.equal(builder.lower(root.get("actor")), actor.toLowerCase()));
            }
            if (hasText(entityType)) {
                predicates.add(builder.equal(root.get("entityType"), entityType));
            }
            if (hasText(entityId)) {
                predicates.add(builder.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };

        return PageResponse.from(
                auditLogRepository.findAll(specification, pageable),
                AuditLogResponse::from);
    }

    private static String currentIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getRemoteAddr();
        }
        return null;
    }

    private static String truncate(String details) {
        if (details == null) {
            return null;
        }
        return details.length() <= MAX_DETAILS_LENGTH ? details : details.substring(0, MAX_DETAILS_LENGTH);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}