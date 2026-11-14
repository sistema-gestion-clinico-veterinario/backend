\set ON_ERROR_STOP on
SET synchronous_commit = off;
SELECT setseed(0.42);

CREATE TEMP TABLE tamano AS
SELECT g AS n,
       CASE WHEN g = 1 THEN 20000 ELSE 800 + (g * 7919) % 1200 END AS n_citas,
       CASE WHEN g = 1 THEN 1200 ELSE 100 + (g * 37) % 100 END AS n_propietarios,
       CASE WHEN g = 1 THEN 10 ELSE 4 + (g % 3) END AS n_empleados
FROM generate_series(1, 6) g;

INSERT INTO company (name, slug, ruc, activo, modo_acceso_propietario, created_at)
SELECT CASE WHEN n = 1 THEN 'Clinica de medicion' ELSE 'Clinica auxiliar ' || n END,
       CASE WHEN n = 1 THEN 'clinica-medicion' ELSE 'auxiliar-' || n END,
       (20000000000 + n)::text, true, 'PORTAL_OPCIONAL', now()
FROM tamano ORDER BY n;

CREATE TEMP TABLE empresa AS
SELECT t.n, c.id AS company_id, t.n_citas, t.n_propietarios, t.n_empleados
FROM tamano t
JOIN company c ON c.slug = CASE WHEN t.n = 1 THEN 'clinica-medicion' ELSE 'auxiliar-' || t.n END;

INSERT INTO usuario (email, username, nombre, apellido, dni, activo, email_verified, company_id)
SELECT 'e' || e.n || '_' || i || '@medicion.test', 'e' || e.n || '_' || i, 'Empleado' || i, 'Medicion',
       lpad(((e.n * 1000 + i) % 100000000)::text, 8, '0'), true, true, e.company_id
FROM empresa e, LATERAL generate_series(1, e.n_empleados) i
ORDER BY e.n, i;

INSERT INTO usuario (email, username, nombre, apellido, dni, activo, email_verified, company_id)
SELECT 'p' || e.n || '_' || i || '@medicion.test', 'p' || e.n || '_' || i, 'Propietario' || i, 'Medicion',
       lpad(((e.n * 100000 + i) % 100000000)::text, 8, '0'), true, true, e.company_id
FROM empresa e, LATERAL generate_series(1, e.n_propietarios) i
ORDER BY e.n, i;

INSERT INTO empleado (user_id, company_id, estado, genero, numero_documento_identidad, tipo_documento_identidad, created_at)
SELECT u.id, u.company_id, true, 'MASCULINO', u.dni, 'DNI', now()
FROM usuario u WHERE u.username LIKE 'e%\_%' ORDER BY u.id;

INSERT INTO apoderado (user_id, company_id, estado, estado_acceso_portal, genero, numero_documento, tipo_documento_identidad, created_at, updated_at)
SELECT u.id, u.company_id, true, 'SIN_ACCESO', 'FEMENINO', u.dni, 'DNI', now(), now()
FROM usuario u WHERE u.username LIKE 'p%\_%' ORDER BY u.id;

INSERT INTO servicios (company_id, activo, disponible, precio, nombre, descripcion, tipo_control_preventivo, created_at)
SELECT e.company_id, true, true, 40 + 10 * s, 'Servicio ' || s, 'Servicio de prueba ' || s, 'NO_APLICA', now()
FROM empresa e, LATERAL generate_series(1, 6) s ORDER BY e.n, s;

INSERT INTO mascota (apoderado_id, uuid, especie, nombre_completo, activo, esterilizado, created_at)
SELECT a.id, gen_random_uuid()::text, (ARRAY['PERRO','GATO','PERRO','AVE'])[1 + (a.id % 4)],
       'Mascota ' || a.id || '-' || m, true, false, now()
FROM apoderado a, LATERAL generate_series(1, 1 + (a.id % 3)) m ORDER BY a.id, m;

CREATE TEMP TABLE por_empresa AS
SELECT e.n, e.company_id, e.n_citas,
       (SELECT min(m.id) FROM mascota m JOIN apoderado a ON a.id = m.apoderado_id WHERE a.company_id = e.company_id) AS primera_mascota,
       (SELECT count(*) FROM mascota m JOIN apoderado a ON a.id = m.apoderado_id WHERE a.company_id = e.company_id) AS n_mascotas,
       (SELECT min(id) FROM empleado WHERE company_id = e.company_id) AS primer_empleado,
       (SELECT count(*) FROM empleado WHERE company_id = e.company_id) AS n_empleados,
       (SELECT min(id) FROM servicios WHERE company_id = e.company_id) AS primer_servicio
FROM empresa e;

INSERT INTO citas (mascota_id, empleado_id, servicio_id, fecha_hora_inicio, fecha_hora_fin, duracion_minutos, estado,
                   motivo_cita, total_servicio, monto_pagado, eliminada, business_trace_id, numero_cita, created_at, updated_at)
SELECT p.primera_mascota + floor(random() * p.n_mascotas)::bigint,
       p.primer_empleado + floor(random() * p.n_empleados)::bigint,
       p.primer_servicio + floor(random() * 6)::bigint,
       f.inicio, f.inicio + interval '30 minutes', 30,
       CASE WHEN f.inicio > now() THEN (ARRAY['PROGRAMADA','CONFIRMADA','PENDIENTE'])[1 + floor(random() * 3)::int]
            WHEN f.r < 0.78 THEN 'COMPLETADA' WHEN f.r < 0.90 THEN 'CANCELADA' ELSE 'NO_ASISTIO' END,
       'Consulta de control', 60, CASE WHEN f.r < 0.78 THEN 60 ELSE 0 END, false,
       gen_random_uuid()::text, 'C-' || p.n || '-' || g, f.inicio, f.inicio
FROM por_empresa p, LATERAL generate_series(1, p.n_citas) g,
     LATERAL (SELECT date_trunc('minute', now() - interval '2 years' + (random() * (interval '2 years' + interval '30 days')) + g * interval '0 second') AS inicio,
                     random() + g * 0 AS r) f;

CREATE TEMP TABLE usuario_empleado AS
SELECT e.id AS empleado_id, e.user_id, e.company_id FROM empleado e;

INSERT INTO purchases (company_id, cita_id, user_id, cliente_id, tipo_purchase, total, monto_recibido, payment_status,
                       metodo_pago, tipo_documento, receipt_status, receipt_attempts, numero_venta, created_at, business_trace_id)
SELECT ue.company_id, c.id, ue.user_id, a.user_id, 'SERVICIO_CITA', c.total_servicio, c.total_servicio, 'PAID',
       (ARRAY['EFECTIVO','YAPE','TARJETA','PLIN'])[1 + (c.id % 4)], 'BOLETA', 'NOT_TRACKED', 0,
       'VC-' || c.id, c.fecha_hora_inicio, gen_random_uuid()::text
FROM citas c
JOIN usuario_empleado ue ON ue.empleado_id = c.empleado_id
JOIN mascota m ON m.id = c.mascota_id
JOIN apoderado a ON a.id = m.apoderado_id
WHERE c.estado = 'COMPLETADA';

INSERT INTO purchases (company_id, cita_id, user_id, cliente_id, tipo_purchase, total, monto_recibido, payment_status,
                       metodo_pago, tipo_documento, receipt_status, receipt_attempts, numero_venta, created_at, business_trace_id)
SELECT p.company_id, NULL, p.primer_user, NULL, 'TIENDA', 25 + (g % 200), 25 + (g % 200), 'PAID',
       (ARRAY['EFECTIVO','YAPE','TARJETA','PLIN'])[1 + (g % 4)], 'BOLETA', 'NOT_TRACKED', 0,
       'VT-' || p.n || '-' || g, now() - interval '2 years' + (random() * interval '2 years'), gen_random_uuid()::text
FROM (SELECT pe.n, pe.company_id, pe.n_citas, (SELECT user_id FROM empleado WHERE id = pe.primer_empleado) AS primer_user
      FROM por_empresa pe) p,
     LATERAL generate_series(1, (p.n_citas * 0.4)::int) g;

INSERT INTO historia_clinica (numero_hc, mascota_id, activa, created_at, updated_at)
SELECT 'HC-' || lpad(m.id::text, 8, '0'), m.id, true, now(), now()
FROM mascota m ORDER BY m.id;

INSERT INTO consulta (version, estado, historia_clinica_id, cita_id, veterinario_id, fecha_consulta, motivo_consulta,
                      tipo_consulta, fecha_cierre, cerrado_por, created_at, updated_at)
SELECT 0, 'CERRADA', h.id, c.id, c.empleado_id, c.fecha_hora_inicio, 'Consulta de control',
       (ARRAY['PRIMERA_VEZ','SEGUIMIENTO','CONTROL_RUTINA','VACUNACION','OTRO'])[1 + (c.id % 5)],
       c.fecha_hora_inicio + interval '30 minutes', 'medicion', c.fecha_hora_inicio, c.fecha_hora_inicio
FROM citas c
JOIN historia_clinica h ON h.mascota_id = c.mascota_id
WHERE c.estado = 'COMPLETADA';

INSERT INTO prescripciones (consulta_id, veterinario_id, created_at, medicamento, dosis, frecuencia, duracion_dias, fecha_inicio, fecha_fin)
SELECT q.id, q.veterinario_id, q.fecha_consulta, 'Medicamento ' || (q.id % 20), '1 tableta', 'Cada 12 horas', 7,
       q.fecha_consulta::date, q.fecha_consulta::date + 7
FROM consulta q WHERE q.id % 2 = 0;

INSERT INTO diagnosticos (consulta_id, nombre, codigo_cie, descripcion, tipo, estado)
SELECT q.id, 'Diagnostico ' || (q.id % 30), 'A' || lpad((q.id % 90)::text, 2, '0'), 'Diagnostico de prueba', 'DEFINITIVO', 'RESUELTO'
FROM consulta q;

ANALYZE;

SELECT 'company' AS tabla, count(*) AS filas FROM company
UNION ALL SELECT 'usuario', count(*) FROM usuario
UNION ALL SELECT 'apoderado', count(*) FROM apoderado
UNION ALL SELECT 'mascota', count(*) FROM mascota
UNION ALL SELECT 'empleado', count(*) FROM empleado
UNION ALL SELECT 'citas', count(*) FROM citas
UNION ALL SELECT 'purchases', count(*) FROM purchases
UNION ALL SELECT 'historia_clinica', count(*) FROM historia_clinica
UNION ALL SELECT 'consulta', count(*) FROM consulta
UNION ALL SELECT 'prescripciones', count(*) FROM prescripciones
UNION ALL SELECT 'diagnosticos', count(*) FROM diagnosticos;
