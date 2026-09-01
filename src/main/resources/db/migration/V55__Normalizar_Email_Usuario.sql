DO $$
BEGIN
    IF EXISTS (
        SELECT LOWER(BTRIM(email))
        FROM usuario
        GROUP BY LOWER(BTRIM(email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Existen correos duplicados al ignorar mayusculas; deben resolverse antes de continuar';
    END IF;
END $$;

UPDATE usuario
SET email = LOWER(BTRIM(email))
WHERE email <> LOWER(BTRIM(email));

CREATE UNIQUE INDEX IF NOT EXISTS uk_usuario_email_normalized
    ON usuario (LOWER(email));
