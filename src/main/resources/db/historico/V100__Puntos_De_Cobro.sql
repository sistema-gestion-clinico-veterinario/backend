CREATE TABLE caja (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    nombre VARCHAR(80) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    dispositivo_token_hash VARCHAR(64),
    dispositivo_info VARCHAR(120),
    dispositivo_vinculado_at TIMESTAMP,
    dispositivo_ultimo_uso_at TIMESTAMP,
    creada_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_caja_nombre_empresa ON caja (company_id, LOWER(nombre));
CREATE UNIQUE INDEX uq_caja_dispositivo_token ON caja (dispositivo_token_hash) WHERE dispositivo_token_hash IS NOT NULL;

INSERT INTO caja (company_id, nombre) SELECT id, 'Caja principal' FROM company;

ALTER TABLE sesion_caja ADD COLUMN IF NOT EXISTS caja_id BIGINT REFERENCES caja(id);
UPDATE sesion_caja s SET caja_id = c.id
FROM caja c
WHERE c.company_id = s.company_id AND s.caja_id IS NULL;

CREATE UNIQUE INDEX uq_sesion_caja_abierta_por_caja ON sesion_caja (caja_id) WHERE estado = 'ABIERTA';
CREATE UNIQUE INDEX uq_sesion_caja_abierta_por_persona ON sesion_caja (abierta_por_usuario_id)
    WHERE estado = 'ABIERTA' AND abierta_por_usuario_id IS NOT NULL;

ALTER TABLE movimiento_caja ADD COLUMN IF NOT EXISTS sesion_caja_id BIGINT REFERENCES sesion_caja(id);
UPDATE movimiento_caja m SET sesion_caja_id = s.id
FROM sesion_caja s
WHERE s.company_id = m.company_id
  AND m.sesion_caja_id IS NULL
  AND m.fecha >= s.abierta_at
  AND (s.cerrada_at IS NULL OR m.fecha <= s.cerrada_at);

CREATE INDEX idx_movimiento_caja_sesion ON movimiento_caja (sesion_caja_id);
