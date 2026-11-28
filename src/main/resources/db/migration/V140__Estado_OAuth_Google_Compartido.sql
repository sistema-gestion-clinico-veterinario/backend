CREATE TABLE IF NOT EXISTS oauth_google_estado (
    tipo VARCHAR(20) NOT NULL,
    clave_hash VARCHAR(64) NOT NULL,
    contenido TEXT NOT NULL,
    expira_en TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tipo, clave_hash)
);

CREATE INDEX IF NOT EXISTS idx_oauth_google_estado_expira_en
    ON oauth_google_estado (expira_en);

COMMENT ON TABLE oauth_google_estado IS
    'Estado de un solo uso del inicio de sesion con Google, compartido entre las instancias del backend. Vive segundos o minutos.';
COMMENT ON COLUMN oauth_google_estado.clave_hash IS
    'SHA-256 del codigo entregado al navegador; el codigo en claro no se guarda.';
COMMENT ON COLUMN oauth_google_estado.contenido IS
    'JSON cifrado con AES-256-GCM (nonce y texto cifrado en Base64); puede contener tokens de sesion.';
