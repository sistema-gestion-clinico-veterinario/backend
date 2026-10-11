-- Derecho de cancelación (Ley N.° 29733): solicitudes del titular, su verificación de identidad,
-- la resolución de la clínica y la fecha límite de respuesta. No guarda datos personales del titular:
-- solo referencias por identificador.
CREATE TABLE IF NOT EXISTS solicitud_cancelacion_datos (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    canal VARCHAR(16) NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE',
    identidad_verificada BOOLEAN NOT NULL,
    metodo_verificacion VARCHAR(200) NOT NULL,
    motivo_titular VARCHAR(500),
    registrada_por_usuario_id INTEGER,
    creada_at TIMESTAMP NOT NULL,
    fecha_limite DATE NOT NULL,
    resuelta_por_usuario_id INTEGER,
    resuelta_at TIMESTAMP,
    motivo_resolucion VARCHAR(500),
    resumen TEXT,
    CONSTRAINT ck_solicitud_cancelacion_canal CHECK (canal IN ('TITULAR', 'ADMINISTRADOR')),
    CONSTRAINT ck_solicitud_cancelacion_estado CHECK (estado IN ('PENDIENTE', 'EJECUTADA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_cancelacion_resolucion CHECK (
        estado = 'PENDIENTE' OR (resuelta_at IS NOT NULL AND resuelta_por_usuario_id IS NOT NULL))
);

CREATE INDEX IF NOT EXISTS idx_solicitud_cancelacion_company_estado
    ON solicitud_cancelacion_datos (company_id, estado, creada_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_cancelacion_pendiente
    ON solicitud_cancelacion_datos (usuario_id, company_id) WHERE estado = 'PENDIENTE';
