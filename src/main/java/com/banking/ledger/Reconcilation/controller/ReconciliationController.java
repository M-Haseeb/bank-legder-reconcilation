package com.banking.ledger.Reconcilation.controller;

import com.banking.ledger.Reconcilation.dto.PageResponse;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRequest;
import com.banking.ledger.Reconcilation.dto.ReconciliationDtos.ReconciliationRunResponse;
import com.banking.ledger.Reconcilation.service.ReconciliationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/reconciliations")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping
    public ResponseEntity<ReconciliationRunResponse> run(
            @PathVariable Long accountId, @Valid @RequestBody ReconciliationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reconciliationService.run(accountId, request));
    }

    @GetMapping
    public PageResponse<ReconciliationRunResponse> list(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reconciliationService.list(
                accountId, BankAccountController.pageRequest(page, size, "startedAt"));
    }

    @GetMapping("/{runId}")
    public ReconciliationRunResponse get(@PathVariable Long accountId, @PathVariable Long runId) {
        return reconciliationService.get(accountId, runId);
    }
}