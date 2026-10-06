-- Suspender (temporal) y dar de baja (fin de la relación) son acciones distintas sobre una
-- persona dentro de una empresa. Ambas dejan estado=false; el tipo registra cuál fue.
ALTER TABLE empleado ADD COLUMN IF NOT EXISTS tipo_inactividad VARCHAR(16);
ALTER TABLE apoderado ADD COLUMN IF NOT EXISTS tipo_inactividad VARCHAR(16);

-- Hasta ahora "desactivar" era la única acción y equivalía a una baja.
UPDATE empleado SET tipo_inactividad = 'BAJA' WHERE estado = FALSE AND tipo_inactividad IS NULL;
UPDATE apoderado SET tipo_inactividad = 'BAJA' WHERE estado = FALSE AND tipo_inactividad IS NULL;

ALTER TABLE empleado
    ADD CONSTRAINT ck_empleado_tipo_inactividad
    CHECK (tipo_inactividad IS NULL OR (estado = FALSE AND tipo_inactividad IN ('SUSPENSION', 'BAJA')));
ALTER TABLE apoderado
    ADD CONSTRAINT ck_apoderado_tipo_inactividad
    CHECK (tipo_inactividad IS NULL OR (estado = FALSE AND tipo_inactividad IN ('SUSPENSION', 'BAJA')));


DO $$
DECLARE restriccion RECORD;
BEGIN
    FOR restriccion IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'mascota'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%motivo_baja%'
    LOOP
        EXECUTE format('ALTER TABLE mascota DROP CONSTRAINT %I', restriccion.conname);
    END LOOP;
END $$;

ALTER TABLE mascota ALTER COLUMN motivo_baja TYPE VARCHAR(40);
