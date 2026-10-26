package com.pulseops.transaction_service.repository;

import com.pulseops.transaction_service.domain.Transaction;
import java.util.UUID;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pulseops.transaction_service.domain.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    Page<Transaction> findByOwnerKey(String ownerKey, Pageable pageable);
    Page<Transaction> findByOwnerKeyAndStatus(String ownerKey, TransactionStatus status, Pageable pageable);
    Optional<Transaction> findByIdAndOwnerKey(UUID id, String ownerKey);
    long countByOwnerKey(String ownerKey);
    long countByOwnerKeyAndStatus(String ownerKey, TransactionStatus status);
    List<Transaction> findByOwnerKeyAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(String ownerKey, Instant from, Instant to);
    long countByStatus(TransactionStatus status);
    List<Transaction> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);
    List<Transaction> findBySource(String source);
    void deleteBySource(String source);
}
