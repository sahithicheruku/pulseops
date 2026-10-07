package com.pulseops.transaction_service.event;

import com.pulseops.transaction_service.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final TransactionService transactionService;

    public TransactionEventConsumer(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @RetryableTopic(attempts = "${app.kafka.retry.attempts:3}", backOff = @BackOff(delay = 1000, multiplier = 2), dltTopicSuffix = ".DLT")
    @KafkaListener(topics = TransactionEventProducer.TOPIC, groupId = "transaction-service")
    public void consume(TransactionCreatedEvent event) {
        log.info("event=transaction_created_received transactionId={} traceId={} status={}",
                event.transactionId(), event.traceId(), event.status());
        try {
            transactionService.processCreatedEvent(event);
            log.info("event=transaction_created_processed transactionId={} traceId={}", event.transactionId(), event.traceId());
        } catch (RuntimeException exception) {
            log.error("event=transaction_created_processing_failed transactionId={} traceId={} errorType={}",
                    event.transactionId(), event.traceId(), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    @DltHandler
    public void handleDeadLetter(TransactionCreatedEvent event, Exception exception) {
        var reason = "Kafka processing exhausted retries: " + exception.getClass().getSimpleName();
        transactionService.markFailedAfterKafkaRetries(event, reason);
        log.error("event=transaction_dead_lettered transactionId={} traceId={} reason={}", event.transactionId(), event.traceId(), reason);
    }
}
