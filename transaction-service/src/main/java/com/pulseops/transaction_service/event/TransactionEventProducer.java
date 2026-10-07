package com.pulseops.transaction_service.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventProducer {
    static final String TOPIC = "transaction.created";

    private final KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate;

    public TransactionEventProducer(KafkaTemplate<String, TransactionCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TransactionCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.transactionId().toString(), event);
    }
}
