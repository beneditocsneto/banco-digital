package com.bank.digital.config;

import com.bank.digital.datasource.entity.Account;
import com.bank.digital.datasource.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            log.info("Dados já carregados. Pulando inicialização.");
            return;
        }

        List<Account> accounts = List.of(
                Account.builder()
                        .accountNumber("1001")
                        .holderName("João Silva")
                        .balance(new BigDecimal("5000.00"))
                        .build(),
                Account.builder()
                        .accountNumber("1002")
                        .holderName("Maria Santos")
                        .balance(new BigDecimal("10000.00"))
                        .build(),
                Account.builder()
                        .accountNumber("1003")
                        .holderName("Carlos Oliveira")
                        .balance(new BigDecimal("2500.00"))
                        .build(),
                Account.builder()
                        .accountNumber("1004")
                        .holderName("Ana Costa")
                        .balance(new BigDecimal("7500.00"))
                        .build()
        );

        accountRepository.saveAll(accounts);
        log.info("=== {} contas carregadas com sucesso ===", accounts.size());
        accounts.forEach(a -> log.info("Conta: {} | Titular: {} | Saldo: R$ {}",
                a.getAccountNumber(), a.getHolderName(), a.getBalance()));
    }
}
