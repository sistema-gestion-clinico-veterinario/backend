ALTER TABLE citas
    ADD COLUMN IF NOT EXISTS business_trace_id VARCHAR(50);

UPDATE citas
SET business_trace_id = 'ATT-' || UPPER(SUBSTRING(MD5(id::text || '-' || COALESCE(created_at::text, '') || '-' || RANDOM()::text), 1, 24))
WHERE business_trace_id IS NULL OR BTRIM(business_trace_id) = '';

ALTER TABLE citas
    ALTER COLUMN business_trace_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_citas_business_trace_id
    ON citas (business_trace_id);

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS business_trace_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(60),
    ADD COLUMN IF NOT EXISTS entity_id VARCHAR(80),
    ADD COLUMN IF NOT EXISTS previous_value VARCHAR(120),
    ADD COLUMN IF NOT EXISTS new_value VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_audit_logs_business_trace
    ON audit_logs (business_trace_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON audit_logs (entity_type, entity_id, timestamp);

ALTER TABLE purchases
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100),
    ADD COLUMN IF NOT EXISTS business_trace_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS receipt_status VARCHAR(16) NOT NULL DEFAULT 'NOT_TRACKED',
    ADD COLUMN IF NOT EXISTS receipt_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS receipt_error VARCHAR(500),
    ADD COLUMN IF NOT EXISTS receipt_generated_at TIMESTAMPTZ;

UPDATE purchases purchase
SET company_id = owner.company_id,
    business_trace_id = appointment.business_trace_id
FROM citas appointment
JOIN mascota pet ON pet.id = appointment.mascota_id
JOIN apoderado owner ON owner.id = pet.apoderado_id
WHERE purchase.cita_id = appointment.id
  AND (purchase.company_id IS NULL OR purchase.business_trace_id IS NULL);

CREATE UNIQUE INDEX IF NOT EXISTS uk_purchases_company_idempotency
    ON purchases (company_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_purchases_business_trace
    ON purchases (business_trace_id);
CREATE INDEX IF NOT EXISTS idx_purchases_receipt_pending
    ON purchases (company_id, receipt_status, created_at)
    WHERE receipt_status = 'PENDING';

ALTER TABLE purchases
    ALTER COLUMN receipt_status SET DEFAULT 'PENDING';

ALTER TABLE purchases
    DROP CONSTRAINT IF EXISTS ck_purchases_receipt_status;
ALTER TABLE purchases
    ADD CONSTRAINT ck_purchases_receipt_status
    CHECK (receipt_status IN ('NOT_TRACKED', 'PENDING', 'GENERATED'));
ALTER TABLE purchases
    DROP CONSTRAINT IF EXISTS ck_purchases_receipt_attempts;
ALTER TABLE purchases
    ADD CONSTRAINT ck_purchases_receipt_attempts
    CHECK (receipt_attempts >= 0);

COMMENT ON COLUMN citas.business_trace_id IS
    'Identificador estable que relaciona cita, consulta, pago, caja, comprobante y auditoría.';
COMMENT ON COLUMN audit_logs.business_trace_id IS
    'Identificador de atención; es distinto del correlation_id técnico de una petición HTTP.';
COMMENT ON COLUMN purchases.idempotency_key IS
    'Clave enviada por el cliente para impedir cobros duplicados por reintentos o doble clic.';
