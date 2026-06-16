package com.bank.digital.service;

import com.bank.digital.dto.request.CreateAccountRequest;
import com.bank.digital.dto.response.AccountResponse;
import com.bank.digital.exception.AccountNotFoundException;
import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.repository.AccountRepository;
import com.bank.digital.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account sourceAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1001")
                .holderName("João Silva")
                .balance(new BigDecimal("5000.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Testes de Criação de Conta")
    class CreateAccountTests {

        @Test
        @DisplayName("Deve criar conta com sucesso")
        void shouldCreateAccount() {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .accountNumber("2001")
                    .holderName("Novo Cliente")
                    .initialBalance(new BigDecimal("1000.00"))
                    .build();

            when(accountRepository.findByAccountNumber("2001")).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
                Account a = i.getArgument(0);
                a.setId(10L);
                a.setCreatedAt(LocalDateTime.now());
                return a;
            });

            AccountResponse response = accountService.createAccount(request);

            assertNotNull(response);
            assertEquals("2001", response.getAccountNumber());
            assertEquals("Novo Cliente", response.getHolderName());
            assertEquals(new BigDecimal("1000.00"), response.getBalance());
        }

        @Test
        @DisplayName("Deve criar conta com saldo zero quando não informado")
        void shouldCreateAccountWithZeroBalanceWhenNotInformed() {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .accountNumber("2002")
                    .holderName("Cliente sem saldo")
                    .build();

            when(accountRepository.findByAccountNumber("2002")).thenReturn(Optional.empty());
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> {
                Account a = i.getArgument(0);
                a.setId(11L);
                return a;
            });

            AccountResponse response = accountService.createAccount(request);

            assertEquals(BigDecimal.ZERO, response.getBalance());
        }

        @Test
        @DisplayName("Deve lançar exceção ao criar conta com número duplicado")
        void shouldThrowWhenAccountNumberExists() {
            CreateAccountRequest request = CreateAccountRequest.builder()
                    .accountNumber("1001")
                    .holderName("Duplicado")
                    .initialBalance(BigDecimal.TEN)
                    .build();

            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));

            assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(request));
        }
    }

    @Nested
    @DisplayName("Testes de Consulta de Conta")
    class QueryAccountTests {

        @Test
        @DisplayName("Deve listar todas as contas")
        void shouldListAllAccounts() {
            Account account2 = Account.builder()
                    .id(2L)
                    .accountNumber("1002")
                    .holderName("Maria Santos")
                    .balance(new BigDecimal("3000.00"))
                    .build();

            when(accountRepository.findAll()).thenReturn(List.of(sourceAccount, account2));

            List<AccountResponse> accounts = accountService.listAllAccounts();

            assertEquals(2, accounts.size());
            assertEquals("1001", accounts.get(0).getAccountNumber());
            assertEquals("1002", accounts.get(1).getAccountNumber());
        }

        @Test
        @DisplayName("Deve buscar conta por número")
        void shouldGetAccountByNumber() {
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));

            AccountResponse response = accountService.getAccountByNumber("1001");

            assertNotNull(response);
            assertEquals("1001", response.getAccountNumber());
            assertEquals("João Silva", response.getHolderName());
            assertEquals(new BigDecimal("5000.00"), response.getBalance());
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar conta inexistente")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepository.findByAccountNumber("9999")).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class,
                    () -> accountService.getAccountByNumber("9999"));
        }
    }
}
