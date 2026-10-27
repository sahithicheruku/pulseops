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
import com.pulseops.transaction_service.service.TransactionStatusStream;
import com.pulseops.transaction_service.controller.RateLimitExceededException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.pulseops.transaction_service.domain.TransactionStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;
    private final TransactionRateLimiter rateLimiter;
    private final TransactionStatusStream statusStream;

    public TransactionController(TransactionService service, TransactionRateLimiter rateLimiter, TransactionStatusStream statusStream) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.statusStream = statusStream;
    }

    @GetMapping
    public Page<Transaction> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            Authentication authentication) {
        return service.findAll(PageRequest.of(page, size), status, authentication.getName());
    }

    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            HttpServletRequest httpRequest, Authentication authentication) {
        rejectDemoMutation(authentication);
        if (!rateLimiter.allow(httpRequest.getRemoteAddr())) {
            throw new RateLimitExceededException();
        }
        var transaction = service.create(request, idempotencyKey, correlationId, authentication.getName());
        return ResponseEntity.status(201).body(transaction);
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable UUID id, Authentication authentication) {
        return service.findById(id, authentication.getName());
    }

    @GetMapping("/{id}/status")
    public List<com.pulseops.transaction_service.domain.TransactionStatusHistory> getStatusHistory(
            @PathVariable UUID id, Authentication authentication) {
        return service.getStatusHistory(id, authentication.getName());
    }

    @PostMapping("/{id}/retry")
    public Transaction retry(@PathVariable UUID id, Authentication authentication) {
        rejectDemoMutation(authentication);
        return service.retry(id, authentication.getName());
    }

    private void rejectDemoMutation(Authentication authentication) {
        if ("demo".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demo access is read-only");
        }
    }

    @GetMapping(value = "/{id}/status/stream", produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamStatus(@PathVariable UUID id,
            Authentication authentication) {
        service.findById(id, authentication.getName());
        return statusStream.subscribe(id);
    }
}
