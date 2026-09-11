-- Script manual: archivos_clinicos.fecha_documento (RF-32 / RV-10, ERS v1.4).
-- Aplicar ANTES de levantar la app en un entorno con ddl-auto=update/validate.
-- Idempotente.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='archivos_clinicos' AND column_name='fecha_documento') THEN
        ALTER TABLE archivos_clinicos ADD COLUMN fecha_documento DATE;
    END IF;
END $$;
