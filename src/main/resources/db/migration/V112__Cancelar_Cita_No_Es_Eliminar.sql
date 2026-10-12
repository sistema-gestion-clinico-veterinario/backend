-- Cancelar una cita es un cambio de estado (modificar), no una eliminación. Los roles de cliente que podían cancelar
-- porque tenían "eliminar" en Mis citas conservan esa capacidad como "modificar", y pierden el permiso de eliminar.
UPDATE rol_vista_permisos permiso
SET modificar = TRUE
FROM roles rol, vistas vista
WHERE rol.id = permiso.rol_id
  AND vista.id = permiso.vista_id
  AND vista.codigo = 'VISTA_MIS_CITAS'
  AND rol.scope = 'CLIENT'
  AND permiso.eliminar = TRUE;

UPDATE roles
SET permission_version = permission_version + 1
WHERE id IN (
    SELECT permiso.rol_id
    FROM rol_vista_permisos permiso
    JOIN vistas vista ON vista.id = permiso.vista_id
    JOIN roles rol ON rol.id = permiso.rol_id
    WHERE vista.codigo = 'VISTA_MIS_CITAS'
      AND rol.scope = 'CLIENT'
      AND permiso.eliminar = TRUE
);

UPDATE rol_vista_permisos permiso
SET eliminar = FALSE
FROM roles rol, vistas vista
WHERE rol.id = permiso.rol_id
  AND vista.id = permiso.vista_id
  AND vista.codigo = 'VISTA_MIS_CITAS'
  AND rol.scope = 'CLIENT'
  AND permiso.eliminar = TRUE;
