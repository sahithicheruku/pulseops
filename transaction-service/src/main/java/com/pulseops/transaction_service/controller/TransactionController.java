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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Transaction> findAll() {
        return service.findAll();
    }

    @PostMapping
    public ResponseEntity<Transaction> create(@Valid @RequestBody CreateTransactionRequest request) {
        var transaction = service.create(request);
        return ResponseEntity.status(201).body(transaction);
    }

    @GetMapping("/{id}")
    public Transaction findById(@PathVariable UUID id) {
        return service.findById(id);
    }
}
