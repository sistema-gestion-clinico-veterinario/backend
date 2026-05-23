-- ============================================================
-- SCRIPT SEGURO: Asigna vistas nuevas al SUPER_ADMIN
-- SOLO agrega las vistas que NO tienen permiso aún.
-- NO sobreescribe permisos existentes (configurados manualmente).
-- ============================================================

DO $$
DECLARE
    v_role_id INT;
    v_count   INT;
BEGIN
    SELECT id INTO v_role_id FROM roles WHERE name = 'ROLE_SUPER_ADMIN' LIMIT 1;

    IF v_role_id IS NULL THEN
        RAISE NOTICE 'No se encontró el rol ROLE_SUPER_ADMIN';
        RETURN;
    END IF;

    INSERT INTO rol_vista_permisos (rol_id, vista_id, leer, escribir, modificar, eliminar, created_at, updated_at)
    SELECT v_role_id, v.id, true, true, true, true, NOW(), NOW()
    FROM vistas v
    WHERE v.activo = true
      AND NOT EXISTS (
          SELECT 1 FROM rol_vista_permisos rvp
          WHERE rvp.rol_id = v_role_id AND rvp.vista_id = v.id
      );

    GET DIAGNOSTICS v_count = ROW_COUNT;

    IF v_count > 0 THEN
        RAISE NOTICE '% nuevas vistas asignadas al SUPER_ADMIN', v_count;
    ELSE
        RAISE NOTICE 'SUPER_ADMIN ya tiene todos los permisos al día, no se modificó nada';
    END IF;
END;
$$;

-- Verificación: estado actual del SUPER_ADMIN
SELECT
    r.name          AS rol,
    v.codigo,
    v.nombre,
    v.grupo,
    rvp.leer,
    rvp.escribir,
    rvp.modificar,
    rvp.eliminar
FROM rol_vista_permisos rvp
JOIN roles r  ON rvp.rol_id  = r.id
JOIN vistas v ON rvp.vista_id = v.id
WHERE r.name = 'ROLE_SUPER_ADMIN'
ORDER BY v.grupo, v.nombre;
