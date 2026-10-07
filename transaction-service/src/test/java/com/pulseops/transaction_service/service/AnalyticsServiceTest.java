package com.pulseops.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.repository.TransactionAnalyticsRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {
    @Test
    void aggregatesPersistedAnalyticsRows() {
        var repository = mock(TransactionAnalyticsRepository.class);
        var bucket = mock(TransactionAnalyticsRepository.Bucket.class);
        var currency = mock(TransactionAnalyticsRepository.CurrencyCount.class);
        var status = mock(TransactionAnalyticsRepository.StatusCount.class);
        var latency = mock(TransactionAnalyticsRepository.Latency.class);
        when(bucket.getBucket()).thenReturn(Instant.parse("2026-10-06T00:00:00Z"));
        when(bucket.getCount()).thenReturn(3L);
        when(currency.getCurrency()).thenReturn("USD");
        when(currency.getCount()).thenReturn(3L);
        when(status.getStatus()).thenReturn("COMPLETED");
        when(status.getCount()).thenReturn(3L);
        when(latency.getAverageMs()).thenReturn(125.0);
        when(latency.getP95Ms()).thenReturn(250.0);
        when(repository.volume(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("7d")))
                .thenReturn(List.of(bucket));
        when(repository.throughput(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("7d")))
                .thenReturn(List.of(bucket));
        when(repository.currencyDistribution(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(currency));
        when(repository.statusDistribution(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of(status));
        when(repository.latency(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(latency);
        when(repository.recentFailureCount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);

        var analytics = new AnalyticsService(repository).getAnalytics("7d");

        assertThat(analytics.totalTransactions()).isEqualTo(3);
        assertThat(analytics.successRate()).isEqualTo(100.0);
        assertThat(analytics.averageProcessingLatencyMs()).isEqualTo(125.0);
        assertThat(analytics.p95ProcessingLatencyMs()).isEqualTo(250.0);
        assertThat(analytics.volume()).hasSize(1);
        assertThat(analytics.currencyDistribution()).containsEntry("USD", 3L);
    }

    @Test
    void rejectsUnsupportedRange() {
        var repository = mock(TransactionAnalyticsRepository.class);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AnalyticsService(repository).getAnalytics("90d"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
