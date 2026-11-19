package com.banking.ledger.reconciliation.domain;

import com.banking.ledger.reconciliation.domain.enums.EntryType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void normalisesToTheCanonicalScale() {
        assertThat(Money.of(new BigDecimal("10")).scale()).isEqualTo(Money.SCALE);
        assertThat(Money.of(new BigDecimal("10.5"))).isEqualTo(new BigDecimal("10.5000"));
    }

    @Test
    void roundsHalfUpBeyondTheCanonicalScale() {
        assertThat(Money.of(new BigDecimal("10.12345"))).isEqualTo(new BigDecimal("10.1235"));
        assertThat(Money.of(new BigDecimal("10.12344"))).isEqualTo(new BigDecimal("10.1234"));
    }

    @Test
    void zeroUsesTheCanonicalScale() {
        assertThat(Money.ZERO.scale()).isEqualTo(Money.SCALE);
        assertThat(Money.ZERO).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void signApplicationKeepsTheCanonicalScale() {
        BigDecimal credit = EntryType.CREDIT.applySign(new BigDecimal("250"));

        assertThat(credit).isEqualByComparingTo("-250");
        assertThat(credit.scale()).isEqualTo(Money.SCALE);
        assertThat(EntryType.DEBIT.applySign(new BigDecimal("250"))).isEqualByComparingTo("250");
    }
}