ALTER TABLE solicitud_cambio_cumplimiento
    ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS evidencia_referencia VARCHAR(200),
    ADD COLUMN IF NOT EXISTS vigente_desde_solicitada TIMESTAMP,
    ADD COLUMN IF NOT EXISTS revision_por_usuario_id INTEGER REFERENCES usuario(id),
    ADD COLUMN IF NOT EXISTS revision_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS vista_clinica_at TIMESTAMP;

ALTER TABLE solicitud_cambio_cumplimiento
    DROP CONSTRAINT IF EXISTS ck_solicitud_cumplimiento_estado;
ALTER TABLE solicitud_cambio_cumplimiento
    ADD CONSTRAINT ck_solicitud_cumplimiento_estado
    CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'APROBADA', 'RECHAZADA'));

ALTER TABLE solicitud_cambio_cumplimiento
    DROP CONSTRAINT IF EXISTS ck_solicitud_cumplimiento_resolucion;
ALTER TABLE solicitud_cambio_cumplimiento
    ADD CONSTRAINT ck_solicitud_cumplimiento_resolucion CHECK (
        estado IN ('PENDIENTE', 'EN_REVISION')
        OR (resuelta_por_usuario_id IS NOT NULL AND resuelta_at IS NOT NULL)
    );

DROP INDEX IF EXISTS uq_solicitud_cumplimiento_pendiente;
DROP INDEX IF EXISTS uq_solicitud_cumplimiento_activa;
CREATE UNIQUE INDEX uq_solicitud_cumplimiento_activa
    ON solicitud_cambio_cumplimiento(company_id, control_code)
    WHERE estado IN ('PENDIENTE', 'EN_REVISION');

CREATE INDEX IF NOT EXISTS idx_solicitud_cumplimiento_bandeja
    ON solicitud_cambio_cumplimiento(estado, solicitada_at DESC);
