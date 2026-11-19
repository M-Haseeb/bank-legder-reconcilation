package com.banking.ledger.reconciliation.security;

import com.banking.ledger.reconciliation.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues and verifies HS256 JSON Web Tokens. Access tokens are short-lived and
 * carry the user's roles; refresh tokens are long-lived, carry a unique id and
 * are additionally tracked in the database so they can be rotated and revoked.
 */
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_ROLES = "roles";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final AppProperties.Jwt properties;

    public JwtService(AppProperties appProperties) {
        this.properties = appProperties.jwt();
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issueAccessToken(String username, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(tokenId)
                .issuer(properties.issuer())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .signWith(key)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public IssuedToken issueRefreshToken(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.refreshTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(tokenId)
                .issuer(properties.issuer())
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .signWith(key)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    /**
     * @throws io.jsonwebtoken.JwtException if the signature, issuer or expiry is invalid
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public long accessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public record IssuedToken(String token, String tokenId, Instant expiresAt) {
    }
}