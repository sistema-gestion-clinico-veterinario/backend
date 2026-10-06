CREATE TABLE legal_document (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    version VARCHAR(20) NOT NULL,
    contenido TEXT NOT NULL,
    vigente_desde TIMESTAMP NOT NULL DEFAULT now(),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_en TIMESTAMP NOT NULL DEFAULT now()
);

-- Solo puede haber un documento activo (vigente) por tipo a la vez.
CREATE UNIQUE INDEX ux_legal_document_tipo_activo
    ON legal_document (tipo)
    WHERE activo;

CREATE TABLE user_consent (
    id BIGSERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuario (id),
    legal_document_id BIGINT NOT NULL REFERENCES legal_document (id),
    fecha_aceptacion TIMESTAMP NOT NULL DEFAULT now(),
    ip_address VARCHAR(64),
    user_agent VARCHAR(255)
);

CREATE INDEX idx_user_consent_usuario_id ON user_consent (usuario_id);
CREATE INDEX idx_user_consent_legal_document_id ON user_consent (legal_document_id);

COMMENT ON TABLE legal_document IS 'Versiones vigentes e históricas de Términos y Condiciones / Política de Privacidad del sistema (documentos globales del SaaS)';
COMMENT ON TABLE user_consent IS 'Registro auditable e inmutable de aceptación de un documento legal por usuario; nunca se actualiza, solo se inserta';

-- Semilla: versión inicial 1.0 de ambos documentos, activa desde ya.
-- El contenido es un borrador provisional: debe ser reemplazado por el texto
-- validado legalmente antes de publicarse a producción con usuarios reales.
INSERT INTO legal_document (tipo, version, contenido, vigente_desde, activo) VALUES
    ('TERMINOS_Y_CONDICIONES', '1.0', 'Borrador provisional de Términos y Condiciones. Pendiente de validación legal.', now(), true),
    ('POLITICA_PRIVACIDAD', '1.0', 'Borrador provisional de Política de Privacidad. Pendiente de validación legal.', now(), true);
