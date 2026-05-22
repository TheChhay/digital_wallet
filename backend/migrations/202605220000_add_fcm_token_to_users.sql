-- +goose Up
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(500);
CREATE INDEX IF NOT EXISTS idx_users_fcm_token ON users(fcm_token) WHERE fcm_token IS NOT NULL;

-- +goose Down
DROP INDEX IF EXISTS idx_users_fcm_token;
ALTER TABLE users DROP COLUMN IF EXISTS fcm_token;
