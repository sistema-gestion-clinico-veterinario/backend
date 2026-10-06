-- V35: Cartilla de vacunación/desparasitación.
-- La ficha de consulta deja de usar "vacunacion_al_dia"/"desparasitacion_al_dia"
-- y pasa a usar "vacunacion_aplicada"/"desparasitacion_aplicada" (se aplico?) mas
-- observacion_vacunacion y observacion_desparasitacion.
--
-- NOTA: este proyecto corre con spring.jpa.hibernate.ddl-auto=update y flyway
-- deshabilitado. Ejectua este script manualmente ANTES de levantar la app para
-- renombrar columnas sin duplicar. Los bloques DO son idempotentes.

DO $$
BEGIN
    -- Renombrar los booleanos de la ficha
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

    -- Columnas de observación (por tipo)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='observacion_vacunacion') THEN
        ALTER TABLE consulta ADD COLUMN observacion_vacunacion TEXT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='consulta' AND column_name='observacion_desparasitacion') THEN
        ALTER TABLE consulta ADD COLUMN observacion_desparasitacion TEXT;
    END IF;

    -- Vínculo opcional del registro de vacunación/desparasitación con la cita-cobro
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='registro_vacunas' AND column_name='cita_id') THEN
        ALTER TABLE registro_vacunas ADD COLUMN cita_id BIGINT;
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='citas' AND column_name='id') THEN
            ALTER TABLE registro_vacunas
                ADD CONSTRAINT FK_registro_vacunas_cita FOREIGN KEY (cita_id) REFERENCES citas (id);
        END IF;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name='registro_desparasitaciones' AND column_name='cita_id') THEN
        ALTER TABLE registro_desparasitaciones ADD COLUMN cita_id BIGINT;
        IF EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name='citas' AND column_name='id') THEN
            ALTER TABLE registro_desparasitaciones
                ADD CONSTRAINT FK_registro_desparasitaciones_cita FOREIGN KEY (cita_id) REFERENCES citas (id);
        END IF;
    END IF;
END $$;