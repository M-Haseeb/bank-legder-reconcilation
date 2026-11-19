package com.banking.ledger.reconciliation.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION", message);
    }
}