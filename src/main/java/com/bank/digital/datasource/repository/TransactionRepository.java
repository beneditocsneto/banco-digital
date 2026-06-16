package com.bank.digital.datasource.repository;

import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByExternalId(String externalId);

    List<Transaction> findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(Account source, Account target);

    List<Transaction> findAllByOrderByCreatedAtDesc();
}
