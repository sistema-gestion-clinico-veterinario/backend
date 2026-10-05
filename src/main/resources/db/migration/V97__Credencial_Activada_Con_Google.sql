ALTER TABLE usuario_empresa_credencial
    ADD COLUMN IF NOT EXISTS activada_con_google BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE usuario_empresa_credencial c
SET activada_con_google = TRUE
FROM usuario u
WHERE u.id = c.usuario_id
  AND c.password_changed = FALSE
  AND EXISTS (
      SELECT 1
      FROM audit_logs a
      WHERE a.action = 'ACTIVAR_CUENTA_GOOGLE'
        AND LOWER(a.user_email) = LOWER(u.email)
        AND a.company_id IS NOT DISTINCT FROM c.company_id
  );

COMMENT ON COLUMN usuario_empresa_credencial.activada_con_google IS 'La cuenta se activó con Google sin crear contraseña; no se le pide cambiar una contraseña que nunca tuvo';
