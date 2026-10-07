ALTER TABLE transaction ADD COLUMN IF NOT EXISTS source VARCHAR(20);
UPDATE transaction SET source = 'USER' WHERE source IS NULL;
ALTER TABLE transaction ALTER COLUMN source SET NOT NULL;
ALTER TABLE transaction ALTER COLUMN source SET DEFAULT 'USER';
CREATE INDEX IF NOT EXISTS idx_transaction_source ON transaction (source);
