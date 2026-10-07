CREATE TABLE IF NOT EXISTS transaction (
    id UUID PRIMARY KEY,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS transaction_status_history (
    id UUID PRIMARY KEY,
    transaction_id UUID,
    status VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_transaction_status_created_at
    ON transaction (status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transaction_created_at
    ON transaction (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_transaction_status_history_transaction_changed_at
    ON transaction_status_history (transaction_id, changed_at ASC);
