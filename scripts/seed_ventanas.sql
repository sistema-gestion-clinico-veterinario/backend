-- Script para inicializar datos en la tabla ventanas
-- Ejecutar este script después de que la aplicación haya creado las tablas

-- Insertar ventanas base sin duplicados
INSERT INTO ventanas (codigo, nombre, grupo, orden, activo)
VALUES
    ('MIS_MASCOTAS', 'Mis Mascotas', 'usuarios', 1, true),
    ('MIS_CITAS', 'Mis Citas', 'usuarios', 2, true),
    ('MI_PERFIL', 'Mi Perfil', 'usuarios', 3, true),
    ('CLIENTES', 'Clientes', 'administracion', 10, true),
    ('EMPLEADOS', 'Empleados', 'administracion', 11, true),
    ('SERVICIOS', 'Servicios', 'administracion', 12, true),
    ('EMPRESA', 'Configuración Empresa', 'configuracion', 20, true),
    ('ROLES', 'Gestión de Roles', 'configuracion', 21, true),
    ('DASHBOARD', 'Dashboard', 'reportes', 30, true),
    ('REPORTES', 'Reportes', 'reportes', 31, true)
ON CONFLICT (codigo) DO NOTHING;

-- Ejemplo: Asociar vistas existentes a ventanas
-- Descomenta y ajusta según tus vistas reales
-- UPDATE vistas SET ventana_id = (SELECT id FROM ventanas WHERE codigo = 'MIS_MASCOTAS')
--   WHERE codigo IN ('VISTA_MASCOTA_LISTAR', 'VISTA_MASCOTA_CREAR', 'VISTA_MASCOTA_EDITAR');
