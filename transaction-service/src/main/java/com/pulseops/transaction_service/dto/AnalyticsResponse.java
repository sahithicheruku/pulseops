package com.pulseops.transaction_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        String range,
        long totalTransactions,
        long successfulTransactions,
        long failedTransactions,
        double successRate,
        double failureRate,
        double averageProcessingLatencyMs,
        double p95ProcessingLatencyMs,
        long recentFailureCount,
        List<Bucket> volume,
        List<Bucket> throughput,
        Map<String, Long> currencyDistribution,
        Map<String, Long> statusDistribution
) {
    public record Bucket(Instant bucket, long count) { }
}
