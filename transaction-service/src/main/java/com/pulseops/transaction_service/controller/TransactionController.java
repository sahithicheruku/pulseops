package com.pulseops.transaction_service.controller;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.service.TransactionService;
import com.pulseops.transaction_service.dto.CreateTransactionRequest;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;
import com.pulseops.transaction_service.service.TransactionRateLimiter;
import com.pulseops.transaction_service.controller.RateLimitExceededException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.pulseops.transaction_service.domain.TransactionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    private final TransactionRateLimiter rateLimiter;

    public TransactionController(TransactionService service, TransactionRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    public Page<Transaction> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status) {
        return service.findAll(PageRequest.of(page, size), status);
    }

    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest httpRequest) {
        if (!rateLimiter.allow(httpRequest.getRemoteAddr())) {
            throw new RateLimitExceededException();
        }
        var transaction = service.create(request, idempotencyKey);
        return ResponseEntity.status(201).body(transaction);
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/status")
    public List<com.pulseops.transaction_service.domain.TransactionStatusHistory> getStatusHistory(
            @PathVariable UUID id) {
        return service.getStatusHistory(id);
    }
}
