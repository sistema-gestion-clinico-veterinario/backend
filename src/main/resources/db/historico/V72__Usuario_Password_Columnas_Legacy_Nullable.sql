-- password/password_changed/credentials_version en `usuario` ya no se usan (ver V70:
-- usuario_empresa_credencial es la fuente real ahora) y la entidad JPA dejo de
-- mapearlos. Se relaja NOT NULL en vez de borrar las columnas: los datos existentes
-- quedan intactos como respaldo, pero un INSERT nuevo (que ya no envia estos campos)
-- deja de fallar por la restriccion. Eliminarlas del todo queda para una migracion
-- posterior, una vez confirmado en produccion que nada las necesita.

ALTER TABLE usuario ALTER COLUMN password DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN password_changed DROP NOT NULL;
ALTER TABLE usuario ALTER COLUMN credentials_version DROP NOT NULL;
