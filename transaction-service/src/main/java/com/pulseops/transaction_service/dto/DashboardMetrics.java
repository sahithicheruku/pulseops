package com.pulseops.transaction_service.dto;

import java.util.List;
import java.util.Map;

public record DashboardMetrics(long total, Map<String, Long> statusCounts, List<ActivityPoint> activity) {
    public record ActivityPoint(String label, long count) { }
}
