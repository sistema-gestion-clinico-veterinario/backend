ALTER TABLE company
    ADD COLUMN IF NOT EXISTS modo_acceso_propietario VARCHAR(32);

UPDATE company
SET modo_acceso_propietario = 'PORTAL_POR_DEFECTO'
WHERE modo_acceso_propietario IS NULL;

ALTER TABLE company
    ALTER COLUMN modo_acceso_propietario SET DEFAULT 'PORTAL_POR_DEFECTO',
    ALTER COLUMN modo_acceso_propietario SET NOT NULL;

ALTER TABLE company
    DROP CONSTRAINT IF EXISTS chk_company_modo_acceso_propietario;

ALTER TABLE company
    ADD CONSTRAINT chk_company_modo_acceso_propietario
    CHECK (modo_acceso_propietario IN ('SOLO_GESTION_INTERNA', 'PORTAL_OPCIONAL', 'PORTAL_POR_DEFECTO'));
