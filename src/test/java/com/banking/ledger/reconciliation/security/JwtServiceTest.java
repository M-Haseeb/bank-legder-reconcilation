package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import com.banking.ledger.reconciliation.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-please-change-0123456789abcdef";

    private final JwtService jwtService = new JwtService(new AppProperties(
            null,
            new AppProperties.Security(new AppProperties.Admin("admin", "secret"), List.of()),
            null,
            new AppProperties.Jwt(SECRET, "test-issuer", Duration.ofMinutes(15), Duration.ofDays(7)),
            null));

    @Test
    void accessTokenRoundTripsWithRoles() {
        JwtService.IssuedToken issued = jwtService.issueAccessToken(
                "accountant", List.of(Role.ACCOUNTANT.name()));

        Claims claims = jwtService.parse(issued.token());

        assertThat(claims.getSubject()).isEqualTo("accountant");
        assertThat(claims.get(JwtService.CLAIM_ROLES, List.class)).containsExactly("ACCOUNTANT");
        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.isRefreshToken(claims)).isFalse();
        assertThat(issued.tokenId()).isEqualTo(claims.getId());
    }

    @Test
    void refreshTokenIsTypedAsRefresh() {
        JwtService.IssuedToken issued = jwtService.issueRefreshToken("auditor");

        Claims claims = jwtService.parse(issued.token());

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isAccessToken(claims)).isFalse();
        assertThat(claims.get(JwtService.CLAIM_ROLES)).isNull();
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.issueAccessToken("admin", List.of("ADMIN")).token();
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtService other = new JwtService(new AppProperties(
                null,
                new AppProperties.Security(new AppProperties.Admin("admin", "secret"), List.of()),
                null,
                new AppProperties.Jwt(SECRET, "other-issuer", Duration.ofMinutes(15), Duration.ofDays(7)),
                null));

        String token = other.issueAccessToken("admin", List.of("ADMIN")).token();

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(JwtException.class);
    }
}