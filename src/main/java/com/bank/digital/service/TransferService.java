package com.bank.digital.service;

import com.bank.digital.dto.request.TransferRequest;
import com.bank.digital.dto.response.TransferResponse;

import java.util.List;

public interface TransferService {

    TransferResponse transfer(TransferRequest request);

    TransferResponse getTransactionByExternalId(String externalId);

    List<TransferResponse> getTransactionsByAccount(String accountNumber);

    List<TransferResponse> listAllTransactions();
}
