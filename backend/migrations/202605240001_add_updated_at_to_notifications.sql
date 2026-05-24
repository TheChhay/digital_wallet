-- +goose Up
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

-- +goose Down
ALTER TABLE notifications
    DROP COLUMN IF EXISTS updated_at;
