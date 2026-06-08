# Bank Account System

A transaction processing system consisting of a Producer, a Balance Tracker, and a React UI.

## Architecture

- **Producer** — a separate JVM that generates random credit and debit transactions at 50/s (25 credits + 25 debits), sent to the Balance Tracker over HTTP
- **Balance Tracker** — a Spring Boot application that processes transactions, tracks the running balance, exposes a REST API, and submits every 1000 transactions to an audit system in the minimum number of batches
- **Balance Tracker UI** — a React application that displays the account number and live balance, refreshed every 2 seconds

## Requirements

- Java 17+
- Maven 3.8+
- Node.js 18+

## Running the application

### 1. Build the Java modules

```bash
mvn clean package -DskipTests
```

### 2. Start the Balance Tracker (port 8081)

```bash
java -jar balance-tracker/target/balance-tracker-1.0.0.jar
```

### 3. Start the Producer (port 8082)

```bash
java -jar producer/target/producer-1.0.0.jar
```

### 4. Start the UI (port 3000)

```bash
cd balance-tracker-ui
npm install
npm run dev
```

Open http://localhost:3000

### 5. Run tests

```bash
mvn test -pl balance-tracker
```

## Key design decisions

**Thread safety** — balance is tracked with `AtomicReference<BigDecimal>`, using a lock-free CAS loop on the hot path. The audit drain is `synchronized` to prevent double-submission.

**BigDecimal for money** — `double` loses precision for financial arithmetic; `BigDecimal` with `HALF_UP` rounding is used throughout.

**Audit batching** — a greedy O(n) bin-packing algorithm minimises batch count while keeping each batch's absolute transaction value within £1,000,000. This scales linearly for larger submission sizes.

**Separate JVMs** — the Producer communicates with the Balance Tracker over HTTP, keeping the two processes fully decoupled.
