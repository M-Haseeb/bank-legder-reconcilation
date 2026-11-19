package com.banking.ledger.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Size(max = 120) String username,
            @NotBlank @Size(max = 200) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 4096) String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String username,
            List<String> roles) {
    }

    public record UserInfo(String username, List<String> roles) {
    }
}