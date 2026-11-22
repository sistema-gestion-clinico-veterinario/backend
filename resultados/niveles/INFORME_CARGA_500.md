# Fase 3e: 500 usuarios (1 CPU, 2 GB)

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
| Veredicto | SATURADO (muy lento, casi sin errores) | SATURADO (muy lento, casi sin errores) | SATURADO (muy lento, casi sin errores) |
| Errores (%) | 2.8 % | 1.8 % | 2.0 % |
| Ritmo medio (peticiones/s) | 46.6 | 48.9 | 46.8 |
| Tiempo mediano (ms) | 1 053 | 1 037 | 1 350 |
| Tiempo p95 (ms) | 10 948 | 9 385 | 10 317 |
| Usuarios sin degradación | 215 | 192 | 202 |
| Peticiones sin respuesta (tiempo agotado) | 353 | 2 | 27 |
| Panel bajo carga: mediana (ms) | 1 134 | 1 092 | 1 376 |
| Panel bajo carga: p95 (ms) | 8 226 | 7 469 | 8 778 |
| Sentencias SQL por carga del panel (sonda) | 51.6 | 51.6 | 52.4 |
| Tiempo SQL por carga del panel (ms, sin carga) | 30.7 | 25.7 | 25.7 |
| CPU del backend por 1.000 peticiones (s) | 16.8 | 17.3 | 18.2 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 12.3 | 11.1 | 11.9 |
| CPU total por 1.000 peticiones (s) | 29.1 | 28.3 | 30.2 |
| CPU del backend, media (% del límite) | 80 | 86 | 87 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 57 | 54 | 56 |
| Memoria pico (MB) | 1 367 | 1 373 | 1 642 |
| Pausas completas de GC | 52 | 58 | 49 |
| Tiempo en GC | 9.9 % | 11.5 % | 12.0 % |
| Heap tras GC, máximo (MB) | 982 | 989 | 1 231 |
| Planes lentos muestreados (>= 300 ms) | 26 | 14 | 21 |
| ...de ellos con compilación JIT | 0 | 0 | 0 |
| Avisos de pool agotado | 147 | 0 | 0 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off | plan_cache_mode=force_custom_plan jit=off |

## Código nuevo, configuración actual: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Se agotó el pool de conexiones a la base de datos: 147 avisos en el registro.
- El servidor rechazó trabajo por falta de hilos: 24 avisos.
- CPU del backend: 80 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 45 % del tiempo.
- CPU de PostgreSQL: 57 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 367 MB (67 % de 2048 MB).
- Los fallos fueron: 39 % sin respuesta (tiempo agotado), 61 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 8.3 con unos 500 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **215 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 1811 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 135 MB (-0.6 %).
- GC: 398 pausas, 72.0 s en total (10 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 982 MB (tope -Xmx1g).
- Clases cargadas: 25637 en total, 141 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 2267 | 320.8 | 141.52 | 27 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2269 | 183.6 | 80.94 | 15 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2269 | 175.5 | 77.35 | 15 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 268 | 167.1 | 623.56 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1188 | 78.4 | 65.97 | 7 % |

## Código nuevo + plan por consulta y sin JIT: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 12 avisos.
- CPU del backend: 86 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 51 % del tiempo.
- CPU de PostgreSQL: 54 % de media y 102 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 373 MB (67 % de 2048 MB).
- Los fallos fueron: 0 % sin respuesta (tiempo agotado), 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **192 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 20.2 s.
- Correo: el servidor de pruebas recibió 1904 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 135 MB (-0.4 %).
- GC: 446 pausas, 83.5 s en total (12 % del tiempo de vida de la JVM); 58 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25593 en total, 139 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 316 | 177.1 | 560.39 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2299 | 113.6 | 49.41 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2299 | 105.1 | 45.71 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1254 | 87.0 | 69.39 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 316 | 54.0 | 171.04 | 7 % |

## Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- El servidor rechazó trabajo por falta de hilos: 57 avisos.
- CPU del backend: 87 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 59 % del tiempo.
- CPU de PostgreSQL: 56 % de media y 102 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 642 MB (80 % de 2048 MB).
- Los fallos fueron: 4 % sin respuesta (tiempo agotado), 96 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 9.6 con unos 500 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **202 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 26.7 s.
- Correo: el servidor de pruebas recibió 1837 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.6 %).
- GC: 368 pausas, 87.2 s en total (12 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 1231 MB (tope -Xmx1g).
- Clases cargadas: 25597 en total, 137 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 350 | 209.3 | 597.86 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2239 | 106.9 | 47.76 | 14 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2239 | 98.3 | 43.88 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1172 | 81.9 | 69.85 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 350 | 60.1 | 171.78 | 8 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
