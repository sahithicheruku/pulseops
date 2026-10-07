package com.pulseops.transaction_service.event;

import static org.mockito.Mockito.verify;

import com.pulseops.transaction_service.service.TransactionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {
    @Mock
    private TransactionService transactionService;

    @Test
    void consumesCreatedEventAndProcessesTransaction() {
        var event = new TransactionCreatedEvent(
                UUID.randomUUID(), new BigDecimal("12.50"), "USD", 
                com.pulseops.transaction_service.domain.TransactionStatus.RECEIVED, Instant.now());

        new TransactionEventConsumer(transactionService).consume(event);

        verify(transactionService).processCreatedEvent(event);
    }
}
