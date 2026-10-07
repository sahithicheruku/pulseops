package com.pulseops.transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.domain.Transaction;
import com.pulseops.transaction_service.repository.TransactionRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionService service;

    @Test
    void findAllReturnsTransactions() {
        var transactions = List.of(new Transaction());
        when(repository.findAll()).thenReturn(transactions);

        assertEquals(transactions, service.findAll());
        verify(repository).findAll();
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
}
