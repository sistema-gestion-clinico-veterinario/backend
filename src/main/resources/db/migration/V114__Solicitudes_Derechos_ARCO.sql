-- Solicitudes de acceso, rectificacion, oposicion y portabilidad. La cancelacion
-- conserva su flujo especializado porque ejecuta anonimizacion y cierre de cuenta.
CREATE TABLE IF NOT EXISTS solicitud_derecho_datos (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    tipo VARCHAR(20) NOT NULL,
    estado VARCHAR(24) NOT NULL DEFAULT 'RECIBIDA',
    descripcion TEXT NOT NULL,
    dato_rectificar TEXT,
    identidad_verificada BOOLEAN NOT NULL DEFAULT TRUE,
    metodo_verificacion VARCHAR(200) NOT NULL,
    creada_at TIMESTAMP NOT NULL,
    fecha_limite DATE NOT NULL,
    actualizada_at TIMESTAMP NOT NULL,
    respuesta TEXT,
    motivo_rechazo TEXT,
    resuelta_at TIMESTAMP,
    resuelta_por_usuario_id INTEGER REFERENCES usuario(id),
    ip_solicitud VARCHAR(64),
    dispositivo_solicitud VARCHAR(255),
    CONSTRAINT ck_solicitud_derecho_tipo CHECK (tipo IN ('ACCESO', 'RECTIFICACION', 'OPOSICION', 'PORTABILIDAD')),
    CONSTRAINT ck_solicitud_derecho_estado CHECK (estado IN ('RECIBIDA', 'EN_REVISION', 'REQUIERE_INFORMACION', 'ATENDIDA', 'RECHAZADA')),
    CONSTRAINT ck_solicitud_derecho_rectificacion CHECK (tipo <> 'RECTIFICACION' OR dato_rectificar IS NOT NULL),
    CONSTRAINT ck_solicitud_derecho_resolucion CHECK (
        estado NOT IN ('ATENDIDA', 'RECHAZADA') OR (resuelta_at IS NOT NULL AND resuelta_por_usuario_id IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_solicitud_derecho_company_estado
    ON solicitud_derecho_datos(company_id, estado, creada_at DESC);
CREATE INDEX IF NOT EXISTS idx_solicitud_derecho_usuario
    ON solicitud_derecho_datos(usuario_id, company_id, creada_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_solicitud_derecho_abierta
    ON solicitud_derecho_datos(usuario_id, company_id, tipo)
    WHERE estado IN ('RECIBIDA', 'EN_REVISION', 'REQUIERE_INFORMACION');

