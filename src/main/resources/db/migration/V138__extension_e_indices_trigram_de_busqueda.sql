CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_mascota_nombre_trgm
    ON mascota USING gin (lower((nombre_completo)::text) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_usuario_nombre_completo_trgm
    ON usuario USING gin (lower((((nombre)::text || ' '::text) || (apellido)::text)) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_usuario_email_trgm
    ON usuario USING gin (lower((email)::text) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_prescripciones_medicamento_trgm
    ON prescripciones USING gin (lower((medicamento)::text) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_prescripciones_principio_activo_trgm
    ON prescripciones USING gin (lower((principio_activo)::text) gin_trgm_ops);
