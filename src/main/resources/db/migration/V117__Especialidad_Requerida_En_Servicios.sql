-- La entidad ServiciosVeterinarios referencia opcionalmente la especialidad
-- necesaria para prestar un servicio. La relación es nullable para conservar
-- los servicios históricos que solo definían un tipo de empleado.
ALTER TABLE servicios
    ADD COLUMN IF NOT EXISTS especialidad_requerida_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'fk_servicios_especialidad_requerida'
           AND conrelid = 'servicios'::regclass
    ) THEN
        ALTER TABLE servicios
            ADD CONSTRAINT fk_servicios_especialidad_requerida
            FOREIGN KEY (especialidad_requerida_id)
            REFERENCES especialidad(id)
            ON DELETE SET NULL;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_servicios_especialidad_requerida
    ON servicios (especialidad_requerida_id);
