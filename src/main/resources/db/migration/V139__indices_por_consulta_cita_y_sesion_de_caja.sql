CREATE INDEX IF NOT EXISTS idx_diagnosticos_consulta_id
    ON diagnosticos (consulta_id);

CREATE INDEX IF NOT EXISTS idx_tratamientos_consulta_id
    ON tratamientos (consulta_id);

CREATE INDEX IF NOT EXISTS idx_archivos_clinicos_consulta_id
    ON archivos_clinicos (consulta_id);

CREATE INDEX IF NOT EXISTS idx_detalle_cuenta_cita_cita_id
    ON detalle_cuenta_cita (cita_id);

CREATE INDEX IF NOT EXISTS idx_purchases_cita_id
    ON purchases (cita_id);

CREATE INDEX IF NOT EXISTS idx_sesion_caja_empresa_fecha
    ON sesion_caja (company_id, abierta_at DESC);
