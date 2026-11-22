# Fase 3e: 350 usuarios (1 CPU, 2 GB)

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
| Veredicto | SATURADO (muy lento, casi sin errores) | FALLA |
| Errores (%) | 1.9 % | 25.1 % |
| Ritmo medio (peticiones/s) | 34.7 | 17.4 |
| Tiempo mediano (ms) | 1 699 | 259 |
| Tiempo p95 (ms) | 9 039 | 30 001 |
| Usuarios sin degradación | 148 | 171 |
| Peticiones sin respuesta (tiempo agotado) | 0 | 2 541 |
| Panel bajo carga: mediana (ms) | 2 864 | 230 |
| Panel bajo carga: p95 (ms) | 9 570 | 30 001 |
| Sentencias SQL por carga del panel (sonda) | 60.8 | 60.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 133.8 | 41.4 |
| CPU del backend por 1.000 peticiones (s) | 21.4 | 24.7 |
| CPU de PostgreSQL por 1.000 peticiones (s) | 22.6 | 8.8 |
| CPU total por 1.000 peticiones (s) | 43.9 | 33.5 |
| CPU del backend, media (% del límite) | 76 | 47 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 78 | 15 |
| Memoria pico (MB) | 1 361 | 1 327 |
| Pausas completas de GC | 36 | 24 |
| Tiempo en GC | 9.1 % | 3.9 % |
| Heap tras GC, máximo (MB) | 989 | 933 |
| Planes lentos muestreados (>= 300 ms) | 79 | 6 |
| ...de ellos con compilación JIT | 0 | 0 |
| Avisos de pool agotado | 0 | 2 703 |
| Configuración de PostgreSQL | plan_cache_mode=auto jit=on | plan_cache_mode=force_custom_plan jit=off |

## Código anterior, configuración actual: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- CPU del backend: 76 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 39 % del tiempo.
- CPU de PostgreSQL: 78 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 361 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 7.2 con unos 350 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **148 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 16.9 s.
- Correo: el servidor de pruebas recibió 1069 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.3 %).
- GC: 267 pausas, 52.6 s en total (9 % del tiempo de vida de la JVM); 36 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25530 en total, 138 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1325 | 551.1 | 415.89 | 29 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1325 | 281.1 | 212.18 | 15 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 186 | 184.5 | 992.04 | 10 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1325 | 181.9 | 137.29 | 10 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1325 | 151.6 | 114.44 | 8 % |

## Código anterior + plan por consulta y sin JIT: FALLA; datos CONSISTENTES

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2703 avisos en el registro.
- CPU del backend: 47 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 29 % del tiempo.
- CPU de PostgreSQL: 15 % de media y 95 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 327 MB (65 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.4 con unos 276 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **171 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 423 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+0.9 %).
- GC: 126 pausas, 23.3 s en total (4 % del tiempo de vida de la JVM); 24 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 933 MB (tope -Xmx1g).
- Clases cargadas: 25492 en total, 84 en la segunda mitad de la vida de la JVM.

**Consultas que más tiempo de PostgreSQL consumen (las 5 primeras):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 70 | 27.5 | 392.79 | 28 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 551 | 11.1 | 20.19 | 11 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 551 | 10.3 | 18.62 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 264 | 8.3 | 31.54 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 70 | 7.1 | 101.68 | 7 % |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen; solo cambian la configuración de PostgreSQL y el heap. Una corrida por variante y nivel.
