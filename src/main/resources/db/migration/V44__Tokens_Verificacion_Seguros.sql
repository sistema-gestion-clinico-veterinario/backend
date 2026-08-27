ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS verification_token_expires_at TIMESTAMP;

-- Los valores anteriores eran tokens en texto plano. Se invalidan para que
-- los usuarios pendientes soliciten un enlace nuevo almacenado como SHA-256.
UPDATE usuario
SET verification_token = NULL,
    verification_token_expires_at = NULL
WHERE verification_token IS NOT NULL
  AND length(verification_token) <> 64;
