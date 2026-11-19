package com.banking.ledger.reconciliation.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The single source of truth for monetary values in the ledger.
 * <p>
 * Money is always a {@link BigDecimal} with a fixed scale of {@link #SCALE}
 * decimal places, rounded with {@link #ROUNDING_MODE}. Never use {@code double}
 * or {@code float} for money: their binary representation cannot represent
 * decimal fractions exactly.
 */
public final class Money {

    /** Decimal places used by every monetary column and calculation. */
    public static final int SCALE = 4;

    /** Rounding rule applied when an input carries more than {@link #SCALE} decimals. */
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    /** A zero amount at the canonical scale. */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);

    private Money() {
    }

    /**
     * Normalises an amount to the canonical money scale.
     *
     * @param amount the raw amount, may be {@code null}
     * @return the amount at {@link #SCALE} places, or {@code null} when given {@code null}
     */
    public static BigDecimal of(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, ROUNDING_MODE);
    }
}