package com.pulseops.transaction_service.controller;

import com.pulseops.transaction_service.dto.DashboardMetrics;
import com.pulseops.transaction_service.service.DashboardMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardMetricsService service;

    public DashboardController(DashboardMetricsService service) {
        this.service = service;
    }

    @GetMapping("/metrics")
    public DashboardMetrics metrics(Authentication authentication) {
        return service.getMetrics(authentication.getName());
    }
}
