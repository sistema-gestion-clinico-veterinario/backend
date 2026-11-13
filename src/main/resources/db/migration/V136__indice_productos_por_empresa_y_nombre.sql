CREATE INDEX IF NOT EXISTS idx_producto_company_nombre
    ON producto (company_id, nombre);
