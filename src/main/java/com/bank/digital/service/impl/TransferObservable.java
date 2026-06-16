package com.bank.digital.service.impl;

import com.bank.digital.service.TransferEvent;
import com.bank.digital.service.TransferObserver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TransferObservable {

    private final List<TransferObserver> observers = new ArrayList<>();

    public void register(TransferObserver observer) {
        observers.add(observer);
    }

    public void notify(TransferEvent event) {
        observers.forEach(obs -> obs.onTransferCompleted(event));
    }
}
