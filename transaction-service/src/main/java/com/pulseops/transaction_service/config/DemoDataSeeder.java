package com.pulseops.transaction_service.config;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import com.pulseops.transaction_service.repository.TransactionRepository;
import com.pulseops.transaction_service.repository.TransactionStatusHistoryRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo & !production")
public class DemoDataSeeder implements CommandLineRunner {
    private static final String SOURCE = "DEMO_SEED";
    private static final String NAMESPACE = "pulseops-demo-seed-v1";
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "CAD", "AUD", "INR", "JPY", "CHF"};
    private final TransactionRepository transactions;
    private final TransactionStatusHistoryRepository histories;
    private final String action;

    public DemoDataSeeder(TransactionRepository transactions, TransactionStatusHistoryRepository histories,
            @Value("${app.demo.seed.action:seed}") String action) {
        this.transactions = transactions;
        this.histories = histories;
        this.action = action;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if ("reset".equalsIgnoreCase(action) || "reseed".equalsIgnoreCase(action)) reset();
        if (!"reset".equalsIgnoreCase(action)) seed();
    }

    private void reset() {
        var seeded = transactions.findBySource(SOURCE);
        if (!seeded.isEmpty()) histories.deleteByTransactionIdIn(seeded.stream().map(Transaction::getId).toList());
        transactions.deleteBySource(SOURCE);
    }

    private void seed() {
        var existing = transactions.findBySource(SOURCE).stream().map(Transaction::getId).collect(java.util.stream.Collectors.toSet());
        var newHistories = new ArrayList<TransactionStatusHistory>();
        var now = Instant.now();
        for (int index = 0; index < 120; index++) {
            var id = deterministicId(index);
            if (existing.contains(id)) continue;
            var createdAt = now.minusSeconds((long) (119 - index) * 6 * 60 * 60).plusSeconds((index % 5) * 71L);
            var traceId = "demo-trace-" + id;
            var failed = index % 17 == 0;
            var processing = !failed && index % 13 == 0;
            var status = failed ? TransactionStatus.FAILED : processing ? TransactionStatus.PROCESSING : TransactionStatus.COMPLETED;
            var transaction = new Transaction(id, amount(index), CURRENCIES[index % CURRENCIES.length], description(index));
            transaction.assignTraceId(traceId);
            transaction.applySeedData(createdAt, status, failed ? failureReason(index) : null,
                    failed ? 1 + index % 2 : 0, failed ? createdAt.plusSeconds(45) : null);
            transactions.save(transaction);
            newHistories.addAll(history(id, traceId, createdAt, status, failed));
        }
        if (!newHistories.isEmpty()) histories.saveAll(newHistories);
    }

    private static List<TransactionStatusHistory> history(UUID id, String traceId, Instant createdAt,
            TransactionStatus finalStatus, boolean failed) {
        var result = new ArrayList<TransactionStatusHistory>();
        result.add(new TransactionStatusHistory(id, TransactionStatus.RECEIVED, null, traceId, createdAt));
        result.add(new TransactionStatusHistory(id, TransactionStatus.VALIDATING, null, traceId, createdAt.plusSeconds(8)));
        result.add(new TransactionStatusHistory(id, TransactionStatus.PROCESSING, null, traceId, createdAt.plusSeconds(18)));
        if (finalStatus == TransactionStatus.COMPLETED) {
            result.add(new TransactionStatusHistory(id, finalStatus, null, traceId, createdAt.plusSeconds(45 + Math.floorMod(id.hashCode(), 180))));
        } else if (failed) {
            result.add(new TransactionStatusHistory(id, finalStatus, failureReason(id), traceId, createdAt.plusSeconds(45)));
        }
        return result;
    }

    private static UUID deterministicId(int index) { return UUID.nameUUIDFromBytes((NAMESPACE + index).getBytes(StandardCharsets.UTF_8)); }
    private static BigDecimal amount(int index) { return BigDecimal.valueOf(25 + ((index * 791) % 245000) / 100.0).setScale(2); }
    private static String description(int index) { return "Demo settlement " + String.format("%03d", index + 1); }
    private static String failureReason(int index) { return index % 2 == 0 ? "Demo bank validation failure" : "Demo downstream timeout"; }
    private static String failureReason(UUID id) { return id.hashCode() % 2 == 0 ? "Demo bank validation failure" : "Demo downstream timeout"; }
}
