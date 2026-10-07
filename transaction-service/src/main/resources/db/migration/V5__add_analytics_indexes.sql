CREATE INDEX IF NOT EXISTS idx_transaction_created_at ON transaction (created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_status_last_failure_at ON transaction (status, last_failure_at);
CREATE INDEX IF NOT EXISTS idx_status_history_transaction_status_changed_at
    ON transaction_status_history (transaction_id, status, changed_at);
