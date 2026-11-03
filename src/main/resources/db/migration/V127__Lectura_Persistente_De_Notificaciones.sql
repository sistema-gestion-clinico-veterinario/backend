CREATE TABLE IF NOT EXISTS notification_read_state (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    notification_key VARCHAR(120) NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_read_state_user_key UNIQUE (usuario_id, notification_key)
);

CREATE INDEX IF NOT EXISTS idx_notification_read_state_user
    ON notification_read_state (usuario_id);
