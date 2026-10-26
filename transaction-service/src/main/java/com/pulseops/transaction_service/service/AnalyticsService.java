package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.dto.AnalyticsResponse;
import com.pulseops.transaction_service.repository.TransactionAnalyticsRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final TransactionAnalyticsRepository repository;

    public AnalyticsService(TransactionAnalyticsRepository repository) {
        this.repository = repository;
    }

    public AnalyticsResponse getAnalytics(String requestedRange) {
        var range = normalizeRange(requestedRange);
        var to = Instant.now();
        var from = to.minus(Duration.ofHours(range.equals("24h") ? 24 : range.equals("7d") ? 24 * 7 : 24 * 30));
        var volume = repository.volume(from, to, range).stream().map(row -> new AnalyticsResponse.Bucket(row.getBucket(), row.getCount())).toList();
        var throughput = repository.throughput(from, to, range).stream().map(row -> new AnalyticsResponse.Bucket(row.getBucket(), row.getCount())).toList();
        var latency = repository.latency(from, to); var currency = new LinkedHashMap<String, Long>();
        repository.currencyDistribution(from, to).forEach(row -> currency.put(row.getCurrency(), row.getCount()));
        var status = new LinkedHashMap<String, Long>(); repository.statusDistribution(from, to).forEach(row -> status.put(row.getStatus(), row.getCount()));
        var total = status.values().stream().mapToLong(Long::longValue).sum(); var successful = status.getOrDefault("COMPLETED", 0L); var failed = status.getOrDefault("FAILED", 0L);
        return new AnalyticsResponse(range, total, successful, failed, rate(successful, total), rate(failed, total), value(latency == null ? null : latency.getAverageMs()), value(latency == null ? null : latency.getP95Ms()), repository.recentFailureCount(from, to), volume, throughput, currency, status);
    }
    public AnalyticsResponse getAnalytics(String requestedRange, String ownerKey) {
        var range = normalizeRange(requestedRange);
        var to = Instant.now();
        var from = to.minus(Duration.ofHours(range.equals("24h") ? 24 : range.equals("7d") ? 24 * 7 : 24 * 30));
        var volume = repository.volume(ownerKey, from, to, range).stream()
                .map(row -> new AnalyticsResponse.Bucket(row.getBucket(), row.getCount())).toList();
        var throughput = repository.throughput(ownerKey, from, to, range).stream()
                .map(row -> new AnalyticsResponse.Bucket(row.getBucket(), row.getCount())).toList();
        var latency = repository.latency(ownerKey, from, to);
        var currency = new LinkedHashMap<String, Long>();
        repository.currencyDistribution(ownerKey, from, to).forEach(row -> currency.put(row.getCurrency(), row.getCount()));
        var status = new LinkedHashMap<String, Long>();
        repository.statusDistribution(ownerKey, from, to).forEach(row -> status.put(row.getStatus(), row.getCount()));
        var total = status.values().stream().mapToLong(Long::longValue).sum();
        var successful = status.getOrDefault("COMPLETED", 0L);
        var failed = status.getOrDefault("FAILED", 0L);
        return new AnalyticsResponse(range, total, successful, failed,
                rate(successful, total), rate(failed, total), value(latency == null ? null : latency.getAverageMs()),
                value(latency == null ? null : latency.getP95Ms()), repository.recentFailureCount(ownerKey, from, to),
                volume, throughput, currency, status);
    }

    private static String normalizeRange(String range) {
        return switch (range == null ? "7d" : range.toLowerCase()) {
            case "24h", "7d", "30d" -> range.toLowerCase();
            default -> throw new IllegalArgumentException("Range must be one of 24h, 7d, or 30d");
        };
    }

    private static double rate(long value, long total) { return total == 0 ? 0 : value * 100.0 / total; }
    private static double value(Double value) { return value == null ? 0 : value; }
}
