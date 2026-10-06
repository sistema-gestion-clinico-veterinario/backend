CREATE TABLE IF NOT EXISTS thesis_performance_measurements (
    id BIGSERIAL PRIMARY KEY,
    measurement_session_id UUID NOT NULL,
    phase VARCHAR(16) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    operation_name VARCHAR(120) NOT NULL,
    route_template VARCHAR(255) NOT NULL,
    http_method VARCHAR(8) NOT NULL,
    duration_ms NUMERIC(15, 3) NOT NULL,
    http_status INTEGER NOT NULL,
    successful BOOLEAN NOT NULL,
    user_id INTEGER NOT NULL,
    company_id INTEGER NULL,
    CONSTRAINT chk_thesis_perf_phase CHECK (phase IN ('WARMUP', 'SAMPLE')),
    CONSTRAINT chk_thesis_perf_duration CHECK (duration_ms >= 0),
    CONSTRAINT chk_thesis_perf_http_status CHECK (http_status BETWEEN 100 AND 599)
);

CREATE INDEX IF NOT EXISTS idx_thesis_perf_session_user
    ON thesis_performance_measurements (measurement_session_id, user_id);

CREATE INDEX IF NOT EXISTS idx_thesis_perf_requested_at
    ON thesis_performance_measurements (requested_at);

COMMENT ON TABLE thesis_performance_measurements IS
    'Mediciones técnicas controladas para el indicador de tiempo de respuesta de la tesis.';

COMMENT ON COLUMN thesis_performance_measurements.route_template IS
    'Plantilla de ruta sin identificadores ni parámetros de consulta para evitar almacenar datos clínicos.';
