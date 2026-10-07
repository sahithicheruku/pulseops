package com.pulseops.transaction_service.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulseops.transaction_service.dto.AnalyticsResponse;
import com.pulseops.transaction_service.service.AnalyticsService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService service;

    @Test
    void returnsAnalyticsForSelectedRange() throws Exception {
        when(service.getAnalytics("24h")).thenReturn(new AnalyticsResponse(
                "24h", 1, 1, 0, 100, 0, 10, 10, 0, List.of(), List.of(), Map.of("USD", 1L), Map.of("COMPLETED", 1L)));

        mockMvc.perform(get("/api/v1/analytics?range=24h").with(jwt()))
                .andExpect(status().isOk());
    }
}
