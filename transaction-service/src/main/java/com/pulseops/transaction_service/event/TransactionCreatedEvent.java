package com.pulseops.transaction_service.event;

import com.pulseops.transaction_service.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCreatedEvent(
        UUID transactionId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        Instant createdAt
) {
}
