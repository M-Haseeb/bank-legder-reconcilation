package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import com.banking.ledger.reconciliation.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Applies Bucket4j limits to login and to state-changing API calls.
 * <p>
 * Registered as an MVC interceptor (not a servlet filter) so the security
 * context is already populated and authenticated writes can be keyed by user.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String API_PREFIX = "/api/";
    private static final String LOGIN_PATH = "/api/auth/login";

    private final RateLimitService rateLimitService;
    private final AppProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());

        AppProperties.RateLimit.Bucket limit;
        String bucketKey;

        if ("POST".equals(method) && LOGIN_PATH.equals(path)) {
            limit = properties.rateLimit().login();
            bucketKey = "login:" + request.getRemoteAddr();
        } else if (WRITE_METHODS.contains(method) && path.startsWith(API_PREFIX)) {
            String caller = CurrentUser.optionalUsername()
                    .map(username -> "user:" + username)
                    .orElseGet(() -> "ip:" + request.getRemoteAddr());
            limit = properties.rateLimit().write();
            bucketKey = "write:" + caller;
        } else {
            return true;
        }

        if (!rateLimitService.tryConsume(bucketKey, limit)) {
            throw new RateLimitExceededException(limit.period());
        }
        return true;
    }
}