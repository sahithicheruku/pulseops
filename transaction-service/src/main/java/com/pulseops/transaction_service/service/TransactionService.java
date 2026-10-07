package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.dto.CreateTransactionRequest;
import com.pulseops.transaction_service.event.TransactionCreatedEvent;
import com.pulseops.transaction_service.event.TransactionEventProducer;
import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.repository.TransactionRepository;
import com.pulseops.transaction_service.repository.TransactionStatusHistoryRepository;
import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository repository;
    private final TransactionEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;
    private final TransactionStatusHistoryRepository historyRepository;
    private final TransactionStatusStream statusStream;

    public TransactionService(TransactionRepository repository, TransactionEventProducer eventProducer,
                              StringRedisTemplate redisTemplate, TransactionStatusHistoryRepository historyRepository,
                              TransactionStatusStream statusStream) {
        this.repository = repository;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
        this.historyRepository = historyRepository;
        this.statusStream = statusStream;
    }

    public Page<Transaction> findAll(Pageable pageable, TransactionStatus status) {
        return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
    }

    public Transaction create(CreateTransactionRequest request, String idempotencyKey) {
        var redisKey = "transaction:idempotency:" + idempotencyKey;
        var reservation = "pending:" + UUID.randomUUID();
        var reservedId = redisTemplate.opsForValue().setIfAbsent(redisKey, reservation);
        if (!Boolean.TRUE.equals(reservedId)) {
            return findById(UUID.fromString(redisTemplate.opsForValue().get(redisKey)));
        }

        var transaction = repository.save(new Transaction(request.amount(), request.currency(), request.description()));
        redisTemplate.opsForValue().set(redisKey, transaction.getId().toString());
        var history = historyRepository.save(new TransactionStatusHistory(transaction.getId(), transaction.getStatus()));
        if (history != null) statusStream.publish(history);
        log.info("event=transaction_created transactionId={} status={}",
                transaction.getId(), transaction.getStatus());
        eventProducer.publish(new TransactionCreatedEvent(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCreatedAt()));
        return transaction;
    }

    public Transaction findById(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    public List<TransactionStatusHistory> getStatusHistory(UUID id) {
        findById(id);
        return historyRepository.findByTransactionIdOrderByChangedAtAsc(id);
    }

    public void processCreatedEvent(TransactionCreatedEvent event) {
        try {
            var transaction = findById(event.transactionId());
            for (var status : new TransactionStatus[] {
                    TransactionStatus.VALIDATING, TransactionStatus.PROCESSING, TransactionStatus.COMPLETED}) {
                var previousStatus = transaction.getStatus();
                transaction.updateStatus(status);
                repository.save(transaction);
                var history = historyRepository.save(new TransactionStatusHistory(transaction.getId(), status));
                if (history != null) statusStream.publish(history);
                log.info("event=transaction_status_changed transactionId={} from={} to={}",
                        transaction.getId(), previousStatus, status);
            }
        } catch (RuntimeException exception) {
            log.error("event=transaction_processing_failed transactionId={} errorType={}",
                    event.transactionId(), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }
}
