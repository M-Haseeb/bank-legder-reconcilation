package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import com.banking.ledger.reconciliation.domain.enums.Role;
import com.banking.ledger.reconciliation.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitInterceptorTest {

    @Test
    void loginIsRejectedOnceTheBucketIsExhausted() {
        AppProperties properties = new AppProperties(
                null,
                new AppProperties.Security(new AppProperties.Admin("admin", "secret"), List.of()),
                null,
                new AppProperties.Jwt(
                        "test-only-jwt-secret-please-change-0123456789abcdef",
                        "test", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.RateLimit(
                        new AppProperties.RateLimit.Bucket(2, Duration.ofMinutes(1)),
                        new AppProperties.RateLimit.Bucket(100, Duration.ofMinutes(1))));

        RateLimitInterceptor interceptor =
                new RateLimitInterceptor(new RateLimitService(), properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(loginRequest("203.0.113.7"), response, new Object())).isTrue();
        assertThat(interceptor.preHandle(loginRequest("203.0.113.7"), response, new Object())).isTrue();
        assertThatThrownBy(() -> interceptor.preHandle(loginRequest("203.0.113.7"), response, new Object()))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void readRequestsAreNotRateLimited() {
        AppProperties properties = new AppProperties(
                null,
                new AppProperties.Security(new AppProperties.Admin("admin", "secret"), List.of()),
                null,
                new AppProperties.Jwt(
                        "test-only-jwt-secret-please-change-0123456789abcdef",
                        "test", Duration.ofMinutes(15), Duration.ofDays(7)),
                new AppProperties.RateLimit(
                        new AppProperties.RateLimit.Bucket(1, Duration.ofMinutes(1)),
                        new AppProperties.RateLimit.Bucket(1, Duration.ofMinutes(1))));

        RateLimitInterceptor interceptor =
                new RateLimitInterceptor(new RateLimitService(), properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/accounts");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
    }

    private static MockHttpServletRequest loginRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}