package com.banking.ledger.reconciliation.service;

import com.banking.ledger.reconciliation.dto.AuthDtos.LoginRequest;
import com.banking.ledger.reconciliation.dto.AuthDtos.TokenResponse;
import com.banking.ledger.reconciliation.dto.AuthDtos.UserInfo;

/**
 * Login and token lifecycle.
 */
public interface AuthService {

    TokenResponse login(LoginRequest request);

    /** Rotates a valid refresh token, revoking the old one. */
    TokenResponse refresh(String refreshToken);

    /** Revokes a refresh token. Idempotent. */
    void logout(String refreshToken);

    UserInfo me();
}