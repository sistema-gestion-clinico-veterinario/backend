ALTER TABLE vistas
    ADD COLUMN IF NOT EXISTS visible_menu BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO vistas (codigo, nombre, ruta, grupo, orden, activo, audience, visible_menu)
VALUES ('VISTA_GESTION_CREDENCIALES', 'Gestión de credenciales',
        '/seguridad/credenciales', 'SEGURIDAD', 99, TRUE, 'STAFF', FALSE)
ON CONFLICT (codigo) DO UPDATE
SET nombre = EXCLUDED.nombre,
    activo = TRUE,
    audience = 'STAFF',
    visible_menu = FALSE;

INSERT INTO rol_vista_permisos
    (rol_id, vista_id, leer, escribir, modificar, eliminar, data_scope)
SELECT role.id, view.id, TRUE, FALSE, TRUE, FALSE, 'COMPANY'
FROM roles role
JOIN vistas view ON view.codigo = 'VISTA_GESTION_CREDENCIALES'
WHERE role.purpose IN ('PLATFORM_ADMIN', 'COMPANY_ADMIN')
ON CONFLICT (rol_id, vista_id) DO UPDATE
SET leer = TRUE,
    modificar = TRUE,
    data_scope = 'COMPANY';

UPDATE roles
SET permission_version = permission_version + 1
WHERE purpose IN ('PLATFORM_ADMIN', 'COMPANY_ADMIN');

COMMENT ON COLUMN vistas.visible_menu IS
    'FALSE para capacidades autorizables que no representan una ruta navegable del sidebar.';
