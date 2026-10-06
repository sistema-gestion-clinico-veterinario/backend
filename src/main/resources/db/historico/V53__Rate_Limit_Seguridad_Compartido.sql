CREATE TABLE security_rate_limits (
    rate_key VARCHAR(64) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_security_rate_limits_expires_at
    ON security_rate_limits (expires_at);

COMMENT ON TABLE security_rate_limits IS
    'Contadores compartidos entre instancias para limitar operaciones sensibles de autenticacion.';
COMMENT ON COLUMN security_rate_limits.rate_key IS
    'SHA-256 del tipo de operacion e identificador; no almacena correos ni IP en claro.';
