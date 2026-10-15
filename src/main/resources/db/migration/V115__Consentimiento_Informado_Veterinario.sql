-- Documento clinico inmutable por version. La decision queda vinculada al texto
-- exacto mediante SHA-256 y a una persona autorizada para esa mascota.
CREATE TABLE consentimiento_informado_veterinario (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    company_id INTEGER NOT NULL REFERENCES company(id),
    mascota_id BIGINT NOT NULL REFERENCES mascota(id),
    consulta_id BIGINT REFERENCES consulta(id),
    apoderado_id BIGINT NOT NULL REFERENCES apoderado(id),
    creado_por_usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    procedimiento VARCHAR(200) NOT NULL,
    diagnostico_indicacion TEXT NOT NULL,
    descripcion_procedimiento TEXT NOT NULL,
    beneficios_esperados TEXT NOT NULL,
    riesgos_previsibles TEXT NOT NULL,
    alternativas TEXT NOT NULL,
    consecuencias_no_realizar TEXT NOT NULL,
    autorizacion_anestesia BOOLEAN NOT NULL DEFAULT FALSE,
    autorizacion_emergencia BOOLEAN NOT NULL DEFAULT FALSE,
    preguntas_aclaradas BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL,
    contenido TEXT NOT NULL,
    contenido_hash VARCHAR(64) NOT NULL,
    estado VARCHAR(16) NOT NULL DEFAULT 'PENDIENTE',
    creado_at TIMESTAMP NOT NULL,
    decidido_at TIMESTAMP,
    nombre_firmante VARCHAR(200),
    documento_firmante VARCHAR(40),
    relacion_firmante VARCHAR(80),
    declaracion_aceptacion BOOLEAN,
    motivo_rechazo TEXT,
    ip_decision VARCHAR(64),
    dispositivo_decision VARCHAR(255),
    revocado_at TIMESTAMP,
    motivo_revocacion TEXT,
    CONSTRAINT ck_consentimiento_vet_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADO', 'RECHAZADO', 'REVOCADO')),
    CONSTRAINT ck_consentimiento_vet_decision CHECK (
        estado = 'PENDIENTE' OR decidido_at IS NOT NULL
    ),
    CONSTRAINT uq_consentimiento_vet_version UNIQUE (company_id, mascota_id, procedimiento, version)
);

CREATE INDEX idx_consentimiento_vet_company_mascota
    ON consentimiento_informado_veterinario(company_id, mascota_id, creado_at DESC);
CREATE INDEX idx_consentimiento_vet_apoderado_estado
    ON consentimiento_informado_veterinario(apoderado_id, estado, creado_at DESC);
CREATE UNIQUE INDEX uq_consentimiento_vet_pendiente
    ON consentimiento_informado_veterinario(company_id, mascota_id, procedimiento)
    WHERE estado = 'PENDIENTE';

