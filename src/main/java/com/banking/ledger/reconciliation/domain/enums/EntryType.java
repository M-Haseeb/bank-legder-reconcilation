package com.banking.ledger.reconciliation.domain.enums;

import com.banking.ledger.reconciliation.domain.Money;

import java.math.BigDecimal;

/**
 * Direction of a ledger or statement line.
 * <p>
 * The ledger uses the debit-positive convention: a {@link #DEBIT} increases the
 * account balance and a {@link #CREDIT} decreases it.
 */
public enum EntryType {
    DEBIT,
    CREDIT;

    /**
     * @return the amount with its balance effect applied (negative for credits)
     */
    public BigDecimal applySign(BigDecimal amount) {
        return Money.of(this == DEBIT ? amount : amount.negate());
    }
}