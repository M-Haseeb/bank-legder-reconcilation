package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.domain.enums.StatementStatus;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.dto.StatementDtos.StatementBulkRequest;
import com.banking.ledger.reconciliation.dto.StatementDtos.StatementEntryResponse;
import com.banking.ledger.reconciliation.service.BankStatementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/statements")
@RequiredArgsConstructor
public class BankStatementController {

    private final BankStatementService statementService;

    @PostMapping
    public ResponseEntity<List<StatementEntryResponse>> importEntries(
            @PathVariable Long accountId, @Valid @RequestBody StatementBulkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statementService.importEntries(accountId, request.entries()));
    }

    @GetMapping
    public PageResponse<StatementEntryResponse> list(
            @PathVariable Long accountId,
            @RequestParam(required = false) StatementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return statementService.list(
                accountId, status, BankAccountController.pageRequest(page, size, "statementDate"));
    }

    @GetMapping("/{entryId}")
    public StatementEntryResponse get(@PathVariable Long accountId, @PathVariable Long entryId) {
        return statementService.get(accountId, entryId);
    }

    @PostMapping("/{entryId}/exclude")
    public StatementEntryResponse exclude(@PathVariable Long accountId, @PathVariable Long entryId) {
        return statementService.exclude(accountId, entryId);
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable Long accountId, @PathVariable Long entryId) {
        statementService.delete(accountId, entryId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}