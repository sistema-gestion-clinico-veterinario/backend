ALTER TABLE user_consent
    ADD COLUMN IF NOT EXISTS tipo_constancia VARCHAR(25);

-- Las constancias son inmutables durante la operación normal. El trigger se
-- suspende únicamente dentro de esta transacción para clasificar el historial.
ALTER TABLE user_consent
    DISABLE TRIGGER trg_user_consent_inmutable;

UPDATE user_consent
SET tipo_constancia = CASE
    WHEN documento_tipo = 'TERMINOS_Y_CONDICIONES' THEN 'ACEPTACION'
    ELSE 'CONSTANCIA_LECTURA'
END
WHERE tipo_constancia IS NULL;

ALTER TABLE user_consent
    ENABLE TRIGGER trg_user_consent_inmutable;

ALTER TABLE user_consent
    ALTER COLUMN tipo_constancia SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_user_consent_tipo_constancia'
          AND conrelid = 'user_consent'::regclass
    ) THEN
        ALTER TABLE user_consent
            ADD CONSTRAINT ck_user_consent_tipo_constancia
                CHECK (tipo_constancia IN ('ACEPTACION', 'CONSTANCIA_LECTURA'));
    END IF;
END
$$;

COMMENT ON COLUMN user_consent.tipo_constancia IS
    'ACEPTACION para términos contractuales; CONSTANCIA_LECTURA para documentos informativos';
