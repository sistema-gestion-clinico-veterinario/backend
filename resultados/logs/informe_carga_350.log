# Fase 3e: 350 usuarios (1 CPU, 2 GB)

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
| Veredicto | AGUANTA | DEGRADADO | AGUANTA |
| Errores (%) | 0.6 % | 0.7 % | 0.5 % |
| Ritmo medio (peticiones/s) | 54.1 | 53.5 | 55.9 |
| Tiempo mediano (ms) | 82 | 54 | 46 |
| Tiempo p95 (ms) | 1 635 | 2 073 | 1 188 |
| Usuarios sin degradación | 327 | 334 | 220 |
| Peticiones sin respuesta (tiempo agotado) | 0 | 0 | 0 |
| Panel bajo carga: mediana (ms) | 254 | 146 | 165 |
| Panel bajo carga: p95 (ms) | 1 609 | 1 823 | 1 132 |
| Sentencias SQL por carga del panel (sonda) | 51.6 | 51.6 | 51.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 24.8 | 20.0 | 19.8 |
| CPU del backend por 1.000 peticiones (s) | 14.1 | 13.2 | 12.5 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 8.9 | 8.8 | 8.9 |
| CPU total por 1.000 peticiones (s) | 23.0 | 22.0 | 21.4 |
| CPU del backend, media (% del límite) | 78 | 73 | 70 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 48 | 47 | 50 |
| Memoria pico (MB) | 1 371 | 1 361 | 1 593 |
| Pausas completas de GC | 62 | 55 | 37 |
| Tiempo en GC | 14.2 % | 12.5 % | 10.0 % |
| Heap tras GC, máximo (MB) | 989 | 989 | 1 216 |
| Planes lentos muestreados (>= 300 ms) | 14 | 10 | 11 |
| ...de ellos con compilación JIT | 0 | 0 | 0 |
| Avisos de pool agotado | 0 | 0 | 0 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off | plan_cache_mode=force_custom_plan jit=off |

## Código nuevo, configuración actual: AGUANTA; datos CONSISTENTES

- CPU del backend: 78 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 37 % del tiempo.
- CPU de PostgreSQL: 48 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 371 MB (67 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **327 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 14.3 s.
- Correo: el servidor de pruebas recibió 1494 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (-0.2 %).
- GC: 424 pausas, 80.1 s en total (14 % del tiempo de vida de la JVM); 62 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25584 en total, 204 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 328 | 157.8 | 481.18 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1911 | 110.6 | 57.86 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1911 | 103.8 | 54.29 | 15 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 328 | 48.7 | 148.36 | 7 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1022 | 48.4 | 47.39 | 7 % |

## Código nuevo + plan por consulta y sin JIT: DEGRADADO; datos CONSISTENTES

- CPU del backend: 73 % de media y 105 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 30 % del tiempo.
- CPU de PostgreSQL: 47 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 361 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **334 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 9.2 s.
- Correo: el servidor de pruebas recibió 1463 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (-0.0 %).
- GC: 388 pausas, 69.8 s en total (13 % del tiempo de vida de la JVM); 55 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25584 en total, 131 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 284 | 134.8 | 474.58 | 30 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1901 | 60.1 | 31.64 | 13 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1901 | 54.1 | 28.48 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1032 | 48.9 | 47.39 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 284 | 40.2 | 141.49 | 9 % |

## Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB: AGUANTA; datos CONSISTENTES

- CPU del backend: 70 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 24 % del tiempo.
- CPU de PostgreSQL: 50 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 593 MB (78 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **220 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.1 s.
- Correo: el servidor de pruebas recibió 1600 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 135 a 137 MB (+1.2 %).
- GC: 292 pausas, 56.3 s en total (10 % del tiempo de vida de la JVM); 37 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 1216 MB (tope -Xmx1g).
- Clases cargadas: 25584 en total, 130 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 264 | 128.0 | 484.72 | 28 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1898 | 60.7 | 31.96 | 13 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1898 | 55.7 | 29.34 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1085 | 55.0 | 50.69 | 12 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 264 | 40.0 | 151.54 | 9 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
