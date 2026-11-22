CREATE INDEX IF NOT EXISTS idx_consentimiento_usuario_company_finalidad
    ON consentimiento_datos (usuario_id, company_id, finalidad, id DESC);
ANALYZE consentimiento_datos;
