package com.pulseops.transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.event.TransactionEventProducer;
import com.pulseops.transaction_service.repository.TransactionRepository;
import com.pulseops.transaction_service.repository.TransactionStatusHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class TransactionOwnershipTest {
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final TransactionService service = new TransactionService(transactions, mock(TransactionEventProducer.class),
            mock(StringRedisTemplate.class), mock(TransactionStatusHistoryRepository.class), mock(TransactionStatusStream.class));

    @Test
    void userCannotReadAnotherUsersTransaction() {
        var id = UUID.randomUUID();
        when(transactions.findByIdAndOwnerKey(id, "user-a")).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.findById(id, "user-a"));
    }

    @Test
    void userCannotRetryAnotherUsersTransaction() {
        var id = UUID.randomUUID();
        when(transactions.findByIdAndOwnerKey(id, "user-a")).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.retry(id, "user-a"));
    }

    @Test
    void userCannotReadAnotherUsersHistory() {
        var id = UUID.randomUUID();
        when(transactions.findByIdAndOwnerKey(id, "user-a")).thenReturn(Optional.empty());
        assertThrows(java.util.NoSuchElementException.class, () -> service.getStatusHistory(id, "user-a"));
    }
}
