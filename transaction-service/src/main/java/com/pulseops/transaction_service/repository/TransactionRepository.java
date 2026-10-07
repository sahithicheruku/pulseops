package com.pulseops.transaction_service.repository;

import com.pulseops.transaction_service.domain.Transaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.pulseops.transaction_service.domain.TransactionStatus;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
}
