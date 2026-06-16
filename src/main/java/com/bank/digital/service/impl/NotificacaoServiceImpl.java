package com.bank.digital.service.impl;

import com.bank.digital.service.TransferEvent;
import com.bank.digital.service.TransferObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoServiceImpl implements TransferObserver {

    private final TransferObservable transferObservable;

    @PostConstruct
    void init() {
        transferObservable.registerObserver(this);
    }

    @Async
    @Override
    public void onTransferCompleted(TransferEvent event) {
        try {
            Thread.sleep(200);//simula latência da notificação.

            log.info("=== NOTIFICAÇÃO DE TRANSFERÊNCIA ===");
            log.info("Origem: {} ({})", event.source().getHolderName(), event.source().getAccountNumber());
            log.info("Destino: {} ({})", event.target().getHolderName(), event.target().getAccountNumber());
            log.info("Valor: {}", formatCurrency(event.amount(), event.currency()));
            log.info("Status: CONFIRMADA");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Falha ao enviar notificação de transferência", e);
        }
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        return switch (currency.toUpperCase()) {
            case "BRL" -> {
                NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
                yield nf.format(amount);
            }
            case "USD" -> String.format("US$ %,.2f", amount);
            case "EUR" -> String.format("€ %,.2f", amount);
            default -> String.format("%s %,.2f", currency, amount);
        };
    }
}
