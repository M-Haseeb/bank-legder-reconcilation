package com.banking.ledger.reconciliation.exception;

import org.springframework.http.HttpStatus;

import java.time.Duration;

public class RateLimitExceededException extends ApiException {

    public RateLimitExceededException(Duration retryAfter) {
        super(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                "Too many requests. Retry after " + Math.max(retryAfter.toSeconds(), 1) + " seconds.");
    }
}