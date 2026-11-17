package com.banking.ledger.Reconcilation.domain.enums;

/**
 * Reconciliation state of an imported bank-statement line.
 */
public enum StatementStatus {
    /** Imported but not yet reconciled. */
    UNMATCHED,
    /** Matched to exactly one ledger entry. */
    MATCHED,
    /** Reviewed and found to disagree with the ledger. */
    DISCREPANCY,
    /** Intentionally ignored during reconciliation. */
    EXCLUDED
}