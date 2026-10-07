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

    public Transaction() {
    }

    public Transaction(BigDecimal amount, String currency, String description) {
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

    @PrePersist
    private void setCreatedAt() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void setUpdatedAt() {
        updatedAt = Instant.now();
    }
}
