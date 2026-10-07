package com.pulseops.transaction_service.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity
public class TransactionStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID transactionId;
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
    private Instant changedAt;

    protected TransactionStatusHistory() { }

    public TransactionStatusHistory(UUID transactionId, TransactionStatus status) {
        this.transactionId = transactionId;
        this.status = status;
        this.changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public TransactionStatus getStatus() { return status; }
    public Instant getChangedAt() { return changedAt; }
}
