// balance-tracker/src/main/java/com/fintrack/tracker/service/AuditService.java
package com.fintrack.tracker.service;

import com.fintrack.tracker.model.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private static final BigDecimal BATCH_LIMIT = BigDecimal.valueOf(1_000_000);

    /**
     * Submits exactly 1000 transactions split into the minimum number of batches,
     * where each batch's absolute total value does not exceed £1,000,000.
     */
    public void submit(List<Transaction> transactions) {
        List<List<Transaction>> batches = partition(transactions);
        printSubmission(batches);
    }

    /**
     * Greedy bin-packing: iterate transactions in order, accumulating into the
     * current batch until adding the next item would breach the £1M absolute limit.
     * When that happens, seal the current batch and open a new one.
     *
     * Complexity: O(n) — scales linearly regardless of submission size.
     */
    List<List<Transaction>> partition(List<Transaction> transactions) {
        List<List<Transaction>> batches = new ArrayList<>();
        List<Transaction> current = new ArrayList<>();
        BigDecimal currentTotal = BigDecimal.ZERO;

        for (Transaction tx : transactions) {
            BigDecimal absAmount = tx.getAmount().abs();

            if (currentTotal.add(absAmount).compareTo(BATCH_LIMIT) > 0 && !current.isEmpty()) {
                batches.add(current);
                current = new ArrayList<>();
                currentTotal = BigDecimal.ZERO;
            }

            current.add(tx);
            currentTotal = currentTotal.add(absAmount);
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    private void printSubmission(List<List<Transaction>> batches) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  submission: {\n    batches: [\n");

        for (int i = 0; i < batches.size(); i++) {
            List<Transaction> batch = batches.get(i);
            BigDecimal total = batch.stream()
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            sb.append("      {\n");
            sb.append("        totalValueOfAllTransactions: ").append(total.toPlainString()).append("\n");
            sb.append("        countOfTransactions: ").append(batch.size()).append("\n");
            sb.append("      }");

            if (i < batches.size() - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append("    ]\n  }\n}");
        System.out.println(sb);
    }
}