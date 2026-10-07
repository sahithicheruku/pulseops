package com.pulseops.transaction_service.controller;

import com.pulseops.transaction_service.dto.AnalyticsResponse;
import com.pulseops.transaction_service.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) { this.service = service; }

    @GetMapping
    public AnalyticsResponse analytics(@RequestParam(defaultValue = "7d") String range) {
        return service.getAnalytics(range);
    }
}
