-- Migración expansiva: incorpora metadatos de autorización y presentación.
-- Las estructuras legacy se conservan durante la transición para permitir rollback.

ALTER TABLE roles ADD COLUMN IF NOT EXISTS scope VARCHAR(16);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS purpose VARCHAR(32);
ALTER TABLE roles ADD COLUMN IF NOT EXISTS system_managed BOOLEAN;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS protected BOOLEAN;
ALTER TABLE roles ADD COLUMN IF NOT EXISTS permission_version BIGINT;

UPDATE roles
SET scope = CASE
        WHEN name = 'ROLE_SUPER_ADMIN' THEN 'PLATFORM'
        WHEN name IN ('ROLE_APODERADO', 'ROLE_CLIENTE') THEN 'CLIENT'
        ELSE 'STAFF'
    END,
    purpose = CASE
        WHEN name = 'ROLE_SUPER_ADMIN' THEN 'PLATFORM_ADMIN'
        WHEN name = 'ROLE_ADMIN' THEN 'COMPANY_ADMIN'
        WHEN name IN ('ROLE_APODERADO', 'ROLE_CLIENTE') THEN 'CLIENT_PORTAL'
        ELSE 'CUSTOM'
    END,
    system_managed = CASE
        WHEN name IN ('ROLE_SUPER_ADMIN', 'ROLE_ADMIN', 'ROLE_APODERADO', 'ROLE_CLIENTE') THEN TRUE
        ELSE FALSE
    END,
    protected = CASE
        WHEN name IN ('ROLE_SUPER_ADMIN', 'ROLE_ADMIN') THEN TRUE
        ELSE FALSE
    END,
    permission_version = 0
WHERE scope IS NULL
   OR purpose IS NULL
   OR system_managed IS NULL
   OR protected IS NULL
   OR permission_version IS NULL;

ALTER TABLE roles ALTER COLUMN scope SET NOT NULL;
ALTER TABLE roles ALTER COLUMN purpose SET NOT NULL;
ALTER TABLE roles ALTER COLUMN system_managed SET NOT NULL;
ALTER TABLE roles ALTER COLUMN protected SET NOT NULL;
ALTER TABLE roles ALTER COLUMN permission_version SET NOT NULL;

ALTER TABLE roles ALTER COLUMN scope SET DEFAULT 'STAFF';
ALTER TABLE roles ALTER COLUMN purpose SET DEFAULT 'CUSTOM';
ALTER TABLE roles ALTER COLUMN system_managed SET DEFAULT FALSE;
ALTER TABLE roles ALTER COLUMN protected SET DEFAULT FALSE;
ALTER TABLE roles ALTER COLUMN permission_version SET DEFAULT 0;

ALTER TABLE ventanas ADD COLUMN IF NOT EXISTS icono VARCHAR(60);
ALTER TABLE ventanas ADD COLUMN IF NOT EXISTS presentacion_default VARCHAR(16);
UPDATE ventanas SET presentacion_default = 'GROUPED' WHERE presentacion_default IS NULL;
ALTER TABLE ventanas ALTER COLUMN presentacion_default SET NOT NULL;
ALTER TABLE ventanas ALTER COLUMN presentacion_default SET DEFAULT 'GROUPED';

ALTER TABLE vistas ADD COLUMN IF NOT EXISTS audience VARCHAR(16);
UPDATE vistas
SET audience = CASE
        WHEN codigo = 'VISTA_PROFILE' THEN 'SHARED'
        WHEN codigo = 'VISTA_APODERADO_DASHBOARD' OR codigo LIKE 'VISTA_MIS_%' OR codigo = 'VISTA_MI_HISTORIAL'
            THEN 'CLIENT'
        ELSE 'STAFF'
    END
WHERE audience IS NULL;
ALTER TABLE vistas ALTER COLUMN audience SET NOT NULL;
ALTER TABLE vistas ALTER COLUMN audience SET DEFAULT 'STAFF';

INSERT INTO ventanas (codigo, nombre, grupo, orden, activo, presentacion_default)
VALUES ('FACTURACION', 'Facturación', 'facturacion', 6, TRUE, 'GROUPED')
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    orden = EXCLUDED.orden,
    activo = TRUE;

INSERT INTO vistas (codigo, nombre, ruta, grupo, orden, activo, ventana_id, audience)
SELECT catalogo.codigo,
       catalogo.nombre,
       catalogo.ruta,
       catalogo.grupo,
       catalogo.orden,
       TRUE,
       ventana.id,
       'STAFF'
FROM (VALUES
    ('VISTA_CAJA', 'Caja', '/caja', 'FACTURACION', 1, 'FACTURACION'),
    ('VISTA_PAGOS', 'Historial de Pagos', '/pagos', 'FACTURACION', 2, 'FACTURACION'),
    ('VISTA_REPORTES', 'Reportes', '/reportes', 'ADMIN', 6, 'ADMINISTRACION'),
    ('VISTA_LABORATORIO', 'Laboratorio', '/laboratorio', 'CLINICA', 7, 'CLINICA')
) AS catalogo(codigo, nombre, ruta, grupo, orden, ventana_codigo)
JOIN ventanas ventana ON ventana.codigo = catalogo.ventana_codigo
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    ruta = EXCLUDED.ruta,
    orden = EXCLUDED.orden,
    activo = TRUE,
    ventana_id = EXCLUDED.ventana_id,
    audience = EXCLUDED.audience;

CREATE TABLE IF NOT EXISTS rol_ventana_configuracion (
    id SERIAL PRIMARY KEY,
    rol_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    ventana_id INTEGER NOT NULL REFERENCES ventanas(id) ON DELETE CASCADE,
    presentacion VARCHAR(16) NOT NULL DEFAULT 'GROUPED',
    orden INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_rol_ventana_configuracion UNIQUE (rol_id, ventana_id),
    CONSTRAINT ck_rol_ventana_presentacion CHECK (presentacion IN ('GROUPED', 'FLAT')),
    CONSTRAINT ck_rol_ventana_orden CHECK (orden >= 0)
);

CREATE INDEX IF NOT EXISTS idx_rol_vista_permisos_rol_leer
    ON rol_vista_permisos (rol_id, leer);

CREATE INDEX IF NOT EXISTS idx_rol_ventana_configuracion_rol
    ON rol_ventana_configuracion (rol_id);

INSERT INTO rol_ventana_configuracion (rol_id, ventana_id, presentacion, orden)
SELECT DISTINCT permiso.rol_id,
       vista.ventana_id,
       CASE
           WHEN role.scope = 'CLIENT' THEN 'FLAT'
           ELSE COALESCE(ventana.presentacion_default, 'GROUPED')
       END,
       ventana.orden
FROM rol_vista_permisos permiso
JOIN roles role ON role.id = permiso.rol_id
JOIN vistas vista ON vista.id = permiso.vista_id
JOIN ventanas ventana ON ventana.id = vista.ventana_id
WHERE permiso.leer = TRUE
ON CONFLICT (rol_id, ventana_id) DO NOTHING;
