package com.banking.ledger.reconciliation.config;

import com.banking.ledger.reconciliation.domain.enums.Role;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Typed application settings bound from the {@code app.*} properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Seed seed,
        Security security,
        Cors cors,
        Jwt jwt,
        RateLimit rateLimit) {

    public AppProperties {
        seed = seed == null ? new Seed(false) : seed;
        security = security == null ? new Security(null, null) : security;
        cors = cors == null ? new Cors(null) : cors;
        jwt = jwt == null ? new Jwt(null, null, null, null) : jwt;
        rateLimit = rateLimit == null ? new RateLimit(null, null) : rateLimit;
    }

    public record Seed(boolean enabled) {
    }

    /**
     * Users are never shipped with defaults: at least one must be configured
     * through environment variables or {@code application-local.properties}.
     */
    public record Security(Admin admin, List<User> users) {

        public Security {
            admin = admin == null ? new Admin(null, null) : admin;

            List<User> configured = new ArrayList<>();
            if (users != null) {
                for (User user : users) {
                    if (user == null) {
                        continue;
                    }
                    boolean untouched = user.username() == null
                            && user.password() == null
                            && user.roles().isEmpty();
                    if (!untouched) {
                        configured.add(user);
                    }
                }
            }
            users = List.copyOf(configured);

            if (admin.username() == null && users.isEmpty()) {
                throw new IllegalStateException(
                        "No users are configured. Set APP_SECURITY_ADMIN_USERNAME / "
                                + "APP_SECURITY_ADMIN_PASSWORD (or APP_SECURITY_USERNAME / "
                                + "APP_SECURITY_PASSWORD), or define app.security.users[...] in "
                                + "application-local.properties.");
            }

            Set<String> usernames = new HashSet<>();
            if (admin.username() != null) {
                usernames.add(admin.username());
            }
            for (User user : users) {
                if (user.username() == null || user.password() == null) {
                    throw new IllegalStateException(
                            "Each app.security.users entry needs both a username and a password");
                }
                if (user.roles().isEmpty()) {
                    throw new IllegalStateException(
                            "User '" + user.username() + "' has no roles; assign one of ADMIN, ACCOUNTANT, AUDITOR");
                }
                if (!usernames.add(user.username())) {
                    throw new IllegalStateException("Duplicate user configured: " + user.username());
                }
            }
        }

        /** @return the primary admin (if configured) followed by the configured users */
        public List<User> allUsers() {
            List<User> all = new ArrayList<>();
            if (admin.username() != null) {
                all.add(new User(admin.username(), admin.password(), List.of(Role.ADMIN)));
            }
            all.addAll(users);
            return all;
        }
    }

    /** Primary administrator, kept separate for simple env-var configuration. */
    public record Admin(String username, String password) {

        public Admin {
            username = hasText(username) ? username.trim() : null;
            password = hasText(password) ? password : null;
            if ((username == null) != (password == null)) {
                throw new IllegalStateException(
                        "Set both APP_SECURITY_ADMIN_USERNAME and APP_SECURITY_ADMIN_PASSWORD, or neither");
            }
        }
    }

    public record User(String username, String password, List<Role> roles) {

        public User {
            username = hasText(username) ? username.trim() : null;
            password = hasText(password) ? password : null;
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }

    public record Jwt(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {

        public Jwt {
            if (!hasText(secret) || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "app.jwt.secret must be at least 32 bytes. Set APP_JWT_SECRET to a long random "
                                + "value (for example: openssl rand -base64 64).");
            }
            issuer = hasText(issuer) ? issuer : "bank-ledger";
            accessTokenTtl = positiveOr(accessTokenTtl, Duration.ofMinutes(15));
            refreshTokenTtl = positiveOr(refreshTokenTtl, Duration.ofDays(7));
            if (refreshTokenTtl.compareTo(accessTokenTtl) <= 0) {
                throw new IllegalStateException("app.jwt.refresh-token-ttl must exceed app.jwt.access-token-ttl");
            }
        }
    }

    public record RateLimit(Bucket login, Bucket write) {

        public RateLimit {
            login = login == null ? new Bucket(5, Duration.ofMinutes(1)) : login;
            write = write == null ? new Bucket(60, Duration.ofMinutes(1)) : write;
        }

        public record Bucket(int capacity, Duration period) {

            public Bucket {
                if (capacity <= 0) {
                    throw new IllegalStateException("Rate-limit capacity must be positive");
                }
                if (period == null || period.isZero() || period.isNegative()) {
                    throw new IllegalStateException("Rate-limit period must be positive");
                }
            }
        }
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = (allowedOrigins == null || allowedOrigins.isEmpty())
                    ? List.of("http://localhost:3000", "http://localhost:5173")
                    : List.copyOf(allowedOrigins);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Duration positiveOr(Duration value, Duration fallback) {
        if (value == null) {
            return fallback;
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalStateException("Token TTLs must be positive");
        }
        return value;
    }
}