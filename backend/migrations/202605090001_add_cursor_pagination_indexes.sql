-- +goose Up
CREATE INDEX IF NOT EXISTS idx_users_created_id
    ON users(created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_created_id
    ON transactions(created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_sender_created_id
    ON transactions(sender_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_receiver_created_id
    ON transactions(receiver_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_type_status_created_id
    ON transactions(type, status, created_at DESC, id DESC);

-- +goose Down
DROP INDEX IF EXISTS idx_transactions_type_status_created_id;
DROP INDEX IF EXISTS idx_transactions_receiver_created_id;
DROP INDEX IF EXISTS idx_transactions_sender_created_id;
DROP INDEX IF EXISTS idx_transactions_created_id;
DROP INDEX IF EXISTS idx_users_created_id;
