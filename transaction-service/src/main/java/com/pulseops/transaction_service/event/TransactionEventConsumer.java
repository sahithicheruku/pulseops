package com.pulseops.transaction_service.event;

import com.pulseops.transaction_service.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final TransactionService transactionService;

    public TransactionEventConsumer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @KafkaListener(topics = TransactionEventProducer.TOPIC, groupId = "transaction-service")
    public void consume(TransactionCreatedEvent event) {
        log.info("event=transaction_created_received transactionId={} status={}",
                event.transactionId(), event.status());
        try {
            transactionService.processCreatedEvent(event);
            log.info("event=transaction_created_processed transactionId={}", event.transactionId());
        } catch (RuntimeException exception) {
            log.error("event=transaction_created_processing_failed transactionId={} errorType={}",
                    event.transactionId(), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }
}
