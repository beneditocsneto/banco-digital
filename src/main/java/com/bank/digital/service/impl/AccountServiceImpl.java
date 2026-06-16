package com.bank.digital.service.impl;

import com.bank.digital.dto.request.CreateAccountRequest;
import com.bank.digital.dto.response.AccountResponse;
import com.bank.digital.exception.AccountNotFoundException;
import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.repository.AccountRepository;
import com.bank.digital.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.findByAccountNumber(request.getAccountNumber()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta com o número: " + request.getAccountNumber());
        }

        Account account = Account.builder()
                .accountNumber(request.getAccountNumber())
                .holderName(request.getHolderName())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .build();

        accountRepository.save(account);
        log.info("Conta criada: número={}, titular={}", account.getAccountNumber(), account.getHolderName());

        return toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Conta não encontrada: " + accountNumber));
        return toAccountResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> listAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toAccountResponse)
                .toList();
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .holderName(account.getHolderName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
