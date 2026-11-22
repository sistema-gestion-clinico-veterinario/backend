# Fase 3e: 250 usuarios (1 CPU, 2 GB)

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| | Código anterior, configuración actual | Código anterior + plan por consulta y sin JIT |
|---|---|---|
| Veredicto | SATURADO (muy lento, casi sin errores) | DEGRADADO |
| Errores (%) | 1.5 % | 0.6 % |
| Ritmo medio (peticiones/s) | 28.7 | 39.7 |
| Tiempo mediano (ms) | 1 316 | 99 |
| Tiempo p95 (ms) | 6 939 | 2 528 |
| Usuarios sin degradación | 130 | 139 |
| Peticiones sin respuesta (tiempo agotado) | 0 | 0 |
| Panel bajo carga: mediana (ms) | 3 120 | 296 |
| Panel bajo carga: p95 (ms) | 9 060 | 2 029 |
| Sentencias SQL por carga del panel (sonda) | 60.8 | 60.8 |
| Tiempo SQL por carga del panel (ms, sin carga) | 134.8 | 42.7 |
| CPU del backend por 1.000 peticiones (s) | 23.4 | 20.4 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 28.4 | 12.6 |
| CPU total por 1.000 peticiones (s) | 51.8 | 33.0 |
| CPU del backend, media (% del límite) | 68 | 83 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 82 | 50 |
| Memoria pico (MB) | 1 304 | 1 358 |
| Pausas completas de GC | 27 | 33 |
| Tiempo en GC | 6.3 % | 9.8 % |
| Heap tras GC, máximo (MB) | 934 | 982 |
| Planes lentos muestreados (>= 300 ms) | 85 | 16 |
| ...de ellos con compilación JIT | 0 | 0 |
| Avisos de pool agotado | 0 | 0 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off |

## Código anterior, configuración actual: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- CPU del backend: 68 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 35 % del tiempo.
- CPU de PostgreSQL: 82 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 304 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **130 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 13.8 s.
- Correo: el servidor de pruebas recibió 722 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 160 pausas, 30.8 s en total (6 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 934 MB (tope -Xmx1g).
- Clases cargadas: 25470 en total, 162 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 918 | 799.5 | 870.88 | 44 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 918 | 279.6 | 304.61 | 15 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 918 | 228.3 | 248.65 | 12 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 918 | 103.0 | 112.25 | 6 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 86 | 89.8 | 1043.65 | 5 % |

## Código anterior + plan por consulta y sin JIT: DEGRADADO; datos CONSISTENTES

- CPU del backend: 83 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 57 % del tiempo.
- CPU de PostgreSQL: 50 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 358 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **139 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.0 s.
- Correo: el servidor de pruebas recibió 858 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 230 pausas, 46.4 s en total (10 % del tiempo de vida de la JVM); 33 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 982 MB (tope -Xmx1g).
- Clases cargadas: 25566 en total, 246 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 154 | 92.3 | 599.67 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1149 | 48.6 | 42.26 | 13 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 1149 | 43.1 | 37.52 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 602 | 35.7 | 59.28 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 154 | 29.4 | 190.95 | 8 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
