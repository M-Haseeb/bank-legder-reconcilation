package com.banking.ledger.reconciliation.config;

import com.banking.ledger.reconciliation.domain.enums.Role;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppPropertiesTest {

    private static final String VALID_SECRET = "test-only-jwt-secret-please-change-0123456789abcdef";

    @Test
    void requiresAtLeastOneUser() {
        assertThatThrownBy(() -> new AppProperties.Security(null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No users");
    }

    @Test
    void rejectsIncompleteUser() {
        AppProperties.User user = new AppProperties.User("accountant", null, List.of(Role.ACCOUNTANT));

        assertThatThrownBy(() -> new AppProperties.Security(null, List.of(user)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username and a password");
    }

    @Test
    void rejectsDuplicateUsernames() {
        AppProperties.User first = new AppProperties.User("dup", "pw1", List.of(Role.ACCOUNTANT));
        AppProperties.User second = new AppProperties.User("dup", "pw2", List.of(Role.AUDITOR));

        assertThatThrownBy(() -> new AppProperties.Security(null, List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate user");
    }

    @Test
    void primaryAdminReceivesTheAdminRole() {
        AppProperties.Security security = new AppProperties.Security(
                new AppProperties.Admin("root", "secret"), null);

        assertThat(security.allUsers()).singleElement().satisfies(user -> {
            assertThat(user.username()).isEqualTo("root");
            assertThat(user.roles()).containsExactly(Role.ADMIN);
        });
    }

    @Test
    void rejectsShortJwtSecret() {
        assertThatThrownBy(() -> new AppProperties.Jwt("too-short", null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void appliesJwtAndRateLimitDefaults() {
        AppProperties.Jwt jwt = new AppProperties.Jwt(VALID_SECRET, null, null, null);
        AppProperties.RateLimit limits = new AppProperties.RateLimit(null, null);

        assertThat(jwt.issuer()).isEqualTo("bank-ledger");
        assertThat(jwt.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(jwt.refreshTokenTtl()).isEqualTo(Duration.ofDays(7));
        assertThat(limits.login().capacity()).isEqualTo(5);
        assertThat(limits.write().capacity()).isEqualTo(60);
    }

    @Test
    void rejectsRefreshTokenTtlShorterThanAccessTokenTtl() {
        assertThatThrownBy(() -> new AppProperties.Jwt(
                VALID_SECRET, "issuer", Duration.ofDays(7), Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refresh-token-ttl");
    }
}