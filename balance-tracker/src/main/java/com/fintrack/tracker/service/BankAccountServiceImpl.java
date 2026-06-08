// balance-tracker/src/main/java/com/fintrack/tracker/service/BankAccountServiceImpl.java
package com.fintrack.tracker.service;

import com.fintrack.tracker.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    // AtomicReference<BigDecimal> for thread-safe, lock-free balance tracking
    private final AtomicReference<BigDecimal> balance = new AtomicReference<>(BigDecimal.ZERO);

    // Unbounded queue — holds transactions pending audit submission
    private final BlockingQueue<Transaction> auditQueue = new LinkedBlockingQueue<>();

    private static final int AUDIT_BATCH_SIZE = 1000;

    private final AuditService auditService;

    @Autowired
    public BankAccountServiceImpl(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void processTransaction(Transaction transaction) {
        // Thread-safe balance update using CAS loop
        balance.updateAndGet(current -> current.add(transaction.getAmount()));

        auditQueue.offer(transaction);

        // Trigger audit submission when we've accumulated 1000 transactions
        if (auditQueue.size() >= AUDIT_BATCH_SIZE) {
            drainAndSubmit();
        }
    }

    private synchronized void drainAndSubmit() {
        if (auditQueue.size() < AUDIT_BATCH_SIZE) return; // double-check after acquiring lock

        java.util.List<Transaction> batch = new java.util.ArrayList<>(AUDIT_BATCH_SIZE);
        int drained = auditQueue.drainTo(batch, AUDIT_BATCH_SIZE);

        if (drained == AUDIT_BATCH_SIZE) {
            auditService.submit(batch);
        } else {
            // Put them back if we didn't get exactly 1000
            batch.forEach(auditQueue::offer);
        }
    }

    @Override
    public double retrieveBalance() {
        return balance.get().doubleValue();
    }
}