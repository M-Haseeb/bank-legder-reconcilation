package com.banking.ledger.reconciliation.domain.enums;

/**
 * Business actions recorded in the append-only audit trail.
 */
public enum AuditAction {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    TOKEN_REFRESHED,
    TOKEN_REFRESH_FAILED,
    LOGOUT,
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,
    ACCOUNT_STATUS_CHANGED,
    ACCOUNT_DELETED,
    LEDGER_ENTRY_CREATED,
    LEDGER_ENTRY_DELETED,
    STATEMENTS_IMPORTED,
    STATEMENT_EXCLUDED,
    STATEMENT_DELETED,
    RECONCILIATION_RUN
}