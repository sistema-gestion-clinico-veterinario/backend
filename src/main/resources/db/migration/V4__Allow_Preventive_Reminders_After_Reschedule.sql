ALTER TABLE recordatorio_preventivo
    DROP CONSTRAINT IF EXISTS uk_recordatorio_control_tipo;

ALTER TABLE recordatorio_preventivo
    ADD CONSTRAINT uk_recordatorio_control_tipo_fecha
    UNIQUE (control_preventivo_id, tipo_aviso, fecha_programada);
