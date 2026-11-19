package com.banking.ledger.reconciliation.domain.enums;

/**
 * Application roles. {@link #ADMIN} manages users and destructive operations,
 * {@link #ACCOUNTANT} performs ledger and statement work, and {@link #AUDITOR}
 * is strictly read-only.
 */
public enum Role {
    ADMIN,
    ACCOUNTANT,
    AUDITOR
}