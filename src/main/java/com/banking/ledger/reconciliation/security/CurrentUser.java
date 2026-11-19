package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.domain.enums.Role;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Convenience accessors for the authenticated principal.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** @return the authenticated username, or {@code null} when the request is anonymous */
    public static String authenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    /** @return the actor used in audit records ({@code system} for background work) */
    public static String username() {
        String username = authenticatedUsername();
        return username == null ? "system" : username;
    }

    public static Optional<String> optionalUsername() {
        return Optional.ofNullable(authenticatedUsername());
    }

    public static boolean hasRole(Role role) {
        String username = authenticatedUsername();
        if (username == null) {
            return false;
        }
        String authority = "ROLE_" + role.name();
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static boolean isAdmin() {
        return hasRole(Role.ADMIN);
    }
}