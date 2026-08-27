ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS jti VARCHAR(36);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS family_id VARCHAR(36);
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS used_at TIMESTAMPTZ;
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_family_active
    ON refresh_tokens (family_id)
    WHERE revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_active
    ON refresh_tokens (usuario_id)
    WHERE revoked_at IS NULL;

-- La columna historica "token" pasa a contener exclusivamente SHA-256 hexadecimal.
-- Los registros anteriores contienen JWT completos y deben invalidarse en el despliegue:
DELETE FROM refresh_tokens WHERE length(token) <> 64;
