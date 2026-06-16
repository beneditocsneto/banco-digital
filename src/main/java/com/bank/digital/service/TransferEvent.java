package com.bank.digital.service;

import com.bank.digital.datasource.entity.Account;
import java.math.BigDecimal;

public record TransferEvent(Account source, Account target, BigDecimal amount, String currency) {}
