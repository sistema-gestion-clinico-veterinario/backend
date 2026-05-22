-- Asignar todas las vistas activas al rol SUPER_ADMIN con todos los permisos
-- Primero, obtener el ID del rol SUPER_ADMIN
SET @roleId = (SELECT id FROM roles WHERE name = 'ROLE_SUPER_ADMIN' LIMIT 1);

-- Si el rol existe, asignar todas las vistas activas
IF @roleId IS NOT NULL THEN
  -- Limpiar asignaciones previas
  DELETE FROM rol_vista_permisos WHERE rol_id = @roleId;

  -- Insertar todas las vistas activas con todos los permisos
  INSERT INTO rol_vista_permisos (rol_id, vista_id, leer, escribir, modificar, eliminar, created_at, updated_at)
  SELECT
    @roleId,
    v.id,
    1,
    1,
    1,
    1,
    NOW(),
    NOW()
  FROM vistas v
  WHERE v.activo = 1;

  SELECT CONCAT('✅ ', ROW_COUNT(), ' vistas asignadas al rol SUPER_ADMIN') AS resultado;
ELSE
  SELECT '❌ No se encontró el rol ROLE_SUPER_ADMIN' AS resultado;
END IF;

-- Verificación: mostrar vistas asignadas al SUPER_ADMIN
SELECT
  r.name as rol,
  v.codigo,
  v.nombre,
  v.grupo,
  rvp.leer,
  rvp.escribir,
  rvp.modificar,
  rvp.eliminar
FROM rol_vista_permisos rvp
JOIN roles r ON rvp.rol_id = r.id
JOIN vistas v ON rvp.vista_id = v.id
WHERE r.name = 'ROLE_SUPER_ADMIN'
ORDER BY v.grupo, v.nombre;
