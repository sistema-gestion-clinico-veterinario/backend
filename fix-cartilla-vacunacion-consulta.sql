-- Script manual: Ficha de consulta - cartilla de vacunación/desparasitación.
-- Aplicar ANTES de levantar la app (ddl-auto=update no renombra columnas).
-- Idempotente.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='vacunacion_al_dia')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='vacunacion_aplicada') THEN
        ALTER TABLE consulta RENAME COLUMN vacunacion_al_dia TO vacunacion_aplicada;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='desparasitacion_al_dia')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='desparasitacion_aplicada') THEN
        ALTER TABLE consulta RENAME COLUMN desparasitacion_al_dia TO desparasitacion_aplicada;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='observacion_vacunacion') THEN
        ALTER TABLE consulta ADD COLUMN observacion_vacunacion TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='observacion_desparasitacion') THEN
        ALTER TABLE consulta ADD COLUMN observacion_desparasitacion TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='registro_vacunas' AND column_name='cita_id') THEN
        ALTER TABLE registro_vacunas ADD COLUMN cita_id BIGINT;
        ALTER TABLE registro_vacunas ADD CONSTRAINT FK_registro_vacunas_cita
            FOREIGN KEY (cita_id) REFERENCES citas (id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='registro_desparasitaciones' AND column_name='cita_id') THEN
        ALTER TABLE registro_desparasitaciones ADD COLUMN cita_id BIGINT;
        ALTER TABLE registro_desparasitaciones ADD CONSTRAINT FK_registro_desparasitaciones_cita
            FOREIGN KEY (cita_id) REFERENCES citas (id);
    END IF;
END $$;