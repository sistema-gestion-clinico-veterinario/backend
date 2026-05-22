-- ============================================================
-- reset-menu.sql
-- Limpia las tablas de menú/permisos para empezar desde cero.
-- Las vistas serán resembradas automáticamente por DataInitializer.
-- Las ventanas se crearán manualmente desde /admin/ventanas.
--
-- ⚠️  EJECUTAR UNA SOLA VEZ antes de reiniciar el backend.
-- ⚠️  NO borra usuarios, mascotas, citas ni datos clínicos.
-- ============================================================

-- 1. Permisos personalizados por usuario (referencian ventanas)
TRUNCATE TABLE usuario_por_rol_permisos RESTART IDENTITY CASCADE;

-- 2. Permisos de rol sobre ventanas
TRUNCATE TABLE rol_ventana_permisos RESTART IDENTITY CASCADE;

-- 3. Vistas (rutas del frontend, referencian ventanas)
TRUNCATE TABLE vistas RESTART IDENTITY CASCADE;

-- 4. Ventanas (menú jerárquico)
TRUNCATE TABLE ventanas RESTART IDENTITY CASCADE;

-- ============================================================
-- VERIFICACIÓN — deben retornar 0 filas cada una
-- ============================================================
SELECT 'ventanas'                AS tabla, COUNT(*) FROM ventanas
UNION ALL
SELECT 'vistas'                  AS tabla, COUNT(*) FROM vistas
UNION ALL
SELECT 'rol_ventana_permisos'    AS tabla, COUNT(*) FROM rol_ventana_permisos
UNION ALL
SELECT 'usuario_por_rol_permisos'AS tabla, COUNT(*) FROM usuario_por_rol_permisos;
