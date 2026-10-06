-- Insert initial ventanas (modules/windows) with categorized groups
INSERT INTO ventanas (codigo, nombre, grupo, orden, activo) VALUES
-- User/Client section
('MIS_MASCOTAS', 'Mis Mascotas', 'usuarios', 1, true),
('MIS_CITAS', 'Mis Citas', 'usuarios', 2, true),
('MI_PERFIL', 'Mi Perfil', 'usuarios', 3, true),

-- Administration section
('ADMINISTRACION_CLIENTES', 'Clientes', 'administracion', 10, true),
('ADMINISTRACION_EMPLEADOS', 'Empleados', 'administracion', 11, true),
('ADMINISTRACION_SERVICIOS', 'Servicios', 'administracion', 12, true),

-- Settings section
('CONFIGURACION_EMPRESA', 'Configuración Empresa', 'configuracion', 20, true),
('CONFIGURACION_ROLES', 'Gestión de Roles', 'configuracion', 21, true),

-- Dashboard/Reports
('DASHBOARD', 'Dashboard', 'reportes', 30, true),
('REPORTES', 'Reportes', 'reportes', 31, true)
ON CONFLICT (codigo) DO NOTHING;
