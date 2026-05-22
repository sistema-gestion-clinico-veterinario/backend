BEGIN;

WITH ventanas_seed(codigo, nombre, grupo, orden, activo) AS (
  VALUES
    ('ADMINISTRACION', 'Administración', 'administracion', 2, true),
    ('PERSONAL', 'Personal', 'rrhh', 3, true),
    ('CLINICA', 'Clínica', 'clinica', 4, true),
    ('PORTAL_APODERADO', 'Portal Apoderado', 'apoderado', 5, true)
)
INSERT INTO ventanas (codigo, nombre, grupo, orden, activo)
SELECT codigo, nombre, grupo, orden, activo
FROM ventanas_seed
ON CONFLICT (codigo) DO UPDATE SET
  nombre = EXCLUDED.nombre,
  grupo = EXCLUDED.grupo,
  orden = EXCLUDED.orden,
  activo = EXCLUDED.activo;

WITH vistas_seed(codigo, nombre, ruta, grupo, orden, ventana_codigo, activo) AS (
  VALUES
    ('VISTA_DASHBOARD', 'Dashboard', '/dashboard', 'GENERAL', 1, NULL, true),
    ('VISTA_PROFILE', 'Mi Perfil', '/profile', 'GENERAL', 98, NULL, true),
    ('VISTA_COMPANY', 'Mi Empresa', '/admin/company', 'ADMIN', 1, 'ADMINISTRACION', true),
    ('VISTA_AUDITORIA_ADMIN', 'Auditoría', '/admin/auditoria', 'ADMIN', 2, 'ADMINISTRACION', true),
    ('VISTA_ROLES', 'Roles', '/admin/roles', 'ADMIN', 3, 'ADMINISTRACION', true),
    ('VISTA_VENTANAS', 'Gestión de Vistas', '/admin/ventanas', 'ADMIN', 4, 'ADMINISTRACION', true),
    ('VISTA_COMPLEMENTARIO', 'Complementario', '/admin/complementario', 'ADMIN', 5, 'ADMINISTRACION', true),
    ('VISTA_PAGOS', 'Pagos', '/admin/pagos', 'ADMIN', 6, 'ADMINISTRACION', true),
    ('VISTA_EMPLEADOS', 'Empleados', '/admin/empleados', 'RRHH', 1, 'PERSONAL', true),
    ('VISTA_HORARIOS', 'Horarios', '/admin/empleados/horarios', 'RRHH', 2, 'PERSONAL', true),
    ('VISTA_MI_HORARIO', 'Mi Horario', '/mi-horario', 'RRHH', 3, 'PERSONAL', true),
    ('VISTA_CLIENTES', 'Clientes', '/clientes', 'CLINICA', 1, 'CLINICA', true),
    ('VISTA_MASCOTAS', 'Mascotas', '/mascotas', 'CLINICA', 2, 'CLINICA', true),
    ('VISTA_RECETAS', 'Recetas', '/recetas', 'CLINICA', 3, 'CLINICA', true),
    ('VISTA_HISTORIAS', 'Historias Clínicas', '/historias-clinicas', 'CLINICA', 4, 'CLINICA', true),
    ('VISTA_CITAS_AGENDA', 'Agenda de Citas', '/citas/agenda', 'CLINICA', 5, 'CLINICA', true),
    ('VISTA_APODERADO_DASHBOARD', 'Mi Portal', '/apoderado/dashboard', 'APODERADO', 1, 'PORTAL_APODERADO', true),
    ('VISTA_MIS_MASCOTAS', 'Mis Mascotas', '/apoderado/mis-mascotas', 'APODERADO', 2, 'PORTAL_APODERADO', true),
    ('VISTA_MIS_CITAS', 'Mis Citas', '/apoderado/mis-citas', 'APODERADO', 3, 'PORTAL_APODERADO', true),
    ('VISTA_MI_HISTORIAL', 'Mi Historial', '/apoderado/mi-historial', 'APODERADO', 4, 'PORTAL_APODERADO', true),
    ('VISTA_MIS_PAGOS', 'Mis Pagos', '/apoderado/mis-pagos', 'APODERADO', 5, 'PORTAL_APODERADO', true)
)
INSERT INTO vistas (codigo, nombre, ruta, grupo, orden, ventana_id, activo)
SELECT
  vs.codigo,
  vs.nombre,
  vs.ruta,
  vs.grupo,
  vs.orden,
  v.id,
  vs.activo
FROM vistas_seed vs
LEFT JOIN ventanas v ON v.codigo = vs.ventana_codigo
ON CONFLICT (codigo) DO UPDATE SET
  nombre = EXCLUDED.nombre,
  ruta = EXCLUDED.ruta,
  grupo = EXCLUDED.grupo,
  orden = EXCLUDED.orden,
  ventana_id = EXCLUDED.ventana_id,
  activo = EXCLUDED.activo;

UPDATE vistas
SET activo = false
WHERE codigo IN (
  'VISTA_EMPRESA',
  'VISTA_AUDITORIA',
  'VISTA_PERFIL',
  'VISTA_LOGOUT',
  'VISTA_ROLES_LISTAR',
  'VISTA_ROLES_CREAR',
  'VISTA_ROLES_PERMISOS',
  'VISTA_ROLES_HISTORIAL',
  'VISTA_VISTAS_LISTAR',
  'VISTA_VISTAS_CREAR',
  'VISTA_VISTAS_CONFIGURAR',
  'VISTA_EMPLEADOS_LISTAR',
  'VISTA_EMPLEADOS_CREAR',
  'VISTA_EMPLEADOS_ROLES',
  'VISTA_EMPLEADOS_HORARIOS',
  'VISTA_MASCOTAS_LISTAR',
  'VISTA_MASCOTAS_CREAR',
  'VISTA_HISTORIAS_CLINICAS',
  'VISTA_CITAS'
);

UPDATE ventanas
SET activo = false
WHERE codigo IN (
  'ROLES_PERMISOS',
  'GESTION_VISTAS',
  'EMPLEADOS',
  'MASCOTAS'
);

DELETE FROM usuario_por_rol_permisos
WHERE vista_id IN (SELECT id FROM vistas WHERE activo = false);

DELETE FROM rol_vista_permisos
WHERE vista_id IN (SELECT id FROM vistas WHERE activo = false);

INSERT INTO rol_vista_permisos (rol_id, vista_id, leer, escribir, modificar, eliminar)
SELECT r.id, v.id, true, true, true, true
FROM roles r
CROSS JOIN vistas v
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND v.activo = true
ON CONFLICT (rol_id, vista_id) DO UPDATE SET
  leer = true,
  escribir = true,
  modificar = true,
  eliminar = true;

COMMIT;

SELECT id, codigo, nombre, ruta, grupo, orden, ventana_id, activo
FROM vistas
WHERE activo = true
ORDER BY COALESCE(ventana_id, 0), orden, nombre;
