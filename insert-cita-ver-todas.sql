-- ============================================================
-- Agrega la vista CITA_VER_TODAS y la asigna al SUPER_ADMIN
-- ============================================================
BEGIN;

INSERT INTO vistas (codigo, nombre, ruta, grupo, orden, activo)
VALUES ('CITA_VER_TODAS', 'Ver Todas las Citas', '#', 'CLINICA', 6, true)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_vista_permisos (rol_id, vista_id, leer, escribir, modificar, eliminar)
SELECT r.id, v.id, true, true, true, true
FROM roles r
CROSS JOIN vistas v
WHERE r.name = 'ROLE_SUPER_ADMIN'
  AND v.codigo = 'CITA_VER_TODAS'
ON CONFLICT (rol_id, vista_id) DO NOTHING;

COMMIT;
