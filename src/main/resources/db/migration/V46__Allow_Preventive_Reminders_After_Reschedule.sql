-- The former V4 migration was added after V45. It is versioned as V46 so a
-- migration runner executes it in the same order in which the feature evolved.
DO $$
BEGIN
    ALTER TABLE recordatorio_preventivo
        DROP CONSTRAINT IF EXISTS uk_recordatorio_control_tipo;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_recordatorio_control_tipo_fecha'
    ) THEN
        ALTER TABLE recordatorio_preventivo
            ADD CONSTRAINT uk_recordatorio_control_tipo_fecha
            UNIQUE (control_preventivo_id, tipo_aviso, fecha_programada);
    END IF;
END $$;
