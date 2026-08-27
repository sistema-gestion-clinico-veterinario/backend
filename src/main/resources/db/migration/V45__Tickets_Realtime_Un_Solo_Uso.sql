CREATE TABLE IF NOT EXISTS realtime_tickets (
    jti VARCHAR(36) PRIMARY KEY,
    usuario_id INTEGER NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_realtime_tickets_expires_at
    ON realtime_tickets (expires_at);
