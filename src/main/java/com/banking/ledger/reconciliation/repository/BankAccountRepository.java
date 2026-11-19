package com.banking.ledger.reconciliation.repository;

import com.banking.ledger.reconciliation.domain.BankAccount;
import com.banking.ledger.reconciliation.domain.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    Page<BankAccount> findByStatus(AccountStatus status, Pageable pageable);
}