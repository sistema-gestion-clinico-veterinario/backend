CREATE TABLE cierre_cuenta (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    company_id INTEGER NOT NULL REFERENCES company(id),
    estado VARCHAR(12) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    empleado_id BIGINT,
    apoderado_id BIGINT,
    cerrada_at TIMESTAMP NOT NULL,
    vence_at TIMESTAMP NOT NULL,
    reactivada_at TIMESTAMP,
    purgada_at TIMESTAMP,
    CONSTRAINT ck_cierre_cuenta_estado CHECK (estado IN ('CERRADA', 'REACTIVADA', 'PURGADA'))
);

CREATE UNIQUE INDEX uq_cierre_cuenta_abierto ON cierre_cuenta (usuario_id, company_id) WHERE estado = 'CERRADA';
CREATE UNIQUE INDEX uq_cierre_cuenta_token ON cierre_cuenta (token_hash);
CREATE INDEX idx_cierre_cuenta_vencimiento ON cierre_cuenta (vence_at) WHERE estado = 'CERRADA';

CREATE TABLE codigo_verificacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    company_id INTEGER REFERENCES company(id),
    proposito VARCHAR(30) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    sal VARCHAR(32) NOT NULL,
    intentos INTEGER NOT NULL DEFAULT 0,
    creado_at TIMESTAMP NOT NULL,
    expira_at TIMESTAMP NOT NULL,
    usado_at TIMESTAMP
);

CREATE INDEX idx_codigo_verificacion_pendiente
    ON codigo_verificacion (usuario_id, company_id, proposito) WHERE usado_at IS NULL;

ALTER TABLE sesion_caja ADD COLUMN IF NOT EXISTS abierta_por_usuario_id INTEGER REFERENCES usuario(id);
