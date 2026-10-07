package com.pulseops.transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.repository.TransactionRepository;
import com.pulseops.transaction_service.event.TransactionEventProducer;
import com.pulseops.transaction_service.event.TransactionCreatedEvent;
import com.pulseops.transaction_service.domain.TransactionStatus;
import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import com.pulseops.transaction_service.repository.TransactionStatusHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository repository;

    @Mock
    private TransactionEventProducer eventProducer;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private TransactionStatusHistoryRepository historyRepository;

    @InjectMocks
    private TransactionService service;

    @Test
    void findAllReturnsTransactions() {
        var transactions = List.of(new Transaction());
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(transactions));

        assertEquals(transactions, service.findAll(PageRequest.of(0, 20), null).getContent());
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void findByIdReturnsTransaction() {
        var id = UUID.randomUUID();
        var transaction = new Transaction();
        when(repository.findById(id)).thenReturn(java.util.Optional.of(transaction));

        assertEquals(transaction, service.findById(id));
        verify(repository).findById(id);
    }

    @Test
    void findByIdThrowsWhenTransactionDoesNotExist() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(java.util.Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.findById(id));
    }

    @Test
    void processCreatedEventAdvancesTransactionToCompleted() {
        var id = UUID.randomUUID();
        var transaction = new Transaction(new BigDecimal("12.50"), "USD", "Coffee");
        when(repository.findById(id)).thenReturn(java.util.Optional.of(transaction));
        var event = new TransactionCreatedEvent(id, transaction.getAmount(), transaction.getCurrency(),
                TransactionStatus.RECEIVED, Instant.now());

        service.processCreatedEvent(event);

        assertEquals(TransactionStatus.COMPLETED, transaction.getStatus());
        verify(repository, org.mockito.Mockito.times(3)).save(transaction);
        verify(historyRepository, org.mockito.Mockito.times(3)).save(any(TransactionStatusHistory.class));
    }

    @Test
    void createReservesIdempotencyKeyAndPublishesOnce() {
        var id = UUID.randomUUID();
        var transaction = new Transaction(id, new BigDecimal("12.50"), "USD", "Coffee");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("transaction:idempotency:coffee-1"), anyString())).thenReturn(true);
        when(repository.save(any(Transaction.class))).thenReturn(transaction);

        assertEquals(transaction, service.create(
                new com.pulseops.transaction_service.dto.CreateTransactionRequest(
                        new BigDecimal("12.50"), "USD", "Coffee"), "coffee-1"));

        verify(repository).save(any(Transaction.class));
        verify(eventProducer).publish(any(TransactionCreatedEvent.class));
    }

    @Test
    void duplicateIdempotencyKeyReturnsExistingTransaction() {
        var id = UUID.randomUUID();
        var transaction = new Transaction(id, new BigDecimal("12.50"), "USD", "Coffee");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("transaction:idempotency:coffee-1"), anyString())).thenReturn(false);
        when(valueOperations.get("transaction:idempotency:coffee-1")).thenReturn(id.toString());
        when(repository.findById(id)).thenReturn(java.util.Optional.of(transaction));

        assertEquals(transaction, service.create(
                new com.pulseops.transaction_service.dto.CreateTransactionRequest(
                        new BigDecimal("12.50"), "USD", "Coffee"), "coffee-1"));

        verify(repository, never()).save(any());
        verify(eventProducer, never()).publish(any());
    }

    @Test
    void returnsStatusHistoryForExistingTransaction() {
        var id = UUID.randomUUID();
        var transaction = new Transaction(id, new BigDecimal("12.50"), "USD", "Coffee");
        var history = List.of(new TransactionStatusHistory(id, TransactionStatus.RECEIVED));
        when(repository.findById(id)).thenReturn(java.util.Optional.of(transaction));
        when(historyRepository.findByTransactionIdOrderByChangedAtAsc(id)).thenReturn(history);

        assertEquals(history, service.getStatusHistory(id));
    }
}
