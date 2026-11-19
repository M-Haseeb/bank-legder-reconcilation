package com.banking.ledger.reconciliation.config;

import com.banking.ledger.reconciliation.security.CurrentUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Fills {@code @CreatedBy} / {@code @LastModifiedBy} on entities with the
 * authenticated username, falling back to {@code system} for startup tasks.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditingConfig {

    @Bean
    AuditorAware<String> auditorAware() {
        return () -> Optional.of(CurrentUser.username());
    }
}