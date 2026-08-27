-- Los tokens de recuperacion ahora se almacenan como SHA-256 (64 caracteres).
-- Los enlaces legacy se invalidan deliberadamente al desplegar esta mejora.
DELETE FROM password_reset_tokens
WHERE length(token) <> 64;
