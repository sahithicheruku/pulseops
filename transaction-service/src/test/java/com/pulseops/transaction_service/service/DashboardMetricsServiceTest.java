package com.pulseops.transaction_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.repository.TransactionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DashboardMetricsServiceTest {
    @Test
    void buildsMetricsFromRepositoryData() {
        var repository = Mockito.mock(TransactionRepository.class);
        when(repository.count()).thenReturn(4L);
        when(repository.countByStatus(TransactionStatus.COMPLETED)).thenReturn(2L);
        when(repository.countByStatus(TransactionStatus.PROCESSING)).thenReturn(1L);
        when(repository.countByStatus(TransactionStatus.RECEIVED)).thenReturn(1L);
        when(repository.countByStatus(TransactionStatus.VALIDATING)).thenReturn(0L);
        when(repository.countByStatus(TransactionStatus.FAILED)).thenReturn(0L);
        when(repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Mockito.any(Instant.class), Mockito.any(Instant.class)))
                .thenReturn(List.of());

        var metrics = new DashboardMetricsService(repository).getMetrics();

        assertThat(metrics.total()).isEqualTo(4L);
        assertThat(metrics.statusCounts()).containsEntry("COMPLETED", 2L);
        assertThat(metrics.activity()).hasSize(7);
    }
}
