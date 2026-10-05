ALTER TABLE legal_document ADD COLUMN IF NOT EXISTS contenido_hash VARCHAR(64);
UPDATE legal_document
SET contenido_hash = encode(sha256(convert_to(contenido, 'UTF8')), 'hex')
WHERE contenido_hash IS NULL;
ALTER TABLE legal_document ALTER COLUMN contenido_hash SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_legal_document_tipo_version ON legal_document (tipo, version);

ALTER TABLE user_consent ADD COLUMN IF NOT EXISTS documento_tipo VARCHAR(30);
ALTER TABLE user_consent ADD COLUMN IF NOT EXISTS documento_version VARCHAR(20);
ALTER TABLE user_consent ADD COLUMN IF NOT EXISTS contenido_hash VARCHAR(64);
ALTER TABLE user_consent ADD COLUMN IF NOT EXISTS texto_recuperable BOOLEAN NOT NULL DEFAULT TRUE;

DELETE FROM user_consent duplicada
USING user_consent primera
WHERE duplicada.usuario_id = primera.usuario_id
  AND duplicada.legal_document_id = primera.legal_document_id
  AND duplicada.id > primera.id;

UPDATE user_consent uc
SET documento_tipo = ld.tipo,
    documento_version = ld.version,
    texto_recuperable = FALSE
FROM legal_document ld
WHERE ld.id = uc.legal_document_id
  AND uc.documento_tipo IS NULL;

ALTER TABLE user_consent ALTER COLUMN documento_tipo SET NOT NULL;
ALTER TABLE user_consent ALTER COLUMN documento_version SET NOT NULL;
ALTER TABLE user_consent
    ADD CONSTRAINT ck_user_consent_huella
    CHECK (texto_recuperable = FALSE OR contenido_hash IS NOT NULL);
ALTER TABLE user_consent
    ADD CONSTRAINT uq_user_consent_usuario_documento UNIQUE (usuario_id, legal_document_id);

CREATE OR REPLACE FUNCTION legal_document_inmutable() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Los documentos legales no se pueden eliminar';
    END IF;
    IF NEW.tipo IS DISTINCT FROM OLD.tipo
       OR NEW.version IS DISTINCT FROM OLD.version
       OR NEW.contenido IS DISTINCT FROM OLD.contenido
       OR NEW.contenido_hash IS DISTINCT FROM OLD.contenido_hash
       OR NEW.vigente_desde IS DISTINCT FROM OLD.vigente_desde
       OR NEW.creado_en IS DISTINCT FROM OLD.creado_en THEN
        RAISE EXCEPTION 'Un documento legal publicado no se puede modificar; publica una versión nueva';
    END IF;
    IF NEW.activo AND NOT OLD.activo THEN
        RAISE EXCEPTION 'Una versión retirada no se puede volver a activar; publica una versión nueva';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_legal_document_inmutable
    BEFORE UPDATE OR DELETE ON legal_document
    FOR EACH ROW EXECUTE FUNCTION legal_document_inmutable();

CREATE OR REPLACE FUNCTION user_consent_inmutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Las constancias de aceptación no se pueden modificar ni eliminar';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_consent_inmutable
    BEFORE UPDATE OR DELETE ON user_consent
    FOR EACH ROW EXECUTE FUNCTION user_consent_inmutable();

COMMENT ON COLUMN legal_document.contenido_hash IS 'SHA-256 (hex) del contenido publicado; el contenido no cambia una vez publicado';
COMMENT ON COLUMN user_consent.contenido_hash IS 'Huella del texto aceptado; nula si la constancia es anterior a la versión inmutable (texto_recuperable = false)';
