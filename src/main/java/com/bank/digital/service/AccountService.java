package com.bank.digital.service;

import com.bank.digital.dto.request.CreateAccountRequest;
import com.bank.digital.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> listAllAccounts();
}
