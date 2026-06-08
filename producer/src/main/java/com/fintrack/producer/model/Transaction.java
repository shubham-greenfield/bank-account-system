// producer/src/main/java/com/fintrack/producer/model/Transaction.java
package com.fintrack.producer.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {

    private final String id;
    private final BigDecimal amount; // negative = debit, positive = credit

    public Transaction(String id, BigDecimal amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() { return id; }
    public BigDecimal getAmount() { return amount; }

    @Override
    public String toString() {
        return "Transaction{id='" + id + "', amount=" + amount + "}";
    }
}