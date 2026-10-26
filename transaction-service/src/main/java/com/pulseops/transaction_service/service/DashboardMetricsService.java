package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.dto.DashboardMetrics;
import com.pulseops.transaction_service.repository.TransactionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardMetricsService {
    private final TransactionRepository repository;

    public DashboardMetricsService(TransactionRepository repository) {
        this.repository = repository;
    }

    public DashboardMetrics getMetrics() {
        var counts = new LinkedHashMap<String, Long>();
        Arrays.stream(TransactionStatus.values()).forEach(status -> counts.put(status.name(), repository.countByStatus(status)));
        var today = LocalDate.now(); var from = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant(); var to = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        var transactions = repository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to); var activity = new java.util.ArrayList<DashboardMetrics.ActivityPoint>();
        for (int offset = 6; offset >= 0; offset--) { var day = today.minusDays(offset); var count = transactions.stream().filter(transaction -> transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(day)).count(); activity.add(new DashboardMetrics.ActivityPoint(day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()), count)); }
        return new DashboardMetrics(repository.count(), counts, activity);
    }
    public DashboardMetrics getMetrics(String ownerKey) {
        var counts = new LinkedHashMap<String, Long>();
        Arrays.stream(TransactionStatus.values()).forEach(status -> counts.put(status.name(), repository.countByOwnerKeyAndStatus(ownerKey, status)));

        var today = LocalDate.now();
        var from = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant();
        var to = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        var transactions = repository.findByOwnerKeyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(ownerKey, from, to);
        var activity = new java.util.ArrayList<DashboardMetrics.ActivityPoint>();
        for (int offset = 6; offset >= 0; offset--) {
            var day = today.minusDays(offset);
            var count = transactions.stream()
                    .filter(transaction -> transaction.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().equals(day))
                    .count();
            activity.add(new DashboardMetrics.ActivityPoint(day.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()), count));
        }
        return new DashboardMetrics(repository.countByOwnerKey(ownerKey), counts, activity);
    }
}
