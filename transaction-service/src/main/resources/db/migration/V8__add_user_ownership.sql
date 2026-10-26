CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

ALTER TABLE transaction ADD COLUMN IF NOT EXISTS owner_key VARCHAR(320);
UPDATE transaction SET owner_key = 'demo' WHERE owner_key IS NULL;
ALTER TABLE transaction ALTER COLUMN owner_key SET DEFAULT 'demo';
ALTER TABLE transaction ALTER COLUMN owner_key SET NOT NULL;
CREATE INDEX IF NOT EXISTS idx_transaction_owner_created_at ON transaction (owner_key, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_transaction_owner_status_created_at ON transaction (owner_key, status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_user_email_lower ON app_user (LOWER(email));
