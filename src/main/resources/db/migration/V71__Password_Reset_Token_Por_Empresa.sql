-- password_reset_tokens pasa de ser "un token pendiente por usuario" a "un token
-- pendiente por (usuario, empresa)", porque cada empresa ahora tiene su propia
-- credencial (ver V70). Restablecer la contraseña de la Empresa A no debe cancelar
-- un reset ya pendiente para la Empresa B.

ALTER TABLE password_reset_tokens ADD COLUMN company_id INTEGER REFERENCES company(id);

DROP INDEX IF EXISTS uk_password_reset_tokens_usuario;

CREATE UNIQUE INDEX uk_password_reset_tokens_usuario_empresa
    ON password_reset_tokens (usuario_id, company_id)
    WHERE company_id IS NOT NULL;

CREATE UNIQUE INDEX uk_password_reset_tokens_usuario_global
    ON password_reset_tokens (usuario_id)
    WHERE company_id IS NULL;
