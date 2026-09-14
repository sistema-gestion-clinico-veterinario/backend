-- Fase 2 del plan de separacion identidad/empresa: backfill de datos.
-- Solo mueve/copia datos, no toca constraints ni indices (eso va en V65).
-- Ver C:\Users\Usuario\.claude\plans\hidden-purring-abelson.md para el plan completo.
--
-- Nota de calidad de dato aceptada: no existe una senal historica mejor que
-- created_at para aproximar fecha_ingreso, y usuario no tiene created_at
-- propio, por lo que el fallback para usuario_membresia es CURRENT_DATE.

UPDATE empleado e
SET company_id = u.company_id,
    fecha_ingreso = COALESCE(e.created_at::date, CURRENT_DATE)
FROM usuario u
WHERE e.user_id = u.id
  AND u.company_id IS NOT NULL;

UPDATE apoderado a
SET company_id = u.company_id,
    fecha_ingreso = COALESCE(a.created_at::date, CURRENT_DATE),
    estado = true
FROM usuario u
WHERE a.user_id = u.id
  AND u.company_id IS NOT NULL;

-- Cuentas admin puras (sin Empleado ni Apoderado) con company asignada.
INSERT INTO usuario_membresia (usuario_id, company_id, fecha_ingreso, estado)
SELECT u.id, u.company_id, CURRENT_DATE, true
FROM usuario u
WHERE u.company_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM empleado e WHERE e.user_id = u.id)
  AND NOT EXISTS (SELECT 1 FROM apoderado a WHERE a.user_id = u.id);

-- Hoy no es ambiguo (un usuario = una empresa), asi que la copia es directa.
-- La distincion rol global (company NULL) vs. rol de empresa se resuelve
-- aparte, en la Fase 2.5, antes de fijar la unicidad definitiva.
UPDATE usuario_por_rol upr
SET company_id = u.company_id
FROM usuario u
WHERE upr.usuario_id = u.id
  AND u.company_id IS NOT NULL;
