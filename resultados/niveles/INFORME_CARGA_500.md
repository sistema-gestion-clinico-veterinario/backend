# Fase 3e: 500 usuarios (1 CPU, 2 GB)

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| | Código nuevo, configuración actual | Código nuevo + plan por consulta y sin JIT | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB |
|---|---|---|---|
| Veredicto | SATURADO (muy lento, casi sin errores) | SATURADO (muy lento, casi sin errores) | DEGRADADO |
| Errores (%) | 1.5 % | 1.5 % | 1.5 % |
| Ritmo medio (peticiones/s) | 54.9 | 58.6 | 61.5 |
| Tiempo mediano (ms) | 548 | 498 | 311 |
| Tiempo p95 (ms) | 7 624 | 5 888 | 4 466 |
| Usuarios sin degradación | 240 | 192 | 308 |
| Peticiones sin respuesta (tiempo agotado) | 0 | 0 | 0 |
| Panel bajo carga: mediana (ms) | 719 | 617 | 501 |
| Panel bajo carga: p95 (ms) | 6 895 | 4 813 | 3 920 |
| Sentencias SQL por carga del panel (sonda) | 51.7 | 52.2 | 51.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 25.3 | 20.3 | 20.7 |
| CPU del backend por 1.000 peticiones (s) | 13.2 | 13.6 | 12.5 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 10.9 | 9.6 | 9.0 |
| CPU total por 1.000 peticiones (s) | 24.2 | 23.2 | 21.5 |
| CPU del backend, media (% del límite) | 74 | 79 | 78 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 60 | 56 | 55 |
| Memoria pico (MB) | 1 376 | 1 388 | 1 623 |
| Pausas completas de GC | 64 | 71 | 49 |
| Tiempo en GC | 11.6 % | 13.7 % | 11.9 % |
| Heap tras GC, máximo (MB) | 985 | 989 | 1 229 |
| Planes lentos muestreados (>= 300 ms) | 20 | 21 | 22 |
| ...de ellos con compilación JIT | 0 | 0 | 0 |
| Avisos de pool agotado | 0 | 0 | 0 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off | plan_cache_mode=force_custom_plan jit=off |

## Código nuevo, configuración actual: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 5 avisos.
- CPU del backend: 74 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 36 % del tiempo.
- CPU de PostgreSQL: 60 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 376 MB (67 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **240 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 13.8 s.
- Correo: el servidor de pruebas recibió 2076 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 135 MB (-0.8 %).
- GC: 493 pausas, 86.4 s en total (12 % del tiempo de vida de la JVM); 64 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 985 MB (tope -Xmx1g).
- Clases cargadas: 25625 en total, 101 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 2536 | 507.9 | 200.27 | 36 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 354 | 209.4 | 591.61 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2536 | 167.9 | 66.22 | 12 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2536 | 158.4 | 62.47 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1439 | 83.5 | 58.05 | 6 % |

## Código nuevo + plan por consulta y sin JIT: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 17 avisos.
- CPU del backend: 79 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 40 % del tiempo.
- CPU de PostgreSQL: 56 % de media y 102 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 388 MB (68 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **192 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 18.0 s.
- Correo: el servidor de pruebas recibió 2233 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 135 MB (-0.8 %).
- GC: 523 pausas, 100.7 s en total (14 % del tiempo de vida de la JVM); 71 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25613 en total, 103 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 376 | 193.2 | 513.93 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2706 | 110.5 | 40.82 | 14 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2706 | 101.4 | 37.48 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1550 | 96.3 | 62.12 | 13 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 376 | 53.9 | 143.34 | 7 % |

## Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB: DEGRADADO; datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 5 avisos.
- CPU del backend: 78 % de media y 104 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 35 % del tiempo.
- CPU de PostgreSQL: 55 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 623 MB (79 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **308 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 14.8 s.
- Correo: el servidor de pruebas recibió 2312 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.7 %).
- GC: 422 pausas, 84.6 s en total (12 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 1229 MB (tope -Xmx1g).
- Clases cargadas: 25605 en total, 79 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 396 | 210.8 | 532.24 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2784 | 116.5 | 41.83 | 14 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2784 | 106.0 | 38.07 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1570 | 94.9 | 60.45 | 12 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 396 | 57.8 | 146.04 | 7 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
