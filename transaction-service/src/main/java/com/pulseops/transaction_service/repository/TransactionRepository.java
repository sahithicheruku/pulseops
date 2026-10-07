package com.pulseops.transaction_service.repository;

import com.pulseops.transaction_service.domain.Transaction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
