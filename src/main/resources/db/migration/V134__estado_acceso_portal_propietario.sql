ALTER TABLE apoderado
    ADD COLUMN IF NOT EXISTS estado_acceso_portal VARCHAR(32),
    ADD COLUMN IF NOT EXISTS acceso_portal_modificado_en TIMESTAMP,
    ADD COLUMN IF NOT EXISTS acceso_portal_modificado_por VARCHAR(160);

UPDATE apoderado a
SET estado_acceso_portal = CASE
    WHEN c.modo_acceso_propietario = 'SOLO_GESTION_INTERNA' THEN 'REVOCADO'
    WHEN EXISTS (
        SELECT 1
        FROM usuario_por_rol upr
        JOIN rol r ON r.id = upr.rol_id
        WHERE upr.usuario_id = a.user_id
          AND upr.company_id = a.company_id
          AND r.purpose = 'CLIENT_PORTAL'
          AND r.activo = TRUE
    ) AND EXISTS (
        SELECT 1
        FROM usuario_empresa_credencial uec
        WHERE uec.usuario_id = a.user_id
          AND uec.company_id = a.company_id
          AND (uec.password_changed = TRUE OR uec.activada_con_google = TRUE)
    ) THEN 'ACTIVO'
    WHEN EXISTS (
        SELECT 1
        FROM usuario_por_rol upr
        JOIN rol r ON r.id = upr.rol_id
        WHERE upr.usuario_id = a.user_id
          AND upr.company_id = a.company_id
          AND r.purpose = 'CLIENT_PORTAL'
          AND r.activo = TRUE
    ) THEN 'INVITACION_PENDIENTE'
    ELSE 'SIN_ACCESO'
END
FROM company c
WHERE c.id = a.company_id
  AND a.estado_acceso_portal IS NULL;

UPDATE apoderado
SET estado_acceso_portal = 'SIN_ACCESO'
WHERE estado_acceso_portal IS NULL;

ALTER TABLE apoderado
    ALTER COLUMN estado_acceso_portal SET DEFAULT 'SIN_ACCESO',
    ALTER COLUMN estado_acceso_portal SET NOT NULL;

ALTER TABLE apoderado DROP CONSTRAINT IF EXISTS ck_apoderado_estado_acceso_portal;
ALTER TABLE apoderado
    ADD CONSTRAINT ck_apoderado_estado_acceso_portal
    CHECK (estado_acceso_portal IN ('SIN_ACCESO', 'INVITACION_PENDIENTE', 'ACTIVO', 'SUSPENDIDO', 'REVOCADO'));
