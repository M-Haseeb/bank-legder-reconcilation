package com.banking.ledger.Reconcilation.dto;

import com.banking.ledger.Reconcilation.domain.BankAccount;
import com.banking.ledger.Reconcilation.domain.enums.AccountStatus;
import com.banking.ledger.Reconcilation.domain.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountRequest(
            @NotBlank @Size(max = 34) String accountNumber,
            @NotBlank @Size(max = 120) String accountName,
            @NotBlank @Size(max = 120) String bankName,
            @Size(max = 120) String branchName,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotNull AccountType accountType,
            @DecimalMin(value = "0.00") BigDecimal openingBalance) {
    }

    public record AccountResponse(
            Long id,
            String accountNumber,
            String accountName,
            String bankName,
            String branchName,
            String currency,
            AccountType accountType,
            AccountStatus status,
            BigDecimal openingBalance,
            BigDecimal currentBalance,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {

        public static AccountResponse from(BankAccount account) {
            return new AccountResponse(
                    account.getId(),
                    account.getAccountNumber(),
                    account.getAccountName(),
                    account.getBankName(),
                    account.getBranchName(),
                    account.getCurrency(),
                    account.getAccountType(),
                    account.getStatus(),
                    account.getOpeningBalance(),
                    account.getCurrentBalance(),
                    account.getCreatedAt(),
                    account.getUpdatedAt());
        }
    }

    public record AccountStatusRequest(@NotNull AccountStatus status) {
    }
}