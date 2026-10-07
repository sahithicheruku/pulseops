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
    @jakarta.persistence.Column(length = 500)
    private String failureReason;
    @jakarta.persistence.Column(nullable = false, length = 64)
    private String traceId;

    protected TransactionStatusHistory() { }

    public TransactionStatusHistory(UUID transactionId, TransactionStatus status) {
        this(transactionId, status, null, null);
    }
    public TransactionStatusHistory(UUID transactionId, TransactionStatus status, String failureReason) {
        this(transactionId, status, failureReason, null);
    }
    public TransactionStatusHistory(UUID transactionId, TransactionStatus status, String failureReason, String traceId) {
        this(transactionId, status, failureReason, traceId, Instant.now());
    }
    public TransactionStatusHistory(UUID transactionId, TransactionStatus status, String failureReason,
            String traceId, Instant changedAt) {
        this.transactionId = transactionId;
        this.status = status;
        this.changedAt = changedAt;
        this.failureReason = failureReason;
        this.traceId = traceId;
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public TransactionStatus getStatus() { return status; }
    public Instant getChangedAt() { return changedAt; }
    public String getFailureReason() { return failureReason; }
    public String getTraceId() { return traceId; }
}
