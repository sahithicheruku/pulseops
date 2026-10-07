package com.pulseops.transaction_service.repository;

import com.pulseops.transaction_service.domain.Transaction;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TransactionAnalyticsRepository extends Repository<Transaction, UUID> {
    interface Bucket {
        Instant getBucket();
        long getCount();
    }

    interface CurrencyCount {
        String getCurrency();
        long getCount();
    }

    interface StatusCount {
        String getStatus();
        long getCount();
    }

    interface Latency {
        Double getAverageMs();
        Double getP95Ms();
    }

    @Query(value = """
            SELECT date_trunc(
                CASE WHEN :range = '24h' THEN 'hour' WHEN :range = '7d' THEN 'day' ELSE 'day' END,
                t.created_at) AS bucket,
                COUNT(*) AS count
            FROM transaction t
            WHERE t.created_at >= :from AND t.created_at < :to
            GROUP BY bucket ORDER BY bucket
            """, nativeQuery = true)
    List<Bucket> volume(@Param("from") Instant from, @Param("to") Instant to, @Param("range") String range);

    @Query(value = """
            SELECT date_trunc(
                CASE WHEN :range = '24h' THEN 'hour' WHEN :range = '7d' THEN 'day' ELSE 'day' END,
                completed.changed_at) AS bucket,
                COUNT(*) AS count
            FROM transaction t
            JOIN transaction_status_history completed
              ON completed.transaction_id = t.id AND completed.status = 'COMPLETED'
            WHERE completed.changed_at >= :from AND completed.changed_at < :to
            GROUP BY bucket ORDER BY bucket
            """, nativeQuery = true)
    List<Bucket> throughput(@Param("from") Instant from, @Param("to") Instant to, @Param("range") String range);

    @Query(value = """
            SELECT AVG(latency_ms) AS averageMs,
                   percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms) AS p95Ms
            FROM (
                SELECT EXTRACT(EPOCH FROM (completed.changed_at - t.created_at)) * 1000 AS latency_ms
                FROM transaction t
                JOIN transaction_status_history completed
                  ON completed.transaction_id = t.id AND completed.status = 'COMPLETED'
                WHERE t.created_at >= :from AND t.created_at < :to
            ) latencies
            """, nativeQuery = true)
    Latency latency(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT t.currency AS currency, COUNT(*) AS count
            FROM transaction t
            WHERE t.created_at >= :from AND t.created_at < :to
            GROUP BY t.currency ORDER BY count DESC, t.currency
            """, nativeQuery = true)
    List<CurrencyCount> currencyDistribution(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT CASE t.status
                       WHEN 0 THEN 'RECEIVED'
                       WHEN 1 THEN 'VALIDATING'
                       WHEN 2 THEN 'PROCESSING'
                       WHEN 3 THEN 'COMPLETED'
                       WHEN 4 THEN 'FAILED'
                   END AS status,
                   COUNT(*) AS count
            FROM transaction t
            WHERE t.created_at >= :from AND t.created_at < :to
            GROUP BY t.status ORDER BY t.status
            """, nativeQuery = true)
    List<StatusCount> statusDistribution(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT COUNT(*)
            FROM transaction t
            WHERE t.status = 4 AND t.last_failure_at >= :from AND t.last_failure_at < :to
            """, nativeQuery = true)
    long recentFailureCount(@Param("from") Instant from, @Param("to") Instant to);
}
