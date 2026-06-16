package com.bank.digital.service.impl;

import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.entity.Transaction;
import com.bank.digital.datasource.repository.AccountRepository;
import com.bank.digital.datasource.repository.TransactionRepository;
import com.bank.digital.dto.request.TransferRequest;
import com.bank.digital.dto.response.TransferResponse;
import com.bank.digital.exception.AccountNotFoundException;
import com.bank.digital.exception.DuplicateTransactionException;
import com.bank.digital.exception.InsufficientBalanceException;
import com.bank.digital.exception.TransactionNotFoundException;
import com.bank.digital.service.TransferService;
import com.bank.digital.service.TransferEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferObservable transferObservable;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransferResponse transfer(TransferRequest request) {
        log.info("Iniciando transferência: externalId={}", request.getExternalId());

        Transaction retryTarget = findAndValidateExistingTransaction(request.getExternalId());
        if (retryTarget != null) {
            return retryFailedTransfer(retryTarget, request);
        }

        Account source = getAccount(request.getSourceAccountNumber());
        Account target = getAccount(request.getTargetAccountNumber());
        validateDifferentAccounts(source, target);

        return completeNewTransfer(source, target, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getTransactionsByAccount(String accountNumber) {
        Account account = getAccount(accountNumber);
        return transactionRepository
                .findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(account, account)
                .stream()
                .map(this::toTransferResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TransferResponse getTransactionByExternalId(String externalId) {
        Transaction transaction = transactionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada: " + externalId));
        return toTransferResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> listAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toTransferResponse)
                .toList();
    }

    private Transaction findAndValidateExistingTransaction(String externalId) {
        var existing = transactionRepository.findByExternalId(externalId);
        if (existing.isEmpty()) {
            return null;
        }
        Transaction tx = existing.get();
        if (tx.getStatus() == Transaction.TransactionStatus.COMPLETED) {
            log.warn("Transação duplicada detectada: externalId={}", externalId);
            throw new DuplicateTransactionException(
                    "Transferência já processada com externalId: " + externalId);
        }
        return tx;
    }

    private TransferResponse completeNewTransfer(Account source, Account target, TransferRequest request) {
        if (!hasSufficientBalance(source, request.getAmount())) {
            buildAndSaveTransaction(source, target, request,
                    Transaction.TransactionStatus.FAILED, "Saldo insuficiente");
            throw insufficientBalance(source, request.getAmount());
        }

        applyTransfer(source, target, request.getAmount());

        Transaction transaction = buildAndSaveTransaction(source, target, request,
                Transaction.TransactionStatus.COMPLETED, null);
        markCompleted(transaction);

        log.info("Transferência concluída com sucesso: externalId={}, valor={} {}",
                request.getExternalId(), request.getAmount(), request.getCurrency());

        notifyObservers(source, target, request);
        return toTransferResponse(transaction);
    }

    private TransferResponse retryFailedTransfer(Transaction existing, TransferRequest request) {
        Account source = getAccount(request.getSourceAccountNumber());
        Account target = getAccount(request.getTargetAccountNumber());

        if (!hasSufficientBalance(source, request.getAmount())) {
            throw insufficientBalance(source, request.getAmount());
        }

        applyTransfer(source, target, request.getAmount());

        existing.setStatus(Transaction.TransactionStatus.COMPLETED);
        existing.setFailureReason(null);
        markCompleted(existing);

        notifyObservers(source, target, request);

        return toTransferResponse(existing);
    }

    private void notifyObservers(Account source, Account target, TransferRequest request) {
        transferObservable.notify(
                new TransferEvent(source, target, request.getAmount(), request.getCurrency()));
    }

    private Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Conta não encontrada: " + accountNumber));
    }

    private void validateDifferentAccounts(Account source, Account target) {
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Conta de origem e destino devem ser diferentes");
        }
    }

    private boolean hasSufficientBalance(Account account, BigDecimal amount) {
        return account.getBalance().compareTo(amount) >= 0;
    }

    private InsufficientBalanceException insufficientBalance(Account source, BigDecimal amount) {
        return new InsufficientBalanceException(
                String.format("Saldo insuficiente. Saldo atual: %.2f, Valor solicitado: %.2f",
                        source.getBalance(), amount));
    }

    private void applyTransfer(Account source, Account target, BigDecimal amount) {
        debit(source, amount);
        credit(target, amount);
        accountRepository.save(source);
        accountRepository.save(target);
    }

    private void markCompleted(Transaction transaction) {
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    private void debit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
    }

    private void credit(Account account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

    private Transaction buildAndSaveTransaction(Account source, Account target, TransferRequest request,
                                                 Transaction.TransactionStatus status, String failureReason) {
        Transaction transaction = Transaction.builder()
                .externalId(request.getExternalId())
                .sourceAccount(source)
                .targetAccount(target)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(status)
                .failureReason(failureReason)
                .build();
        return transactionRepository.save(transaction);
    }

    private TransferResponse toTransferResponse(Transaction transaction) {
        return TransferResponse.builder()
                .id(transaction.getId())
                .externalId(transaction.getExternalId())
                .sourceAccountNumber(transaction.getSourceAccount().getAccountNumber())
                .targetAccountNumber(transaction.getTargetAccount().getAccountNumber())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .completedAt(transaction.getCompletedAt())
                .build();
    }
}
