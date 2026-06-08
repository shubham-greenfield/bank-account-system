// balance-tracker/src/main/java/com/fintrack/tracker/controller/BalanceController.java
package com.fintrack.tracker.controller;

import com.fintrack.tracker.model.Transaction;
import com.fintrack.tracker.service.BankAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class BalanceController {

    private static final String ACCOUNT_ID = "ACC-00123456";

    private final BankAccountService bankAccountService;

    @Autowired
    public BalanceController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping("/transactions")
    public Map<String, String> receiveTransaction(@RequestBody Transaction transaction) {
        bankAccountService.processTransaction(transaction);
        return Map.of("status", "accepted", "id", transaction.getId());
    }

    @GetMapping("/balance")
    public Map<String, Object> getBalance() {
        return Map.of(
            "accountId", ACCOUNT_ID,
            "balance", bankAccountService.retrieveBalance()
        );
    }
}