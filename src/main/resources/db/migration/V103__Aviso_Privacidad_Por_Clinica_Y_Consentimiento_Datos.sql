CREATE TABLE aviso_privacidad (
    id             BIGSERIAL PRIMARY KEY,
    company_id     INTEGER      NOT NULL REFERENCES company (id),
    version        INTEGER      NOT NULL,
    contenido      TEXT         NOT NULL,
    contenido_hash VARCHAR(64)  NOT NULL,
    campos         TEXT         NOT NULL,
    vigente_desde  TIMESTAMP    NOT NULL,
    activo         BOOLEAN      NOT NULL,
    creado_en      TIMESTAMP    NOT NULL,
    creado_por     VARCHAR(150),
    CONSTRAINT uq_aviso_privacidad_company_version UNIQUE (company_id, version)
);

CREATE UNIQUE INDEX ux_aviso_privacidad_vigente ON aviso_privacidad (company_id) WHERE activo;

CREATE OR REPLACE FUNCTION aviso_privacidad_inmutable() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los avisos de privacidad no se pueden eliminar';
    END IF;
    IF NEW.company_id IS DISTINCT FROM OLD.company_id
       OR NEW.version IS DISTINCT FROM OLD.version
       OR NEW.contenido IS DISTINCT FROM OLD.contenido
       OR NEW.contenido_hash IS DISTINCT FROM OLD.contenido_hash
       OR NEW.campos IS DISTINCT FROM OLD.campos
       OR NEW.vigente_desde IS DISTINCT FROM OLD.vigente_desde
       OR NEW.creado_en IS DISTINCT FROM OLD.creado_en
       OR NEW.creado_por IS DISTINCT FROM OLD.creado_por THEN
        RAISE EXCEPTION 'Un aviso de privacidad publicado no se puede modificar; publica una versión nueva';
    END IF;
    IF NEW.activo AND NOT OLD.activo THEN
        RAISE EXCEPTION 'Un aviso retirado no se puede volver a activar; publica una versión nueva';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_aviso_privacidad_inmutable
    BEFORE UPDATE OR DELETE ON aviso_privacidad
    FOR EACH ROW EXECUTE FUNCTION aviso_privacidad_inmutable();

CREATE TRIGGER trg_aviso_privacidad_no_truncate
    BEFORE TRUNCATE ON aviso_privacidad
    FOR EACH STATEMENT EXECUTE FUNCTION historial_legal_no_se_vacia();

CREATE TABLE consentimiento_datos (
    id                    BIGSERIAL PRIMARY KEY,
    company_id            INTEGER     NOT NULL REFERENCES company (id),
    usuario_id            INTEGER     NOT NULL REFERENCES usuario (id),
    aviso_id              BIGINT      NOT NULL REFERENCES aviso_privacidad (id),
    aviso_version         INTEGER     NOT NULL,
    contenido_hash        VARCHAR(64) NOT NULL,
    finalidad             VARCHAR(40) NOT NULL,
    estado                VARCHAR(12) NOT NULL,
    canal                 VARCHAR(15) NOT NULL,
    registrado_por        INTEGER REFERENCES usuario (id),
    motivo                VARCHAR(300),
    ip_address            VARCHAR(64),
    user_agent            VARCHAR(255),
    fecha                 TIMESTAMP   NOT NULL,
    CONSTRAINT ck_consentimiento_estado CHECK (estado IN ('OTORGADO', 'RETIRADO')),
    CONSTRAINT ck_consentimiento_enterado CHECK (finalidad <> 'ENTERADO' OR estado = 'OTORGADO')
);

CREATE INDEX idx_consentimiento_usuario_empresa_finalidad
    ON consentimiento_datos (usuario_id, company_id, finalidad, id DESC);
CREATE INDEX idx_consentimiento_company ON consentimiento_datos (company_id);

CREATE OR REPLACE FUNCTION consentimiento_datos_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las constancias de consentimiento no se pueden modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_consentimiento_datos_inmutable
    BEFORE UPDATE OR DELETE ON consentimiento_datos
    FOR EACH ROW EXECUTE FUNCTION consentimiento_datos_inmutable();

CREATE TRIGGER trg_consentimiento_datos_no_truncate
    BEFORE TRUNCATE ON consentimiento_datos
    FOR EACH STATEMENT EXECUTE FUNCTION historial_legal_no_se_vacia();

COMMENT ON TABLE aviso_privacidad IS 'Aviso de privacidad versionado de cada clínica; el texto publicado no cambia, un cambio es una versión nueva';
COMMENT ON COLUMN aviso_privacidad.campos IS 'Datos con los que se compuso el texto (JSON), para precargar la versión siguiente';
COMMENT ON TABLE consentimiento_datos IS 'Constancias, solo de inserción, de que una persona fue informada o decidió sobre una finalidad opcional';
