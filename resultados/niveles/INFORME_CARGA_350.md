# Fase 3d: panel optimizado: 350 usuarios (1 CPU, 2 GB)

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| | Código anterior | Código nuevo |
|---|---|---|
| Veredicto | FALLA | DEGRADADO |
| Errores (%) | 29.7 % | 1.2 % |
| Ritmo medio (peticiones/s) | 14.8 | 45.7 |
| Tiempo mediano (ms) | 2 844 | 374 |
| Tiempo p95 (ms) | 30 001 | 4 601 |
| Usuarios sin degradación | 108 | 207 |
| CPU del backend, media (% del límite) | 36 | 85 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 | 55 |
| Memoria pico (MB) | 1 353 | 1 366 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 62.1 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 194.7 | 35.6 |
| Panel sin carga: mediana de 19 llamadas (ms) | 422 | 147 |
| Panel sin carga: primera llamada (ms) | 679 | 615 |
| Panel bajo carga: mediana (ms) | 4 382 | 737 |
| Panel bajo carga: p95 (ms) | 30 001 | 3 795 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 95 | 323 |
| Pausas completas | 17 | 47 |
| Pausas completas por "Allocation Failure" | 2 | 33 |
| Tiempo en pausas completas (s) | 4.8 | 26.6 |
| Tiempo en GC / vida de la JVM | 2.1 % | 11.1 % |
| Heap tras GC, máximo (MB) | 960 | 989 |
| Avisos de pool agotado | 2 786 | 0 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 1685; fallidas: 494.
- Acciones hechas: panel 359, agenda 362, historia 306, reportes 34, recepcion 571, atencion 183, cobro 51, cancelar 57; sin trabajo disponible: 412.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2786 avisos en el registro.
- CPU del backend: 36 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 353 MB (66 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.6 con unos 288 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **108 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 361 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 135 MB (+0.8 %).
- GC: 95 pausas, 13.2 s en total (2 % del tiempo de vida de la JVM); 17 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 960 MB (tope -Xmx1g).
- Clases cargadas: 25454 en total, 77 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.3 | 80 | 81 | 16057 |
| 0.4 | 81 | 82 | 16156 |
| 0.5 | 87 | 88 | 17214 |
| 0.8 | 116 | 117 | 22483 |
| 1.6 | 127 | 130 | 24289 |
| 2.1 | 130 | 133 | 24747 |
| 2.5 | 132 | 134 | 24973 |
| 3.0 | 133 | 135 | 25189 |
| 3.3 | 133 | 136 | 25292 |
| 3.6 | 133 | 136 | 25308 |
| 3.9 | 133 | 136 | 25309 |
| 4.3 | 134 | 137 | 25357 |
| 4.7 | 134 | 137 | 25359 |
| 5.1 | 134 | 137 | 25377 |
| 7.3 | 135 | 138 | 25451 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 33 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 37 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 464 | 303.3 | 653.69 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 464 | 290.5 | 626.16 | 34 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 464 | 93.9 | 202.41 | 11 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 464 | 35.7 | 76.91 | 4 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 42 | 34.4 | 819.91 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 464 | 32.9 | 70.86 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 464 | 19.6 | 42.32 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 198 | 17.2 | 86.9 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 42 | 11.9 | 283.42 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 702 | 3.6 | 5.09 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 339 | 362 | la base tiene más (ver nota) |
| Mascotas | 322 | 324 | la base tiene más (ver nota) |
| Citas agendadas | 304 | 306 | la base tiene más (ver nota) |
| Diagnósticos | 69 | 70 | la base tiene más (ver nota) |
| Recetas | 67 | 68 | la base tiene más (ver nota) |
| Pagos | 37 | 38 | la base tiene más (ver nota) |
| Citas completadas | 65 | 66 | la base tiene más (ver nota) |
| Citas canceladas | 37 | 42 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 339 | 232 | 40.6 % | 5 561 | 30 001 |
| Registrar mascota | 322 | 17 | 5.0 % | 837 | 30 000 |
| Agendar cita | 304 | 18 | 5.6 % | 755 | 12 204 |
| Recepción (llegada) | 126 | 57 | 31.1 % | 3 569 | 30 001 |
| Iniciar atención | 73 | 110 | 60.1 % | 5 062 | 30 001 |
| Leer la consulta | 72 | 1 | 1.4 % | 360 | 8 527 |
| Registrar diagnóstico | 69 | 4 | 5.5 % | 226 | 30 000 |
| Registrar receta | 67 | 6 | 8.2 % | 167 | 30 000 |
| Actualizar mascota | 66 | 7 | 9.6 % | 166 | 30 000 |
| Cerrar la consulta | 65 | 8 | 11.0 % | 566 | 30 000 |
| Caja: crear punto de cobro | 36 | 9 | 20.0 % | 1 667 | 30 001 |
| Caja: vincular equipo | 36 | 0 | 0.0 % | 678 | 6 624 |
| Caja: abrir la caja | 36 | 0 | 0.0 % | 1 087 | 8 562 |
| Cobrar | 37 | 5 | 11.9 % | 1 561 | 30 001 |
| Cancelar cita | 37 | 20 | 35.1 % | 3 833 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 741 | 489 (66 %) | 30 000 | 30 001 | 17 % |
| apoderado | 571 | 232 (41 %) | 5 561 | 30 001 | 9 % |
| panel | 611 | 157 (26 %) | 4 382 | 30 001 | 7 % |
| privacidad | 684 | 186 (27 %) | 1 407 | 30 001 | 7 % |
| navegacion | 684 | 172 (25 %) | 1 806 | 30 001 | 7 % |
| cumplimiento | 669 | 182 (27 %) | 1 657 | 30 001 | 7 % |
| notificaciones | 654 | 177 (27 %) | 1 563 | 30 001 | 7 % |
| avisos_de_cumplimiento | 626 | 156 (25 %) | 1 271 | 30 001 | 6 % |
| empleados | 373 | 137 (37 %) | 4 713 | 30 001 | 5 % |
| propietarios | 362 | 136 (38 %) | 4 587 | 30 001 | 5 % |
| citas_del_mes | 373 | 130 (35 %) | 4 487 | 30 001 | 5 % |
| lista_de_historias | 306 | 118 (39 %) | 5 620 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 20 | 826 | 0 % |
| 0.3 | 33 | 159 | 529 | 1 % |
| 0.8 | 63 | 327 | 853 | 0 % |
| 1.3 | 93 | 398 | 631 | 1 % |
| 1.8 | 123 | 511 | 2 264 | 0 % |
| 2.3 | 153 | 464 | 4 846 | 2 % |
| 2.8 | 183 | 457 | 6 940 | 1 % |
| 3.3 | 213 | 430 | 12 035 | 1 % |
| 3.8 | 243 | 411 | 12 123 | 2 % |
| 4.3 | 273 | 118 | 15 632 | 2 % |
| 4.8 | 303 | 164 | 30 001 | 92 % |
| 5.3 | 333 | 144 | 30 001 | 100 % |
| 5.8 | 350 | 135 | 30 001 | 100 % |
| 6.3 | 350 | 154 | 30 001 | 100 % |
| 6.8 | 350 | 151 | 30 001 | 100 % |
| 7.3 | 350 | 133 | 30 001 | 100 % |
| 7.8 | 350 | 133 | 30 001 | 100 % |
| 8.3 | 350 | 128 | 30 001 | 100 % |
| 8.8 | 276 | 116 | 30 001 | 100 % |
| 9.3 | 140 | 51 | 30 001 | 100 % |

</details>

## Código nuevo: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 8234; fallidas: 303.
- Acciones hechas: panel 1326, agenda 1076, historia 856, reportes 112, recepcion 1322, atencion 692, cobro 298, cancelar 263; sin trabajo disponible: 1018.
- El servidor rechazó trabajo por falta de hilos: 10 avisos.
- CPU del backend: 85 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 56 % del tiempo.
- CPU de PostgreSQL: 55 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 366 MB (67 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **207 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 15.7 s.
- Correo: el servidor de pruebas recibió 1312 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.3 %).
- GC: 323 pausas, 64.0 s en total (11 % del tiempo de vida de la JVM); 47 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25549 en total, 173 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.5 | 89 | 90 | 17420 |
| 2.3 | 131 | 134 | 24888 |
| 3.4 | 133 | 136 | 25311 |
| 4.2 | 134 | 137 | 25376 |
| 4.9 | 135 | 138 | 25451 |
| 5.4 | 135 | 138 | 25452 |
| 5.9 | 136 | 139 | 25492 |
| 6.5 | 136 | 139 | 25494 |
| 6.9 | 136 | 139 | 25509 |
| 7.5 | 136 | 139 | 25548 |
| 8.0 | 136 | 139 | 25548 |
| 8.4 | 135 | 139 | 25549 |
| 8.8 | 135 | 139 | 25549 |
| 9.3 | 135 | 139 | 25549 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 154 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 161 |
| `ch.qos.logback.classic` | 6 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `org.springframework.transaction.event` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 224 | 172.5 | 769.94 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1676 | 160.3 | 95.64 | 20 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then $2 else $3 end),$4) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apo` | 1676 | 153.0 | 91.29 | 19 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 856 | 69.1 | 80.78 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 224 | 52.7 | 235.25 | 6 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1676 | 48.5 | 28.94 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2752 | 40.1 | 14.57 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 2752 | 14.3 | 5.21 | 2 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 112 | 10.0 | 89.23 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 2752 | 9.0 | 3.26 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1322 | 1322 | coincide |
| Mascotas | 1322 | 1322 | coincide |
| Citas agendadas | 1301 | 1301 | coincide |
| Diagnósticos | 410 | 410 | coincide |
| Recetas | 410 | 410 | coincide |
| Pagos | 298 | 298 | coincide |
| Citas completadas | 410 | 410 | coincide |
| Citas canceladas | 263 | 263 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1322 | 0 | 0.0 % | 486 | 5 307 |
| Registrar mascota | 1322 | 0 | 0.0 % | 346 | 4 751 |
| Agendar cita | 1301 | 21 | 1.6 % | 398 | 4 531 |
| Recepción (llegada) | 692 | 0 | 0.0 % | 530 | 5 274 |
| Iniciar atención | 410 | 282 | 40.8 % | 571 | 5 157 |
| Leer la consulta | 410 | 0 | 0.0 % | 119 | 3 214 |
| Registrar diagnóstico | 410 | 0 | 0.0 % | 122 | 3 042 |
| Registrar receta | 410 | 0 | 0.0 % | 128 | 3 419 |
| Actualizar mascota | 410 | 0 | 0.0 % | 139 | 3 041 |
| Cerrar la consulta | 410 | 0 | 0.0 % | 204 | 3 188 |
| Caja: crear punto de cobro | 192 | 0 | 0.0 % | 271 | 4 185 |
| Caja: vincular equipo | 192 | 0 | 0.0 % | 232 | 4 653 |
| Caja: abrir la caja | 192 | 0 | 0.0 % | 129 | 3 771 |
| Cobrar | 298 | 0 | 0.0 % | 358 | 5 129 |
| Cancelar cita | 263 | 0 | 0.0 % | 569 | 4 233 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1676 | 0 (0 %) | 737 | 3 795 | 7 % |
| navegacion | 1676 | 0 (0 %) | 285 | 5 075 | 7 % |
| apoderado | 1322 | 0 (0 %) | 486 | 5 307 | 6 % |
| mascota | 1322 | 0 (0 %) | 346 | 4 751 | 5 % |
| cita | 1322 | 21 (2 %) | 398 | 4 531 | 5 % |
| notificaciones | 1676 | 0 (0 %) | 200 | 3 449 | 5 % |
| privacidad | 1676 | 0 (0 %) | 249 | 3 421 | 5 % |
| citas_del_mes | 1076 | 0 (0 %) | 527 | 4 482 | 5 % |
| cumplimiento | 1676 | 0 (0 %) | 202 | 3 311 | 5 % |
| avisos_de_cumplimiento | 1676 | 0 (0 %) | 197 | 3 187 | 5 % |
| propietarios | 1076 | 0 (0 %) | 546 | 4 910 | 5 % |
| empleados | 1076 | 0 (0 %) | 367 | 4 860 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 84 | 465 | 0 % |
| 0.5 | 42 | 205 | 848 | 0 % |
| 1.0 | 72 | 339 | 211 | 0 % |
| 1.5 | 102 | 453 | 283 | 0 % |
| 2.0 | 132 | 563 | 155 | 0 % |
| 2.5 | 162 | 684 | 208 | 0 % |
| 3.0 | 192 | 811 | 212 | 0 % |
| 3.5 | 222 | 671 | 5 340 | 2 % |
| 4.0 | 252 | 800 | 1 604 | 0 % |
| 4.5 | 282 | 943 | 4 615 | 1 % |
| 5.0 | 312 | 702 | 5 141 | 2 % |
| 5.5 | 342 | 1018 | 3 010 | 1 % |
| 6.0 | 350 | 904 | 15 682 | 2 % |
| 6.5 | 350 | 920 | 3 056 | 3 % |
| 7.0 | 350 | 1183 | 988 | 1 % |
| 7.5 | 350 | 658 | 10 781 | 3 % |
| 8.0 | 350 | 1074 | 3 615 | 3 % |
| 8.5 | 325 | 683 | 791 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
