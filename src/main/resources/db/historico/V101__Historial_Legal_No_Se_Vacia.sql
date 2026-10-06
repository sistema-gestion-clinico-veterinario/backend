CREATE OR REPLACE FUNCTION historial_legal_no_se_vacia() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'El historial de documentos legales y de aceptaciones no se puede vaciar';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_legal_document_no_truncate
    BEFORE TRUNCATE ON legal_document
    FOR EACH STATEMENT EXECUTE FUNCTION historial_legal_no_se_vacia();

CREATE TRIGGER trg_user_consent_no_truncate
    BEFORE TRUNCATE ON user_consent
    FOR EACH STATEMENT EXECUTE FUNCTION historial_legal_no_se_vacia();
