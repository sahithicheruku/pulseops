package com.pulseops.transaction_service.event;

import static org.mockito.Mockito.verify;

import com.pulseops.transaction_service.domain.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class TransactionEventProducerTest {
    @Mock
    private KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    @Test
    void publishesCreatedEventToTransactionTopic() {
        var id = UUID.randomUUID();
        var event = new TransactionCreatedEvent(
                id, new BigDecimal("12.50"), "USD", TransactionStatus.RECEIVED, Instant.now());

        new TransactionEventProducer(kafkaTemplate).publish(event);

        verify(kafkaTemplate).send("transaction.created", id.toString(), event);
    }
}
