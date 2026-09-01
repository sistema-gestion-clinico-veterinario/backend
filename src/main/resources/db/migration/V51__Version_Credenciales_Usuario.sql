ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS credentials_version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN usuario.credentials_version IS
    'Versión de seguridad que invalida access y refresh tokens después de eventos críticos';
