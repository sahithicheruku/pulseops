ALTER TABLE transaction ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS retry_count INTEGER;
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS last_failure_at TIMESTAMPTZ;
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
ALTER TABLE transaction ADD COLUMN IF NOT EXISTS source VARCHAR(20);

UPDATE transaction
SET retry_count = 0
WHERE retry_count IS NULL;

UPDATE transaction
SET trace_id = id::text
WHERE trace_id IS NULL;

UPDATE transaction
SET source = 'USER'
WHERE source IS NULL;

ALTER TABLE transaction ALTER COLUMN retry_count SET DEFAULT 0;
ALTER TABLE transaction ALTER COLUMN retry_count SET NOT NULL;
ALTER TABLE transaction ALTER COLUMN trace_id SET NOT NULL;
ALTER TABLE transaction ALTER COLUMN source SET DEFAULT 'USER';
ALTER TABLE transaction ALTER COLUMN source SET NOT NULL;

ALTER TABLE transaction_status_history ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);
ALTER TABLE transaction_status_history ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);

UPDATE transaction_status_history history
SET trace_id = transaction.trace_id
FROM transaction
WHERE history.transaction_id = transaction.id
  AND history.trace_id IS NULL;

UPDATE transaction_status_history
SET trace_id = transaction_id::text
WHERE trace_id IS NULL;

ALTER TABLE transaction_status_history ALTER COLUMN trace_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transaction_source ON transaction (source);
CREATE INDEX IF NOT EXISTS idx_transaction_status_last_failure_at
    ON transaction (status, last_failure_at);
