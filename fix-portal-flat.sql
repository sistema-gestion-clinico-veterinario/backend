-- ============================================================
-- fix-portal-flat.sql
-- Aplana el menú del portal del apoderado:
--   · PORTAL pasa a activo=false (ya no actúa como agrupador)
--   · Sus hijos pasan a ser ítems raíz (parent_id = NULL)
--   · Se actualizan órdenes para que aparezcan en secuencia
-- Ejecutar UNA SOLA VEZ sobre la BD existente.
-- ============================================================

-- 1. Desactivar el agrupador PORTAL (ya no aparece en menú)
UPDATE ventanas SET activo = false WHERE codigo = 'PORTAL';

-- 2. Aplanar los ítems del portal: quitar parent_id y asignar orden
UPDATE ventanas SET parent_id = NULL, orden = 1, nombre = 'Mi Portal'
  WHERE codigo = 'APODERADO_DASHBOARD';

UPDATE ventanas SET parent_id = NULL, orden = 2
  WHERE codigo = 'MIS_MASCOTAS';

UPDATE ventanas SET parent_id = NULL, orden = 3
  WHERE codigo = 'MIS_CITAS';

UPDATE ventanas SET parent_id = NULL, orden = 4
  WHERE codigo = 'MIS_PAGOS';

UPDATE ventanas SET parent_id = NULL, orden = 5
  WHERE codigo = 'MIS_HISTORIAL';

-- ============================================================
-- VERIFICACIÓN: debería retornar 5 filas con parent_id = NULL
-- ============================================================
SELECT codigo, nombre, ruta, parent_id, orden, activo
FROM ventanas
WHERE codigo IN ('APODERADO_DASHBOARD','MIS_MASCOTAS','MIS_CITAS','MIS_PAGOS','MIS_HISTORIAL')
ORDER BY orden;
