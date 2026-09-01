-- El administrador empresarial es una plantilla global. El aislamiento de datos
-- se mantiene mediante company_id del usuario autenticado, no duplicando el rol.
UPDATE roles
SET name = 'ROLE_ADMIN',
    descripcion = 'Administrador de empresa',
    scope = 'STAFF',
    purpose = 'COMPANY_ADMIN',
    system_managed = TRUE,
    protected = TRUE,
    activo = TRUE
WHERE company_id IS NULL
  AND purpose = 'COMPANY_ADMIN';

INSERT INTO roles (name, descripcion, activo, company_id, scope, purpose,
                   system_managed, protected, permission_version)
SELECT 'ROLE_ADMIN', 'Administrador de empresa', TRUE, NULL, 'STAFF',
       'COMPANY_ADMIN', TRUE, TRUE, 0
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE company_id IS NULL AND purpose = 'COMPANY_ADMIN'
);

INSERT INTO usuario_por_rol (usuario_id, rol_id)
SELECT DISTINCT assignment.usuario_id, global_admin.id
FROM usuario_por_rol assignment
JOIN roles company_admin ON company_admin.id = assignment.rol_id
CROSS JOIN LATERAL (
    SELECT id
    FROM roles
    WHERE company_id IS NULL AND purpose = 'COMPANY_ADMIN'
    ORDER BY id
    LIMIT 1
) global_admin
WHERE company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN'
ON CONFLICT (usuario_id, rol_id) DO NOTHING;

DELETE FROM usuario_por_rol_permisos override_permission
USING usuario_por_rol assignment, roles company_admin
WHERE override_permission.usuario_por_rol_id = assignment.id
  AND assignment.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM usuario_por_rol assignment
USING roles company_admin
WHERE assignment.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM rol_vista_configuracion config
USING roles company_admin
WHERE config.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM rol_ventana_configuracion config
USING roles company_admin
WHERE config.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM rol_ventana_permisos permission
USING roles company_admin
WHERE permission.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM rol_vista_permisos permission
USING roles company_admin
WHERE permission.rol_id = company_admin.id
  AND company_admin.company_id IS NOT NULL
  AND company_admin.purpose = 'COMPANY_ADMIN';

DELETE FROM roles
WHERE company_id IS NOT NULL
  AND purpose = 'COMPANY_ADMIN';
