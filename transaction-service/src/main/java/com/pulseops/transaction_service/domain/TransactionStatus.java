package com.pulseops.transaction_service.domain;

public enum TransactionStatus {
    RECEIVED,
    VALIDATING,
    PROCESSING,
    COMPLETED,
    FAILED
}
