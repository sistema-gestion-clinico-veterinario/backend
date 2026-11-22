# Fase 3e: 350 usuarios (1 CPU, 2 GB)

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| | Código nuevo, configuración actual | Código nuevo + plan por consulta y sin JIT | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB |
|---|---|---|---|
| Veredicto | FALLA | DEGRADADO | DEGRADADO |
| Errores (%) | 14.9 % | 1.0 % | 0.8 % |
| Ritmo medio (peticiones/s) | 24.8 | 46.7 | 52.7 |
| Tiempo mediano (ms) | 280 | 273 | 100 |
| Tiempo p95 (ms) | 30 001 | 4 362 | 2 171 |
| Usuarios sin degradación | 173 | 152 | 239 |
| Peticiones sin respuesta (tiempo agotado) | 2 082 | 0 | 0 |
| Panel bajo carga: mediana (ms) | 289 | 419 | 265 |
| Panel bajo carga: p95 (ms) | 30 001 | 3 428 | 1 867 |
| Sentencias SQL por carga del panel (sonda) | 51.6 | 52.1 | 51.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 30.4 | 25.4 | 25.0 |
| CPU del backend por 1.000 peticiones (s) | 22.8 | 18.3 | 14.8 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 8.4 | 11.1 | 10.1 |
| CPU total por 1.000 peticiones (s) | 31.2 | 29.3 | 24.9 |
| CPU del backend, media (% del límite) | 58 | 86 | 79 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 21 | 52 | 53 |
| Memoria pico (MB) | 1 364 | 1 397 | 1 627 |
| Pausas completas de GC | 33 | 43 | 29 |
| Tiempo en GC | 6.6 % | 11.4 % | 7.9 % |
| Heap tras GC, máximo (MB) | 972 | 989 | 1 237 |
| Planes lentos muestreados (>= 300 ms) | 5 | 7 | 11 |
| ...de ellos con compilación JIT | 0 | 0 | 0 |
| Avisos de pool agotado | 1 660 | 0 | 0 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off | plan_cache_mode=force_custom_plan jit=off |

## Código nuevo, configuración actual: FALLA; datos CONSISTENTES

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 1660 avisos en el registro.
- CPU del backend: 58 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 43 % del tiempo.
- CPU de PostgreSQL: 21 % de media y 97 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 364 MB (67 % de 2048 MB).
- Los fallos fueron: 96 % sin respuesta (tiempo agotado), 4 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.7 con unos 350 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **173 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 686 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 137 MB (+1.2 %).
- GC: 213 pausas, 40.9 s en total (7 % del tiempo de vida de la JVM); 33 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 972 MB (tope -Xmx1g).
- Clases cargadas: 25551 en total, 116 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 150 | 51.4 | 342.59 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 889 | 32.5 | 36.59 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 889 | 24.9 | 28.05 | 13 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 578 | 16.7 | 28.91 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 429 | 13.1 | 30.6 | 7 % |

## Código nuevo + plan por consulta y sin JIT: DEGRADADO; datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 8 avisos.
- CPU del backend: 86 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 55 % del tiempo.
- CPU de PostgreSQL: 52 % de media y 101 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 397 MB (68 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **152 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 15.7 s.
- Correo: el servidor de pruebas recibió 1354 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.7 %).
- GC: 332 pausas, 65.2 s en total (11 % del tiempo de vida de la JVM); 43 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25553 en total, 188 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 234 | 147.5 | 630.21 | 28 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1660 | 68.7 | 41.39 | 13 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1660 | 65.3 | 39.35 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 917 | 62.9 | 68.61 | 12 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 234 | 48.8 | 208.33 | 9 % |

## Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB: DEGRADADO; datos CONSISTENTES

- CPU del backend: 79 % de media y 104 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 38 % del tiempo.
- CPU de PostgreSQL: 53 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 627 MB (79 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **239 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 9.6 s.
- Correo: el servidor de pruebas recibió 1491 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 135 a 137 MB (+1.3 %).
- GC: 237 pausas, 45.0 s en total (8 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 1237 MB (tope -Xmx1g).
- Clases cargadas: 25543 en total, 166 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 125.0 | 664.7 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1840 | 80.2 | 43.6 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1840 | 74.7 | 40.62 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 987 | 72.1 | 73.0 | 13 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 36.8 | 195.85 | 7 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
