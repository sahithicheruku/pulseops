package com.pulseops.transaction_service.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    @Positive
    private BigDecimal amount;
    @Column(nullable = false)
    @Size(min = 3, max = 3)
    private String currency;
    private String description;
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.RECEIVED;
    private Instant createdAt;
    private Instant updatedAt;
    @Column(length = 500)
    private String failureReason;
    @Column(nullable = false)
    private int retryCount;
    private Instant lastFailureAt;
    @Column(nullable = false, length = 64)
    private String traceId;
    @Column(nullable = false, length = 20)
    private String source = "USER";

    public Transaction() {
    }

    public Transaction(BigDecimal amount, String currency, String description) {
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public Transaction(UUID id, BigDecimal amount, String currency, String description) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }

    public UUID getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public TransactionStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getFailureReason() { return failureReason; }
    public int getRetryCount() { return retryCount; }
    public Instant getLastFailureAt() { return lastFailureAt; }
    public String getTraceId() { return traceId; }
    public String getSource() { return source; }


    public void updateStatus(TransactionStatus status) { this.status = status; }
    public void recordFailure(String reason) { this.status = TransactionStatus.FAILED; this.failureReason = reason; this.lastFailureAt = Instant.now(); }
    public void prepareForRetry() { this.status = TransactionStatus.RECEIVED; this.failureReason = null; }
    public void incrementRetryCount() { this.retryCount++; }
    public void assignTraceId(String traceId) { this.traceId = traceId; }
    public void applySeedData(Instant createdAt, TransactionStatus status, String failureReason,
            int retryCount, Instant lastFailureAt) {
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = status;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.lastFailureAt = lastFailureAt;
        this.traceId = this.traceId == null ? UUID.randomUUID().toString() : this.traceId;
        this.source = "DEMO_SEED";
    }

    @PrePersist
    private void setCreatedAt() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    private void setUpdatedAt() {
        updatedAt = Instant.now();
    }
}
