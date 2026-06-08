// balance-tracker/src/main/java/com/fintrack/tracker/model/Transaction.java
package com.fintrack.tracker.model;

import java.math.BigDecimal;

public class Transaction {
    private String id;
    private BigDecimal amount;

    // No-arg constructor required for Jackson deserialization
    public Transaction() {}

    public Transaction(String id, BigDecimal amount) {
        this.id = id;
        this.amount = amount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}