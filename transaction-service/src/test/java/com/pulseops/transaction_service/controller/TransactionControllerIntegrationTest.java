package com.pulseops.transaction_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import com.pulseops.transaction_service.service.TransactionService;
import com.pulseops.transaction_service.service.TransactionRateLimiter;
import com.pulseops.transaction_service.service.TransactionStatusStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.context.annotation.Import;
import com.pulseops.transaction_service.security.SecurityConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;

@WebMvcTest(TransactionController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
class TransactionControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @MockitoBean
    private TransactionRateLimiter rateLimiter;

    @MockitoBean
    private TransactionStatusStream statusStream;

    @Test
    void postCreatesTransaction() throws Exception {
        var transaction = new Transaction(new BigDecimal("12.50"), "USD", "Coffee");
        when(rateLimiter.allow(anyString())).thenReturn(true);
        when(service.create(any(), anyString())).thenReturn(transaction);

        mockMvc.perform(post("/api/v1/transactions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "coffee-1")
                        .content("{\"amount\":12.50,\"currency\":\"USD\",\"description\":\"Coffee\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void postRejectsInvalidTransaction() throws Exception {
        when(rateLimiter.allow(anyString())).thenReturn(true);
        mockMvc.perform(post("/api/v1/transactions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "invalid-1")
                        .content("{\"amount\":0,\"currency\":\"US\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void getReturnsTransactions() throws Exception {
        when(service.findAll(any(), org.mockito.ArgumentMatchers.isNull())).thenReturn(
                new PageImpl<>(List.of(new Transaction(new BigDecimal("5.00"), "USD", "Lunch"))));

        mockMvc.perform(get("/api/v1/transactions").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(5.00))
                .andExpect(jsonPath("$.content[0].currency").value("USD"));
    }

    @Test
    void getFiltersTransactionsByStatus() throws Exception {
        when(service.findAll(any(), org.mockito.ArgumentMatchers.eq(TransactionStatus.COMPLETED))).thenReturn(
                new PageImpl<>(List.of(new Transaction(new BigDecimal("8.00"), "USD", "Paid"))));

        mockMvc.perform(get("/api/v1/transactions?status=COMPLETED&page=1&size=5").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(8.00));
    }

    @Test
    void apiRequiresJwt() throws Exception {
        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metricsRequiresJwt() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prometheusRequiresJwt() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postReturnsTooManyRequestsWhenRateLimitExceeded() throws Exception {
        when(rateLimiter.allow(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/transactions").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "limited-1")
                        .content("{\"amount\":12.50,\"currency\":\"USD\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    void getReturnsTransactionById() throws Exception {
        var id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(new Transaction(new BigDecimal("7.00"), "EUR", "Train"));

        mockMvc.perform(get("/api/v1/transactions/{id}", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(7.00))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getReturnsStatusHistory() throws Exception {
        var id = UUID.randomUUID();
        when(service.getStatusHistory(id)).thenReturn(
                List.of(new TransactionStatusHistory(id, TransactionStatus.RECEIVED)));

        mockMvc.perform(get("/api/v1/transactions/{id}/status", id).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(id.toString()))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }
}
