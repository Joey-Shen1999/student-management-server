ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_updated_at TIMESTAMP;

UPDATE users
SET password_updated_at = COALESCE(password_updated_at, updated_at, created_at, NOW());
