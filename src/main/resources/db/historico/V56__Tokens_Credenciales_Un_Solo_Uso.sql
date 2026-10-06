-- Sólo debe existir una solicitud de recuperación vigente por usuario.
DELETE FROM password_reset_tokens older
USING password_reset_tokens newer
WHERE older.usuario_id = newer.usuario_id
  AND older.id < newer.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_password_reset_tokens_usuario
    ON password_reset_tokens (usuario_id);
