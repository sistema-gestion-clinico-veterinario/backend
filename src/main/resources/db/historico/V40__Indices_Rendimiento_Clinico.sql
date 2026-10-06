ALTER TABLE control_preventivo ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE registro_vacunas ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE registro_desparasitaciones ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_citas_mascota_fecha
    ON citas (mascota_id, fecha_hora_inicio DESC);

CREATE INDEX IF NOT EXISTS idx_citas_empleado_fecha
    ON citas (empleado_id, fecha_hora_inicio);

CREATE INDEX IF NOT EXISTS idx_citas_fecha_estado_activa
    ON citas (fecha_hora_inicio, estado)
    WHERE eliminada = false;

CREATE INDEX IF NOT EXISTS idx_registro_vacunas_historia_fecha
    ON registro_vacunas (historia_clinica_id, fecha_aplicacion DESC);

CREATE INDEX IF NOT EXISTS idx_registro_vacunas_proxima_activa
    ON registro_vacunas (fecha_proxima_dosis)
    WHERE activo = true AND fecha_proxima_dosis IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_registro_desparasitaciones_historia_fecha
    ON registro_desparasitaciones (historia_clinica_id, fecha_aplicacion DESC);

CREATE INDEX IF NOT EXISTS idx_registro_desparasitaciones_proxima_activa
    ON registro_desparasitaciones (fecha_proxima_aplicacion)
    WHERE activo = true AND fecha_proxima_aplicacion IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_control_preventivo_mascota_estado_fecha
    ON control_preventivo (mascota_id, estado, fecha_recomendada);
