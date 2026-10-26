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
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransactionService {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "CAD", "AUD", "JPY", "CHF", "INR");
    private static final int MAX_RETRY_COUNT = 3;
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
    public Page<Transaction> findAll(Pageable pageable, TransactionStatus status, String ownerKey) {
        return status == null ? repository.findByOwnerKey(ownerKey, pageable) : repository.findByOwnerKeyAndStatus(ownerKey, status, pageable);
    }

    public Transaction create(CreateTransactionRequest request, String idempotencyKey) {
        return create(request, idempotencyKey, UUID.randomUUID().toString());
    }

    public Transaction create(CreateTransactionRequest request, String idempotencyKey, String requestedTraceId) {
        return create(request, idempotencyKey, requestedTraceId, "demo");
    }
    public Transaction create(CreateTransactionRequest request, String idempotencyKey, String requestedTraceId, String ownerKey) {
        var traceId = requestedTraceId == null || requestedTraceId.isBlank() ? UUID.randomUUID().toString() : requestedTraceId;
        var redisKey = "transaction:idempotency:" + ownerKey + ":" + idempotencyKey;
        var reservation = "pending:" + UUID.randomUUID();
        var reservedId = redisTemplate.opsForValue().setIfAbsent(redisKey, reservation);
        if (!Boolean.TRUE.equals(reservedId)) {
            return findById(UUID.fromString(redisTemplate.opsForValue().get(redisKey)));
        }

        var transactionEntity = new Transaction(request.amount(), request.currency(), request.description());
        transactionEntity.assignOwner(ownerKey);
        transactionEntity.assignTraceId(traceId);
        var transaction = repository.save(transactionEntity);
        redisTemplate.opsForValue().set(redisKey, transaction.getId().toString());
        var history = historyRepository.save(new TransactionStatusHistory(transaction.getId(), transaction.getStatus(), null, traceId));
        if (history != null) statusStream.publish(history);
        log.info("event=transaction_created transactionId={} traceId={} status={}",
                transaction.getId(), traceId, transaction.getStatus());
        eventProducer.publish(new TransactionCreatedEvent(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getCreatedAt(), traceId));
        return transaction;
    }

    public Transaction findById(UUID id) {
        return repository.findById(id).orElseThrow();
    }
    public Transaction findById(UUID id, String ownerKey) { return repository.findByIdAndOwnerKey(id, ownerKey).orElseThrow(); }

    public List<TransactionStatusHistory> getStatusHistory(UUID id) {
        findById(id);
        return historyRepository.findByTransactionIdOrderByChangedAtAsc(id);
    }
    public List<TransactionStatusHistory> getStatusHistory(UUID id, String ownerKey) { findById(id, ownerKey); return historyRepository.findByTransactionIdOrderByChangedAtAsc(id); }

    public void processCreatedEvent(TransactionCreatedEvent event) {
        try {
            var transaction = findById(event.transactionId());
            var validationFailure = validationFailure(transaction);
            if (validationFailure != null) {
                markFailed(transaction, validationFailure);
                return;
            }
            for (var status : new TransactionStatus[] {
                    TransactionStatus.VALIDATING, TransactionStatus.PROCESSING, TransactionStatus.COMPLETED}) {
                var previousStatus = transaction.getStatus();
                transaction.updateStatus(status);
                repository.save(transaction);
                var history = historyRepository.save(new TransactionStatusHistory(transaction.getId(), status, null, event.traceId()));
                if (history != null) statusStream.publish(history);
                log.info("event=transaction_status_changed transactionId={} traceId={} from={} to={}",
                        transaction.getId(), event.traceId(), previousStatus, status);
            }
        } catch (RuntimeException exception) {
            log.error("event=transaction_processing_failed transactionId={} traceId={} errorType={}",
                    event.transactionId(), event.traceId(), exception.getClass().getSimpleName(), exception);
            throw exception;
        }
    }

    public Transaction retry(UUID id) {
        var transaction = findById(id);
        if (transaction.getStatus() != TransactionStatus.FAILED || transaction.getRetryCount() >= MAX_RETRY_COUNT) {
            throw new IllegalStateException("Transaction is not eligible for retry");
        }
        var retryKey = "transaction:retry:" + id + ":" + transaction.getRetryCount();
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(retryKey, "1"))) {
            throw new IllegalStateException("Transaction retry is already in progress");
        }
        transaction.incrementRetryCount();
        transaction.prepareForRetry();
        var saved = repository.save(transaction);
        var history = historyRepository.save(new TransactionStatusHistory(id, TransactionStatus.RECEIVED, "Manual retry #" + transaction.getRetryCount(), transaction.getTraceId()));
        statusStream.publish(history);
        eventProducer.publish(new TransactionCreatedEvent(id, saved.getAmount(), saved.getCurrency(), saved.getStatus(), saved.getCreatedAt(), saved.getTraceId()));
        return saved;
    }
    public Transaction retry(UUID id, String ownerKey) { findById(id, ownerKey); return retry(id); }

    public void markFailedAfterKafkaRetries(TransactionCreatedEvent event, String reason) {
        var transaction = findById(event.transactionId());
        markFailed(transaction, reason);
    }

    private String validationFailure(Transaction transaction) {
        if (!SUPPORTED_CURRENCIES.contains(transaction.getCurrency())) return "Unsupported currency: " + transaction.getCurrency();
        if (transaction.getAmount().compareTo(new java.math.BigDecimal("1000000")) > 0) return "Amount exceeds the transaction processing limit";
        return null;
    }

    private void markFailed(Transaction transaction, String reason) {
        transaction.recordFailure(reason);
        transaction.incrementRetryCount();
        repository.save(transaction);
        var history = historyRepository.save(new TransactionStatusHistory(transaction.getId(), TransactionStatus.FAILED, reason, transaction.getTraceId()));
        statusStream.publish(history);
        log.warn("event=transaction_failed transactionId={} traceId={} reason={} retryCount={}", transaction.getId(), transaction.getTraceId(), reason, transaction.getRetryCount());
    }
}
