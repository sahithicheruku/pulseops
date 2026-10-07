package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.dto.CreateTransactionRequest;
import com.pulseops.transaction_service.repository.TransactionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    public List<Transaction> findAll() {
        return repository.findAll();
    }

    public Transaction create(CreateTransactionRequest request) {
        return repository.save(new Transaction(request.amount(), request.currency(), request.description()));
    }

    public Transaction findById(UUID id) {
        return repository.findById(id).orElseThrow();
    }
}
