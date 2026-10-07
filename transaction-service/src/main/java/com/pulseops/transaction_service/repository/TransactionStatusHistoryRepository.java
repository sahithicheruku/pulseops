package com.pulseops.transaction_service.repository;

import com.pulseops.transaction_service.domain.TransactionStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, UUID> {
    List<TransactionStatusHistory> findByTransactionIdOrderByChangedAtAsc(UUID transactionId);
    void deleteByTransactionIdIn(List<UUID> transactionIds);
}
