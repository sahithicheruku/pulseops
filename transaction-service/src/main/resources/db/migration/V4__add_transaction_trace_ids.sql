ALTER TABLE transaction ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
UPDATE transaction SET trace_id = id::text WHERE trace_id IS NULL;
ALTER TABLE transaction ALTER COLUMN trace_id SET NOT NULL;
ALTER TABLE transaction_status_history ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64);
UPDATE transaction_status_history history SET trace_id = transaction.trace_id
FROM transaction WHERE transaction.id = history.transaction_id AND history.trace_id IS NULL;
UPDATE transaction_status_history SET trace_id = transaction_id::text WHERE trace_id IS NULL;
ALTER TABLE transaction_status_history ALTER COLUMN trace_id SET NOT NULL;
