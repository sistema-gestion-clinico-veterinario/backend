CREATE TABLE IF NOT EXISTS solicitud_cambio_cumplimiento (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    control_code VARCHAR(50) NOT NULL,
    habilitado_solicitado BOOLEAN NOT NULL,
    modo_solicitado VARCHAR(20) NOT NULL,
    bloqueante_solicitado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    solicitada_por_usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    solicitada_at TIMESTAMP NOT NULL,
    resuelta_por_usuario_id INTEGER REFERENCES usuario(id),
    resuelta_at TIMESTAMP,
    comentario_resolucion TEXT,
    CONSTRAINT ck_solicitud_cumplimiento_control CHECK (control_code IN (
        'AVISO_PRIVACIDAD', 'DERECHOS_DATOS', 'USO_IA',
        'RECORDATORIOS_CORREO', 'CONSENTIMIENTO_VETERINARIO'
    )),
    CONSTRAINT ck_solicitud_cumplimiento_modo CHECK (modo_solicitado IN ('SOFTVET', 'EXTERNO', 'NO_APLICA')),
    CONSTRAINT ck_solicitud_cumplimiento_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_cumplimiento_coherente CHECK (
        (habilitado_solicitado = FALSE AND modo_solicitado = 'NO_APLICA' AND bloqueante_solicitado = FALSE)
        OR
        (habilitado_solicitado = TRUE AND modo_solicitado <> 'NO_APLICA')
    ),
    CONSTRAINT ck_solicitud_cumplimiento_resolucion CHECK (
        estado = 'PENDIENTE' OR (resuelta_por_usuario_id IS NOT NULL AND resuelta_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_cumplimiento_pendiente
    ON solicitud_cambio_cumplimiento(company_id, control_code)
    WHERE estado = 'PENDIENTE';

CREATE INDEX IF NOT EXISTS idx_solicitud_cumplimiento_empresa
    ON solicitud_cambio_cumplimiento(company_id, solicitada_at DESC);

COMMENT ON TABLE solicitud_cambio_cumplimiento IS
    'Solicitudes auditables de una clínica para cambiar cómo gestiona un control de cumplimiento.';
