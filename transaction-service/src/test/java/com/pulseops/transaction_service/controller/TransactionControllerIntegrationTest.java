package com.pulseops.transaction_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.service.TransactionService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransactionController.class)
class TransactionControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService service;

    @Test
    void postCreatesTransaction() throws Exception {
        var transaction = new Transaction(new BigDecimal("12.50"), "USD", "Coffee");
        when(service.create(any())).thenReturn(transaction);

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":12.50,\"currency\":\"USD\",\"description\":\"Coffee\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(12.50))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void postRejectsInvalidTransaction() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":0,\"currency\":\"US\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    void getReturnsTransactions() throws Exception {
        when(service.findAll()).thenReturn(List.of(new Transaction(new BigDecimal("5.00"), "USD", "Lunch")));

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(5.00))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    void getReturnsTransactionById() throws Exception {
        var id = UUID.randomUUID();
        when(service.findById(id)).thenReturn(new Transaction(new BigDecimal("7.00"), "EUR", "Train"));

        mockMvc.perform(get("/api/v1/transactions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(7.00))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }
}
