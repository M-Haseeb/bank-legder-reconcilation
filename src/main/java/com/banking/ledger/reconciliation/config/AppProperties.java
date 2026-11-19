package com.banking.ledger.reconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed application settings bound from the {@code app.*} properties.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Seed seed, Security security, Cors cors) {

    public AppProperties {
        seed = seed == null ? new Seed(false) : seed;
        security = security == null ? new Security(null, null, null) : security;
        cors = cors == null ? new Cors(null) : cors;
    }

    public record Seed(boolean enabled) {
    }

    public record Security(String username, String password, List<String> roles) {

        public Security {
            if (username == null || username.isBlank()) {
                throw new IllegalStateException(
                        "app.security.username is not configured. Set APP_SECURITY_USERNAME "
                                + "(environment variable) or define app.security.username in "
                                + "application-local.properties.");
            }
            if (password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "app.security.password is not configured. Set APP_SECURITY_PASSWORD "
                                + "(environment variable) or define app.security.password in "
                                + "application-local.properties.");
            }
            roles = (roles == null || roles.isEmpty()) ? List.of("ADMIN", "USER") : List.copyOf(roles);
        }
    }

    public record Cors(List<String> allowedOrigins) {

        public Cors {
            allowedOrigins = (allowedOrigins == null || allowedOrigins.isEmpty())
                    ? List.of("http://localhost:3000", "http://localhost:5173")
                    : List.copyOf(allowedOrigins);
        }
    }
}