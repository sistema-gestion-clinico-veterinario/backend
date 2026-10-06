CREATE TABLE IF NOT EXISTS email_change_requests (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    new_email VARCHAR(254) NOT NULL,
    old_email_token_hash VARCHAR(64) NOT NULL UNIQUE,
    new_email_token_hash VARCHAR(64) NOT NULL UNIQUE,
    old_email_confirmed_at TIMESTAMP,
    new_email_confirmed_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_change_request_user UNIQUE (usuario_id)
);

CREATE INDEX IF NOT EXISTS idx_email_change_requests_expiry
    ON email_change_requests (expires_at);
