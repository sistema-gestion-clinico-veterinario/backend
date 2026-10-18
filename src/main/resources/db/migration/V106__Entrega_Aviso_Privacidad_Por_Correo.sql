CREATE TABLE IF NOT EXISTS entrega_aviso_privacidad (
    id             BIGSERIAL PRIMARY KEY,
    company_id     INTEGER      NOT NULL REFERENCES company (id),
    usuario_id     INTEGER      NOT NULL REFERENCES usuario (id),
    aviso_id       BIGINT       NOT NULL REFERENCES aviso_privacidad (id),
    canal          VARCHAR(20)  NOT NULL,
    destino        VARCHAR(320) NOT NULL,
    estado         VARCHAR(15)  NOT NULL,
    solicitado_en  TIMESTAMP    NOT NULL,
    enviado_en     TIMESTAMP,
    detalle        VARCHAR(300),
    CONSTRAINT ck_entrega_aviso_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    CONSTRAINT ck_entrega_aviso_canal CHECK (canal IN ('EMAIL')),
    CONSTRAINT uq_entrega_aviso_usuario_version_canal UNIQUE (usuario_id, aviso_id, canal)
);

CREATE INDEX IF NOT EXISTS ix_entrega_aviso_company_estado
    ON entrega_aviso_privacidad (company_id, estado, solicitado_en DESC);

COMMENT ON TABLE entrega_aviso_privacidad IS
    'Evidencia del intento de entrega de cada versión del aviso; no equivale a consentimiento ni a lectura personal';
