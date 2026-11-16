\set ON_ERROR_STOP on

\echo '--- columnas de las tablas de configuración (para diagnóstico)'
SELECT table_name, string_agg(column_name || ':' || data_type || CASE WHEN is_nullable = 'NO' AND column_default IS NULL THEN ':OBLIGATORIA' ELSE '' END, ', ' ORDER BY ordinal_position) AS columnas
FROM information_schema.columns
WHERE table_name IN ('company_operating_hours', 'horario_empleado', 'raza')
GROUP BY table_name;

INSERT INTO company_operating_hours (company_id, dia_semana, opening_time, closing_time, is_open)
SELECT c.id, d.dia, TIME '07:00', TIME '21:00', true
FROM company c
CROSS JOIN (VALUES ('LUNES'), ('MARTES'), ('MIERCOLES'), ('JUEVES'), ('VIERNES'), ('SABADO'), ('DOMINGO')) AS d(dia)
WHERE c.slug = :'slug'
  AND NOT EXISTS (SELECT 1 FROM company_operating_hours h WHERE h.company_id = c.id AND h.dia_semana = d.dia);

INSERT INTO raza (nombre, especie, activo, created_at)
SELECT 'Mestizo', e.especie, true, now()
FROM (VALUES ('PERRO'), ('GATO')) AS e(especie)
WHERE NOT EXISTS (SELECT 1 FROM raza r WHERE r.especie = e.especie AND r.nombre = 'Mestizo');

INSERT INTO horario_empleado (empleado_id, fecha, dia_semana, hora_inicio, hora_fin, activo, created_at)
SELECT e.id, f.dia, (ARRAY['LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO'])[extract(isodow FROM f.dia)::int],
       TIME '08:00', TIME '20:00', true, now()
FROM empleado e
JOIN company c ON c.id = e.company_id AND c.slug = :'slug'
CROSS JOIN LATERAL generate_series(((now() AT TIME ZONE 'America/Lima')::date), ((now() AT TIME ZONE 'America/Lima')::date + 125), interval '1 day') AS f(dia)
WHERE e.estado = true AND e.user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM horario_empleado h WHERE h.empleado_id = e.id AND h.fecha = f.dia::date);

SELECT 'horarios de la clínica: ' || count(*) FROM company_operating_hours h JOIN company c ON c.id = h.company_id AND c.slug = :'slug';
SELECT 'turnos de empleados: ' || count(*) FROM horario_empleado h JOIN empleado e ON e.id = h.empleado_id JOIN company c ON c.id = e.company_id AND c.slug = :'slug';
SELECT 'razas: ' || count(*) FROM raza;
