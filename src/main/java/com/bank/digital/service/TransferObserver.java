package com.bank.digital.service;

public interface TransferObserver {

    void onTransferCompleted(TransferEvent event);
}
