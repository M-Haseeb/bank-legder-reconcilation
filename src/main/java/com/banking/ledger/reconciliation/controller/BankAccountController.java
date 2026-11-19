package com.banking.ledger.reconciliation.controller;

import com.banking.ledger.reconciliation.domain.enums.AccountStatus;
import com.banking.ledger.reconciliation.dto.AccountDtos.AccountRequest;
import com.banking.ledger.reconciliation.dto.AccountDtos.AccountResponse;
import com.banking.ledger.reconciliation.dto.AccountDtos.AccountStatusRequest;
import com.banking.ledger.reconciliation.dto.PageResponse;
import com.banking.ledger.reconciliation.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody AccountRequest request) {
        AccountResponse created = accountService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<AccountResponse> list(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return accountService.list(status, pageRequest(page, size, "id"));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accountService.get(id);
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return accountService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse changeStatus(
            @PathVariable Long id, @Valid @RequestBody AccountStatusRequest request) {
        return accountService.changeStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    static PageRequest pageRequest(int page, int size, String sortProperty) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortProperty));
    }
}