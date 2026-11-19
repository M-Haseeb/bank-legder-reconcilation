package com.banking.ledger.reconciliation.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPropertiesTest {

    @Test
    void rejectsMissingUsername() {
        assertThatThrownBy(() -> new AppProperties.Security(null, "secret", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_SECURITY_USERNAME");
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> new AppProperties.Security("ledger", "  ", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_SECURITY_PASSWORD");
    }

    @Test
    void acceptsConfiguredCredentialsAndDefaultsRoles() {
        AppProperties.Security security = new AppProperties.Security("ledger", "secret", null);

        assertThat(security.roles()).containsExactly("ADMIN", "USER");
    }
}