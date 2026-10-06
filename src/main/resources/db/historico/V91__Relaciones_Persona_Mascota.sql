CREATE TABLE IF NOT EXISTS mascota_persona_relacion (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    mascota_id BIGINT NOT NULL REFERENCES mascota(id),
    apoderado_id BIGINT NOT NULL REFERENCES apoderado(id),
    company_id INTEGER NOT NULL REFERENCES company(id),
    tipo_relacion VARCHAR(40) NOT NULL,
    puede_recibir_informacion BOOLEAN NOT NULL DEFAULT FALSE,
    puede_autorizar_atencion BOOLEAN NOT NULL DEFAULT FALSE,
    puede_realizar_pagos BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_inicio DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin DATE,
    observaciones VARCHAR(500),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(150) NOT NULL DEFAULT 'MIGRACION',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(150) NOT NULL DEFAULT 'MIGRACION',
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(150),
    CONSTRAINT uq_mascota_persona_relacion UNIQUE (mascota_id, apoderado_id),
    CONSTRAINT uq_mascota_persona_relacion_uuid UNIQUE (uuid),
    CONSTRAINT ck_mascota_persona_relacion_tipo CHECK (tipo_relacion IN (
        'PROPIETARIO_PRINCIPAL', 'COPROPIETARIO', 'REPRESENTANTE_AUTORIZADO', 'RESPONSABLE_PAGO'
    )),
    CONSTRAINT ck_mascota_persona_relacion_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio)
);

CREATE INDEX IF NOT EXISTS idx_mascota_persona_relacion_mascota
    ON mascota_persona_relacion (mascota_id, activo);
CREATE INDEX IF NOT EXISTS idx_mascota_persona_relacion_apoderado
    ON mascota_persona_relacion (apoderado_id, activo);
CREATE INDEX IF NOT EXISTS idx_mascota_persona_relacion_company
    ON mascota_persona_relacion (company_id, activo);

INSERT INTO mascota_persona_relacion (
    uuid, mascota_id, apoderado_id, company_id, tipo_relacion,
    puede_recibir_informacion, puede_autorizar_atencion, puede_realizar_pagos,
    fecha_inicio, activo, created_at, created_by, updated_at, updated_by
)
SELECT
    gen_random_uuid()::text,
    m.id,
    m.apoderado_id,
    a.company_id,
    'PROPIETARIO_PRINCIPAL',
    TRUE,
    TRUE,
    TRUE,
    COALESCE(m.created_at::date, CURRENT_DATE),
    TRUE,
    COALESCE(m.created_at, CURRENT_TIMESTAMP),
    'MIGRACION',
    COALESCE(m.updated_at, CURRENT_TIMESTAMP),
    'MIGRACION'
FROM mascota m
JOIN apoderado a ON a.id = m.apoderado_id
WHERE a.company_id IS NOT NULL
ON CONFLICT (mascota_id, apoderado_id) DO NOTHING;

CREATE UNIQUE INDEX IF NOT EXISTS uq_mascota_propietario_principal_activo
    ON mascota_persona_relacion (mascota_id)
    WHERE tipo_relacion = 'PROPIETARIO_PRINCIPAL' AND activo = TRUE;
