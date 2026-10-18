CREATE TABLE IF NOT EXISTS company_compliance_policy (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL REFERENCES company(id),
    control_code VARCHAR(40) NOT NULL,
    version INTEGER NOT NULL,
    habilitado BOOLEAN NOT NULL,
    modo VARCHAR(16) NOT NULL,
    bloqueante BOOLEAN NOT NULL,
    justificacion TEXT NOT NULL,
    referencia_contractual VARCHAR(200),
    confirmo_revision_legal BOOLEAN NOT NULL,
    vigente_desde TIMESTAMP NOT NULL,
    creada_at TIMESTAMP NOT NULL,
    creada_por_usuario_id INTEGER NOT NULL REFERENCES usuario(id),
    CONSTRAINT uq_company_compliance_version UNIQUE (company_id, control_code, version),
    CONSTRAINT ck_company_compliance_control CHECK (control_code IN (
        'AVISO_PRIVACIDAD', 'DERECHOS_DATOS', 'USO_IA', 'RECORDATORIOS_CORREO', 'CONSENTIMIENTO_VETERINARIO'
    )),
    CONSTRAINT ck_company_compliance_modo CHECK (modo IN ('SOFTVET', 'EXTERNO', 'NO_APLICA')),
    CONSTRAINT ck_company_compliance_coherente CHECK (
        (habilitado = FALSE AND modo = 'NO_APLICA' AND bloqueante = FALSE)
        OR (habilitado = TRUE AND modo <> 'NO_APLICA')
    )
);

CREATE INDEX IF NOT EXISTS idx_company_compliance_current
    ON company_compliance_policy(company_id, control_code, vigente_desde DESC, version DESC);

COMMENT ON TABLE company_compliance_policy IS
    'Versiones inmutables de los controles documentales aprobados por plataforma para cada clínica';
