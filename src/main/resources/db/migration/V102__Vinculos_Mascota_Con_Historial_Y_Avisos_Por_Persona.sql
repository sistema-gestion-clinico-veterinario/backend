ALTER TABLE mascota_persona_relacion DROP CONSTRAINT IF EXISTS uq_mascota_persona_relacion;
CREATE UNIQUE INDEX IF NOT EXISTS uq_mascota_persona_relacion_activa
    ON mascota_persona_relacion (mascota_id, apoderado_id)
    WHERE activo = TRUE;

ALTER TABLE recordatorio_preventivo DROP CONSTRAINT IF EXISTS uk_recordatorio_control_tipo_fecha;
ALTER TABLE recordatorio_preventivo
    ADD CONSTRAINT uk_recordatorio_control_tipo_fecha_persona
    UNIQUE (control_preventivo_id, tipo_aviso, fecha_programada, apoderado_id);
