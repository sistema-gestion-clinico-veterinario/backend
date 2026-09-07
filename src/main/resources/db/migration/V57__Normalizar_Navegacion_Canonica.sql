-- Normaliza la navegación global después de retirar el orden y la
-- presentación visual por rol. Esta migración no modifica permisos.

UPDATE ventanas
SET orden = CASE codigo
        WHEN 'ADMINISTRACION' THEN 2
        WHEN 'PERSONAL' THEN 3
        WHEN 'CLINICA' THEN 4
        WHEN 'PORTAL_APODERADO' THEN 5
        WHEN 'FACTURACION' THEN 6
        ELSE orden
    END,
    presentacion_default = CASE
        WHEN codigo = 'PORTAL_APODERADO' THEN 'FLAT'
        ELSE 'GROUPED'
    END
WHERE codigo IN (
    'ADMINISTRACION',
    'PERSONAL',
    'CLINICA',
    'PORTAL_APODERADO',
    'FACTURACION'
);

-- Las vistas sin módulo usan su orden como posición principal del Sidebar.
-- Dashboard queda antes de los módulos y Mi Perfil se mantiene al final.
UPDATE vistas
SET orden = CASE codigo
        WHEN 'VISTA_DASHBOARD' THEN 1
        WHEN 'VISTA_EMPLEADO_DASHBOARD' THEN 1
        WHEN 'VISTA_PROFILE' THEN 98
        ELSE orden
    END
WHERE codigo IN (
    'VISTA_DASHBOARD',
    'VISTA_EMPLEADO_DASHBOARD',
    'VISTA_PROFILE'
);

-- Orden estable del portal cuando su presentación global es plana.
UPDATE vistas
SET orden = CASE codigo
        WHEN 'VISTA_APODERADO_DASHBOARD' THEN 1
        WHEN 'VISTA_MIS_MASCOTAS' THEN 2
        WHEN 'VISTA_MIS_CITAS' THEN 3
        WHEN 'VISTA_MI_HISTORIAL' THEN 4
        WHEN 'VISTA_MIS_RECETAS' THEN 5
        WHEN 'VISTA_MIS_PAGOS' THEN 6
        ELSE orden
    END
WHERE codigo IN (
    'VISTA_APODERADO_DASHBOARD',
    'VISTA_MIS_MASCOTAS',
    'VISTA_MIS_CITAS',
    'VISTA_MI_HISTORIAL',
    'VISTA_MIS_RECETAS',
    'VISTA_MIS_PAGOS'
);
