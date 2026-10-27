package com.pulseops.transaction_service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.repository.TransactionRepository;
import com.pulseops.transaction_service.repository.TransactionStatusHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoDataSeederTest {
    @Test
    void firstSeedInsertsExpectedRowsAndPreservesNonDemoRows() {
        var transactions = org.mockito.Mockito.mock(TransactionRepository.class);
        var histories = org.mockito.Mockito.mock(TransactionStatusHistoryRepository.class);
        var jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        when(transactions.findById(any(UUID.class))).thenReturn(Optional.empty());
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(1L, 1L, 0L, 121L, 1L, 120L, 0L);

        var report = new DemoDataSeeder(transactions, histories, jdbc, "seed", false).seedInsertOnly();

        assertThat(report.inserted()).isEqualTo(120);
        assertThat(report.nonDemoBefore()).isEqualTo(report.nonDemoAfter());
        assertThat(report.demoAfter()).isEqualTo(120);
    }

    @Test
    void secondSeedFindsDeterministicRowsAndInsertsNoDuplicates() {
        var transactions = org.mockito.Mockito.mock(TransactionRepository.class);
        var histories = org.mockito.Mockito.mock(TransactionStatusHistoryRepository.class);
        var jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
        var existing = new Transaction(new BigDecimal("10.00"), "USD", "existing demo");
        existing.applySeedData(Instant.now(), com.pulseops.transaction_service.domain.TransactionStatus.COMPLETED, null, 0, null);
        when(transactions.findById(any(UUID.class))).thenReturn(Optional.of(existing));
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(120L, 0L, 120L, 120L, 0L, 120L, 0L);

        var report = new DemoDataSeeder(transactions, histories, jdbc, "seed", false).seedInsertOnly();

        assertThat(report.inserted()).isZero();
        assertThat(report.totalAfter()).isEqualTo(report.totalBefore());
        assertThat(report.demoAfter()).isEqualTo(120);
    }
}
