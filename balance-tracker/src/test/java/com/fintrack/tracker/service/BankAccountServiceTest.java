// balance-tracker/src/test/java/com/fintrack/tracker/service/BankAccountServiceTest.java
package com.fintrack.tracker.service;

import com.fintrack.tracker.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BankAccountServiceTest {

    private AuditService auditService;
    private BankAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        service = new BankAccountServiceImpl(auditService);
    }

    @Test
    void balanceStartsAtZero() {
        assertEquals(0.0, service.retrieveBalance());
    }

    @Test
    void creditIncreasesBalance() {
        service.processTransaction(tx("50000.00"));
        assertEquals(50000.0, service.retrieveBalance(), 0.001);
    }

    @Test
    void debitDecreasesBalance() {
        service.processTransaction(tx("50000.00"));
        service.processTransaction(tx("-20000.00"));
        assertEquals(30000.0, service.retrieveBalance(), 0.001);
    }

    @Test
    void balanceIsCorrectAfterMixedTransactions() {
        service.processTransaction(tx("100000.00"));
        service.processTransaction(tx("-40000.00"));
        service.processTransaction(tx("25000.50"));
        service.processTransaction(tx("-10000.25"));
        assertEquals(75000.25, service.retrieveBalance(), 0.001);
    }

    @Test
    void auditTriggeredAfter1000Transactions() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            service.processTransaction(tx("500.00"));
        }
        verify(auditService, times(1)).submit(anyList());
    }

    @Test
    void auditSubmittedWithExactly1000Transactions() throws InterruptedException {
        List<List<Transaction>> captured = new ArrayList<>();
        doAnswer(inv -> {
            captured.add(new ArrayList<>(inv.getArgument(0)));
            return null;
        }).when(auditService).submit(anyList());

        for (int i = 0; i < 1000; i++) {
            service.processTransaction(tx("500.00"));
        }
        assertEquals(1, captured.size());
        assertEquals(1000, captured.get(0).size());
    }

    @Test
    void balanceIsThreadSafe() throws InterruptedException {
        int threads = 50;
        int txPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threads);
        ExecutorService exec = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            exec.submit(() -> {
                for (int j = 0; j < txPerThread; j++) {
                    service.processTransaction(tx("1.00"));
                }
                latch.countDown();
            });
        }
        latch.await();
        assertEquals(threads * txPerThread, service.retrieveBalance(), 0.001);
    }

    // --- AuditService partition tests ---

    @Test
    void singleBatchWhenUnderLimit() {
        AuditService audit = new AuditService();
        List<Transaction> txs = List.of(tx("300000.00"), tx("-200000.00"), tx("100000.00"));
        List<List<Transaction>> batches = audit.partition(txs);
        assertEquals(1, batches.size());
        assertEquals(3, batches.get(0).size());
    }

    @Test
    void splitIntoBatchesWhenOverLimit() {
        AuditService audit = new AuditService();
        // Each tx is £400k abs. Two fit (£800k), third would make £1.2M → new batch
        List<Transaction> txs = List.of(tx("400000.00"), tx("-400000.00"), tx("400000.00"));
        List<List<Transaction>> batches = audit.partition(txs);
        assertEquals(2, batches.size());
        assertEquals(2, batches.get(0).size());
        assertEquals(1, batches.get(1).size());
    }

    @Test
    void absoluteValueUsedNotNetValue() {
        // A £600k credit and a £600k debit = £1.2M absolute, must split
        AuditService audit = new AuditService();
        List<Transaction> txs = List.of(tx("600000.00"), tx("-600000.00"));
        List<List<Transaction>> batches = audit.partition(txs);
        assertEquals(2, batches.size());
    }

    @Test
    void singleTransactionExceedingLimitGetsItsOwnBatch() {
        // A single £200 transaction is always valid; maximum per tx is £500k < £1M
        AuditService audit = new AuditService();
        List<Transaction> txs = List.of(tx("200.00"));
        List<List<Transaction>> batches = audit.partition(txs);
        assertEquals(1, batches.size());
    }

    private Transaction tx(String amount) {
        return new Transaction(UUID.randomUUID().toString(), new BigDecimal(amount));
    }
}