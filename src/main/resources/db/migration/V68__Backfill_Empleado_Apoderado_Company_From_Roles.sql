-- V64 hizo el backfill inicial de empleado.company_id/apoderado.company_id
-- copiando usuario.company_id, pero ese campo solo tiene valor cuando el
-- usuario tenia EXACTAMENTE una empresa activa al momento de correr V64
-- (usuario.company_id es null para cualquiera con mas de una relacion
-- activa, ej. empleado en A y cliente en B). Esas filas quedaron con
-- company_id NULL y siguen bloqueando funciones que ya dependen del
-- company_id propio de Empleado/Apoderado (ver EmpleadoServiceImpl,
-- horarios de trabajo).
--
-- Backfill de segunda pasada: para cada fila sin company_id, se infiere la
-- empresa desde usuario_por_rol (fuente de verdad de autorizacion), tomando
-- el rol STAFF/CLIENT segun corresponda. Solo se completa cuando hay una
-- unica empresa candidata (sin ambiguedad); si el usuario tiene asignaciones
-- en mas de una empresa para ese scope, la fila se deja para revision manual.

UPDATE empleado e
SET company_id = candidatos.company_id
FROM (
    SELECT upr.usuario_id, MIN(upr.company_id) AS company_id
    FROM usuario_por_rol upr
    JOIN roles r ON r.id = upr.rol_id
    WHERE r.scope = 'STAFF' AND upr.company_id IS NOT NULL
    GROUP BY upr.usuario_id
    HAVING COUNT(DISTINCT upr.company_id) = 1
) candidatos
WHERE e.company_id IS NULL
  AND e.user_id = candidatos.usuario_id;

UPDATE apoderado a
SET company_id = candidatos.company_id
FROM (
    SELECT upr.usuario_id, MIN(upr.company_id) AS company_id
    FROM usuario_por_rol upr
    JOIN roles r ON r.id = upr.rol_id
    WHERE r.scope = 'CLIENT' AND upr.company_id IS NOT NULL
    GROUP BY upr.usuario_id
    HAVING COUNT(DISTINCT upr.company_id) = 1
) candidatos
WHERE a.company_id IS NULL
  AND a.user_id = candidatos.usuario_id;
