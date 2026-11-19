package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryRequest;
import com.banking.ledger.reconciliation.dto.LedgerDtos.LedgerEntryResponse;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.service.LedgerEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/ledger")
@RequiredArgsConstructor
public class LedgerEntryController {

    private final LedgerEntryService ledgerEntryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<LedgerEntryResponse> create(
            @PathVariable Long accountId, @Valid @RequestBody LedgerEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ledgerEntryService.create(accountId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','AUDITOR')")
    public PageResponse<LedgerEntryResponse> list(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ledgerEntryService.list(accountId, BankAccountController.pageRequest(page, size, "entryDate"));
    }

    @GetMapping("/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT','AUDITOR')")
    public LedgerEntryResponse get(@PathVariable Long accountId, @PathVariable Long entryId) {
        return ledgerEntryService.get(accountId, entryId);
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTANT')")
    public ResponseEntity<Void> delete(@PathVariable Long accountId, @PathVariable Long entryId) {
        ledgerEntryService.delete(accountId, entryId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}