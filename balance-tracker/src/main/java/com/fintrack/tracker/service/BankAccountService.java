// balance-tracker/src/main/java/com/fintrack/tracker/service/BankAccountService.java
package com.fintrack.tracker.service;

import com.fintrack.tracker.model.Transaction;

/**
 * Service to aggregate transactions tracking the overall balance for an account.
 */
public interface BankAccountService {

    /**
     * Process a given transaction - this is to be called by the credit and debit generation threads.
     * @param transaction transaction to process
     */
    void processTransaction(Transaction transaction);

    /**
     * Retrieve the balance in the account
     */
    double retrieveBalance();
}