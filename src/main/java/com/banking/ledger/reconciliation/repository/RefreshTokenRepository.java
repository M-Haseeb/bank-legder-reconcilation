package com.banking.ledger.reconciliation.repository;

import com.banking.ledger.reconciliation.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);

    long deleteByExpiresAtBefore(Instant cutoff);
}