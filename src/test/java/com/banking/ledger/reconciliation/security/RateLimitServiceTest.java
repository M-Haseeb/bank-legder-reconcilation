package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    private final RateLimitService rateLimitService = new RateLimitService();
    private final AppProperties.RateLimit.Bucket limit =
            new AppProperties.RateLimit.Bucket(2, Duration.ofMinutes(1));

    @Test
    void allowsRequestsUpToCapacityThenRejects() {
        assertThat(rateLimitService.tryConsume("client-a", limit)).isTrue();
        assertThat(rateLimitService.tryConsume("client-a", limit)).isTrue();
        assertThat(rateLimitService.tryConsume("client-a", limit)).isFalse();
    }

    @Test
    void bucketsAreIsolatedPerKey() {
        rateLimitService.tryConsume("client-a", limit);
        rateLimitService.tryConsume("client-a", limit);

        assertThat(rateLimitService.tryConsume("client-b", limit)).isTrue();
    }
}