CREATE INDEX IF NOT EXISTS idx_purchases_empresa_tipo_fecha
    ON purchases (company_id, tipo_purchase, created_at DESC);
