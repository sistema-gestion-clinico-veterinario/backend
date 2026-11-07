ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS';

CREATE INDEX IF NOT EXISTS idx_audit_logs_correlation_id
    ON audit_logs (correlation_id);

CREATE TABLE IF NOT EXISTS system_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    category VARCHAR(40) NOT NULL,
    event VARCHAR(80) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    status_code INTEGER,
    duration_ms BIGINT,
    user_email VARCHAR(255),
    company_id INTEGER,
    ip_address VARCHAR(64),
    dispositivo VARCHAR(120),
    error_type VARCHAR(150),
    CONSTRAINT ck_system_logs_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILED', 'DENIED', 'UNKNOWN')),
    CONSTRAINT ck_system_logs_status
        CHECK (status_code IS NULL OR status_code BETWEEN 100 AND 599),
    CONSTRAINT ck_system_logs_duration
        CHECK (duration_ms IS NULL OR duration_ms >= 0)
);

CREATE INDEX IF NOT EXISTS idx_system_logs_timestamp
    ON system_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_system_logs_correlation
    ON system_logs (correlation_id);
CREATE INDEX IF NOT EXISTS idx_system_logs_company_timestamp
    ON system_logs (company_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_system_logs_status_timestamp
    ON system_logs (status_code, timestamp DESC);

CREATE OR REPLACE FUNCTION softvet_reject_system_log_mutation()
RETURNS trigger AS $$
BEGIN
    IF current_setting('softvet.log_maintenance', true) IS DISTINCT FROM 'on' THEN
        RAISE EXCEPTION 'Los registros técnicos son inmutables';
    END IF;
    IF TG_OP = 'UPDATE' THEN
        RETURN NEW;
    END IF;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_system_logs_no_update ON system_logs;
CREATE TRIGGER trg_system_logs_no_update
    BEFORE UPDATE ON system_logs
    FOR EACH ROW EXECUTE FUNCTION softvet_reject_system_log_mutation();

DROP TRIGGER IF EXISTS trg_system_logs_no_delete ON system_logs;
CREATE TRIGGER trg_system_logs_no_delete
    BEFORE DELETE ON system_logs
    FOR EACH ROW EXECUTE FUNCTION softvet_reject_system_log_mutation();

COMMENT ON TABLE system_logs IS
    'Trazabilidad técnica sin cuerpos, credenciales, tokens ni parámetros de consulta';
COMMENT ON COLUMN system_logs.correlation_id IS
    'Identificador que permite relacionar la solicitud técnica con la auditoría funcional';
