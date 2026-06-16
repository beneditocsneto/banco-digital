package com.bank.digital.service;

import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.entity.Transaction;
import com.bank.digital.datasource.repository.AccountRepository;
import com.bank.digital.datasource.repository.TransactionRepository;
import com.bank.digital.dto.request.TransferRequest;
import com.bank.digital.dto.response.TransferResponse;
import com.bank.digital.exception.AccountNotFoundException;
import com.bank.digital.exception.DuplicateTransactionException;
import com.bank.digital.exception.InsufficientBalanceException;
import com.bank.digital.service.impl.TransferObservable;
import com.bank.digital.service.impl.TransferServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferObservable transferObservable;

    @InjectMocks
    private TransferServiceImpl transferService;

    @Captor
    private ArgumentCaptor<Account> accountCaptor;

    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;

    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        sourceAccount = Account.builder()
                .id(1L)
                .accountNumber("1001")
                .holderName("João Silva")
                .balance(new BigDecimal("5000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        targetAccount = Account.builder()
                .id(2L)
                .accountNumber("1002")
                .holderName("Maria Santos")
                .balance(new BigDecimal("3000.00"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Testes de Transferência")
    class TransferTests {

        @Test
        @DisplayName("Deve transferir valor com sucesso")
        void shouldTransferSuccessfully() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("500.00"))
                    .currency("BRL")
                    .externalId("ext-123")
                    .build();

            when(transactionRepository.findByExternalId("ext-123")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(targetAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction t = i.getArgument(0);
                t.setId(10L);
                t.setCreatedAt(LocalDateTime.now());
                t.setCompletedAt(LocalDateTime.now());
                return t;
            });

            TransferResponse response = transferService.transfer(request);

            assertNotNull(response);
            assertEquals("ext-123", response.getExternalId());
            assertEquals(new BigDecimal("500.00"), response.getAmount());
            assertEquals("COMPLETED", response.getStatus());

            assertEquals(new BigDecimal("4500.00"), sourceAccount.getBalance());
            assertEquals(new BigDecimal("3500.00"), targetAccount.getBalance());

            verify(transferObservable, times(1)).notify(
                    new TransferEvent(sourceAccount, targetAccount, new BigDecimal("500.00"), "BRL"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta de origem não existe")
        void shouldThrowWhenSourceAccountNotFound() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("inexistente")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("100.00"))
                    .currency("BRL")
                    .externalId("ext-456")
                    .build();

            when(transactionRepository.findByExternalId("ext-456")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("inexistente")).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> transferService.transfer(request));
        }

        @Test
        @DisplayName("Deve lançar exceção quando conta de destino não existe")
        void shouldThrowWhenTargetAccountNotFound() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("inexistente")
                    .amount(new BigDecimal("100.00"))
                    .currency("BRL")
                    .externalId("ext-789")
                    .build();

            when(transactionRepository.findByExternalId("ext-789")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber("inexistente")).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () -> transferService.transfer(request));
        }

        @Test
        @DisplayName("Deve lançar exceção quando saldo é insuficiente")
        void shouldThrowWhenInsufficientBalance() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("10000.00"))
                    .currency("BRL")
                    .externalId("ext-overdraft")
                    .build();

            when(transactionRepository.findByExternalId("ext-overdraft")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(targetAccount));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

            assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(request));
        }

        @Test
        @DisplayName("Deve lançar exceção para transação duplicada")
        void shouldThrowWhenDuplicateTransaction() {
            Transaction existing = Transaction.builder()
                    .id(5L)
                    .externalId("ext-duplicate")
                    .sourceAccount(sourceAccount)
                    .targetAccount(targetAccount)
                    .amount(new BigDecimal("200.00"))
                    .currency("BRL")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .build();

            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("200.00"))
                    .currency("BRL")
                    .externalId("ext-duplicate")
                    .build();

            when(transactionRepository.findByExternalId("ext-duplicate")).thenReturn(Optional.of(existing));

            assertThrows(DuplicateTransactionException.class, () -> transferService.transfer(request));
        }

        @Test
        @DisplayName("Deve lançar exceção quando origem e destino são iguais")
        void shouldThrowWhenSameAccount() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1001")
                    .amount(new BigDecimal("100.00"))
                    .currency("BRL")
                    .externalId("ext-same")
                    .build();

            when(transactionRepository.findByExternalId("ext-same")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001"))
                    .thenReturn(Optional.of(sourceAccount))
                    .thenReturn(Optional.of(sourceAccount));

            assertThrows(IllegalArgumentException.class, () -> transferService.transfer(request));
        }

        @Test
        @DisplayName("Deve transferir valor mínimo (1 centavo)")
        void shouldTransferMinimumAmount() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("0.01"))
                    .currency("BRL")
                    .externalId("ext-min")
                    .build();

            when(transactionRepository.findByExternalId("ext-min")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(targetAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction t = i.getArgument(0);
                t.setId(11L);
                return t;
            });

            TransferResponse response = transferService.transfer(request);

            assertEquals("COMPLETED", response.getStatus());
            assertEquals(new BigDecimal("4999.99"), sourceAccount.getBalance());
            assertEquals(new BigDecimal("3000.01"), targetAccount.getBalance());
        }

        @Test
        @DisplayName("Deve transferir valor total da conta")
        void shouldTransferFullBalance() {
            TransferRequest request = TransferRequest.builder()
                    .sourceAccountNumber("1001")
                    .targetAccountNumber("1002")
                    .amount(new BigDecimal("5000.00"))
                    .currency("BRL")
                    .externalId("ext-full")
                    .build();

            when(transactionRepository.findByExternalId("ext-full")).thenReturn(Optional.empty());
            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByAccountNumber("1002")).thenReturn(Optional.of(targetAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
                Transaction t = i.getArgument(0);
                t.setId(12L);
                return t;
            });

            TransferResponse response = transferService.transfer(request);

            assertEquals("COMPLETED", response.getStatus());
            assertEquals(BigDecimal.ZERO.compareTo(sourceAccount.getBalance()), 0);
            assertEquals(new BigDecimal("8000.00"), targetAccount.getBalance());
        }
    }

    @Nested
    @DisplayName("Testes de Consulta")
    class QueryTests {

        @Test
        @DisplayName("Deve buscar transações por conta")
        void shouldGetTransactionsByAccount() {
            Transaction tx1 = Transaction.builder()
                    .id(1L)
                    .externalId("ext-1")
                    .sourceAccount(sourceAccount)
                    .targetAccount(targetAccount)
                    .amount(new BigDecimal("200.00"))
                    .currency("BRL")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(accountRepository.findByAccountNumber("1001")).thenReturn(Optional.of(sourceAccount));
            when(transactionRepository.findBySourceAccountOrTargetAccountOrderByCreatedAtDesc(
                    sourceAccount, sourceAccount)).thenReturn(List.of(tx1));

            List<TransferResponse> transactions = transferService.getTransactionsByAccount("1001");

            assertFalse(transactions.isEmpty());
            assertEquals(1, transactions.size());
            assertEquals("ext-1", transactions.get(0).getExternalId());
        }

        @Test
        @DisplayName("Deve buscar transação por externalId")
        void shouldGetTransactionByExternalId() {
            Transaction tx = Transaction.builder()
                    .id(1L)
                    .externalId("ext-999")
                    .sourceAccount(sourceAccount)
                    .targetAccount(targetAccount)
                    .amount(new BigDecimal("150.00"))
                    .currency("BRL")
                    .status(Transaction.TransactionStatus.COMPLETED)
                    .createdAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .build();

            when(transactionRepository.findByExternalId("ext-999")).thenReturn(Optional.of(tx));

            TransferResponse response = transferService.getTransactionByExternalId("ext-999");

            assertEquals("ext-999", response.getExternalId());
            assertEquals(new BigDecimal("150.00"), response.getAmount());
        }
    }
}
