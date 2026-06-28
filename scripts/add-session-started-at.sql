ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS session_started_at TIMESTAMP;
UPDATE refresh_tokens SET session_started_at = NOW() WHERE session_started_at IS NULL;
ALTER TABLE refresh_tokens ALTER COLUMN session_started_at SET NOT NULL;
