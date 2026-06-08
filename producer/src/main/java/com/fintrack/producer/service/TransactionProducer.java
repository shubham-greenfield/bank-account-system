// producer/src/main/java/com/fintrack/producer/service/TransactionProducer.java
package com.fintrack.producer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.producer.model.Transaction;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionProducer {

    private static final int RATE_PER_SECOND = 25;          // 25 credits + 25 debits = 50/s
    private static final BigDecimal MIN_VALUE = BigDecimal.valueOf(200);
    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(500_000);

    @Value("${tracker.url:http://localhost:8081/transactions}")
    private String trackerUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Random random = new Random();

    @PostConstruct
    public void start() {
        ScheduledExecutorService creditExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "credit-thread");
            t.setDaemon(true);
            return t;
        });

        ScheduledExecutorService debitExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "debit-thread");
            t.setDaemon(true);
            return t;
        });

        // 25 credits per second = 1 every 40ms
        long intervalMs = 1000L / RATE_PER_SECOND;

        creditExecutor.scheduleAtFixedRate(
            () -> sendTransaction(randomAmount(true)), 0, intervalMs, TimeUnit.MILLISECONDS);

        debitExecutor.scheduleAtFixedRate(
            () -> sendTransaction(randomAmount(false)), 0, intervalMs, TimeUnit.MILLISECONDS);

        System.out.println("Producer started — sending 25 credits/s and 25 debits/s to " + trackerUrl);
    }

    private BigDecimal randomAmount(boolean isCredit) {
        BigDecimal range = MAX_VALUE.subtract(MIN_VALUE);
        BigDecimal scaled = range.multiply(BigDecimal.valueOf(random.nextDouble()))
                                 .add(MIN_VALUE)
                                 .setScale(2, RoundingMode.HALF_UP);
        return isCredit ? scaled : scaled.negate();
    }

    private void sendTransaction(BigDecimal amount) {
        try {
            Transaction tx = new Transaction(UUID.randomUUID().toString(), amount);
            String json = objectMapper.writeValueAsString(tx);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trackerUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            System.err.println("Failed to send transaction: " + e.getMessage());
        }
    }
}