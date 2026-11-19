package com.banking.ledger.reconciliation.service.impl;

import com.banking.ledger.reconciliation.domain.RefreshToken;
import com.banking.ledger.reconciliation.domain.enums.AuditAction;
import com.banking.ledger.reconciliation.dto.AuthDtos.LoginRequest;
import com.banking.ledger.reconciliation.dto.AuthDtos.TokenResponse;
import com.banking.ledger.reconciliation.dto.AuthDtos.UserInfo;
import com.banking.ledger.reconciliation.exception.InvalidCredentialsException;
import com.banking.ledger.reconciliation.exception.InvalidTokenException;
import com.banking.ledger.reconciliation.repository.RefreshTokenRepository;
import com.banking.ledger.reconciliation.security.CurrentUser;
import com.banking.ledger.reconciliation.security.JwtService;
import com.banking.ledger.reconciliation.service.AuditService;
import com.banking.ledger.reconciliation.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(), request.password()));
        } catch (AuthenticationException ex) {
            auditService.recordAs(request.username(), AuditAction.LOGIN_FAILED,
                    "User", request.username(), "Invalid credentials");
            throw new InvalidCredentialsException();
        }

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        auditService.recordAs(principal.getUsername(), AuditAction.LOGIN_SUCCESS,
                "User", principal.getUsername(), null);
        return issueTokens(principal);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            auditService.recordAs("anonymous", AuditAction.TOKEN_REFRESH_FAILED,
                    "User", null, "Malformed or expired refresh token");
            throw new InvalidTokenException("Invalid or expired refresh token");
        }

        if (!jwtService.isRefreshToken(claims) || claims.getId() == null) {
            auditService.recordAs(claims.getSubject(), AuditAction.TOKEN_REFRESH_FAILED,
                    "User", claims.getSubject(), "Token is not a refresh token");
            throw new InvalidTokenException("Not a refresh token");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenIdAndRevokedFalse(claims.getId())
                .orElseThrow(() -> {
                    auditService.recordAs(claims.getSubject(), AuditAction.TOKEN_REFRESH_FAILED,
                            "User", claims.getSubject(), "Refresh token is revoked or unknown");
                    return new InvalidTokenException("Refresh token is revoked or unknown");
                });

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            auditService.recordAs(stored.getUsername(), AuditAction.TOKEN_REFRESH_FAILED,
                    "User", stored.getUsername(), "Refresh token expired");
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (!stored.getUsername().equals(claims.getSubject())) {
            throw new InvalidTokenException("Refresh token does not match its subject");
        }

        // Rotate: the presented token can never be used again.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        UserDetails principal;
        try {
            principal = userDetailsService.loadUserByUsername(stored.getUsername());
        } catch (AuthenticationException ex) {
            throw new InvalidTokenException("User no longer exists");
        }

        auditService.recordAs(principal.getUsername(), AuditAction.TOKEN_REFRESHED,
                "User", principal.getUsername(), null);
        return issueTokens(principal);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        try {
            Claims claims = jwtService.parse(refreshToken);
            if (!jwtService.isRefreshToken(claims) || claims.getId() == null) {
                return;
            }
            String currentUser = CurrentUser.authenticatedUsername();
            if (currentUser != null && !currentUser.equals(claims.getSubject())) {
                throw new AccessDeniedException(
                        "Refresh token does not belong to the authenticated user");
            }
            refreshTokenRepository.findByTokenIdAndRevokedFalse(claims.getId()).ifPresent(stored -> {
                stored.setRevoked(true);
                refreshTokenRepository.save(stored);
                auditService.recordAs(stored.getUsername(), AuditAction.LOGOUT,
                        "User", stored.getUsername(), null);
            });
        } catch (JwtException | IllegalArgumentException ex) {
            // Logout is idempotent: an invalid token is treated as already logged out.
        }
    }

    @Override
    public UserInfo me() {
        String username = CurrentUser.authenticatedUsername();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (username == null || authentication == null) {
            throw new InvalidTokenException("Not authenticated");
        }
        return new UserInfo(username, rolesOf(authentication));
    }

    private TokenResponse issueTokens(UserDetails principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();

        JwtService.IssuedToken access = jwtService.issueAccessToken(principal.getUsername(), roles);
        JwtService.IssuedToken refresh = jwtService.issueRefreshToken(principal.getUsername());

        RefreshToken stored = new RefreshToken();
        stored.setTokenId(refresh.tokenId());
        stored.setUsername(principal.getUsername());
        stored.setExpiresAt(refresh.expiresAt());
        refreshTokenRepository.save(stored);

        return new TokenResponse(
                access.token(),
                refresh.token(),
                "Bearer",
                jwtService.accessTokenTtlSeconds(),
                principal.getUsername(),
                roles);
    }

    private static List<String> rolesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .sorted()
                .toList();
    }
}