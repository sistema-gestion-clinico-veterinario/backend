CREATE TABLE account_lockouts (
    account_key VARCHAR(64) PRIMARY KEY,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_account_lockouts_locked_until
    ON account_lockouts (locked_until);

COMMENT ON TABLE account_lockouts IS
    'Bloqueo temporal de cuentas tras intentos de login con credenciales invalidas consecutivos.';
COMMENT ON COLUMN account_lockouts.account_key IS
    'SHA-256 del identificador de la cuenta (username/email); no almacena el valor en claro.';
