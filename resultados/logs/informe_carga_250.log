# Fase 3d: panel optimizado: 250 usuarios (1 CPU, 2 GB)

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
| Veredicto | SATURADO (muy lento, casi sin errores) | DEGRADADO |
| Errores (%) | 1.1 % | 0.4 % |
| Ritmo medio (peticiones/s) | 30.4 | 41.2 |
| Tiempo mediano (ms) | 766 | 56 |
| Tiempo p95 (ms) | 7 035 | 2 226 |
| Usuarios sin degradación | 106 | 25 |
| CPU del backend, media (% del límite) | 64 | 76 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 79 | 42 |
| Memoria pico (MB) | 1 313 | 1 349 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.6 | 53.1 |
| Tiempo SQL por carga del panel (ms, sin carga) | 233.5 | 34.6 |
| Panel sin carga: mediana de 19 llamadas (ms) | 421 | 150 |
| Panel sin carga: primera llamada (ms) | 629 | 592 |
| Panel bajo carga: mediana (ms) | 2 636 | 221 |
| Panel bajo carga: p95 (ms) | 9 329 | 1 645 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 179 | 236 |
| Pausas completas | 24 | 35 |
| Pausas completas por "Allocation Failure" | 11 | 22 |
| Tiempo en pausas completas (s) | 9.2 | 16.9 |
| Tiempo en GC / vida de la JVM | 6.2 % | 9.5 % |
| Heap tras GC, máximo (MB) | 942 | 969 |
| Avisos de pool agotado | 0 | 0 |

## Código anterior: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4372; fallidas: 155.
- Acciones hechas: panel 682, agenda 531, historia 465, reportes 58, recepcion 742, atencion 352, cobro 120, cancelar 123; sin trabajo disponible: 540.
- CPU del backend: 64 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 29 % del tiempo.
- CPU de PostgreSQL: 79 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 313 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **106 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 12.0 s.
- Correo: el servidor de pruebas recibió 742 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 179 pausas, 29.5 s en total (6 % del tiempo de vida de la JVM); 24 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 942 MB (tope -Xmx1g).
- Clases cargadas: 25467 en total, 174 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4517 |
| 0.4 | 81 | 82 | 16127 |
| 0.8 | 116 | 118 | 22527 |
| 2.1 | 130 | 133 | 24820 |
| 2.6 | 132 | 134 | 24975 |
| 3.1 | 133 | 136 | 25251 |
| 3.8 | 133 | 136 | 25305 |
| 4.5 | 134 | 137 | 25353 |
| 5.1 | 134 | 137 | 25356 |
| 5.8 | 135 | 138 | 25382 |
| 6.2 | 135 | 138 | 25398 |
| 6.6 | 135 | 138 | 25447 |
| 7.0 | 136 | 139 | 25466 |
| 7.4 | 136 | 139 | 25466 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 167 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 172 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 932 | 620.3 | 665.54 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 932 | 285.2 | 306.03 | 16 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 932 | 273.6 | 293.51 | 15 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 116 | 126.2 | 1087.74 | 7 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 932 | 111.6 | 119.69 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 932 | 104.5 | 112.14 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 932 | 99.7 | 106.96 | 6 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 465 | 56.9 | 122.37 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 116 | 41.3 | 356.45 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1463 | 16.8 | 11.48 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 742 | 742 | coincide |
| Mascotas | 742 | 742 | coincide |
| Citas agendadas | 730 | 730 | coincide |
| Diagnósticos | 209 | 209 | coincide |
| Recetas | 209 | 209 | coincide |
| Pagos | 120 | 120 | coincide |
| Citas completadas | 209 | 209 | coincide |
| Citas canceladas | 123 | 123 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 742 | 0 | 0.0 % | 972 | 6 805 |
| Registrar mascota | 742 | 0 | 0.0 % | 734 | 6 556 |
| Agendar cita | 730 | 12 | 1.6 % | 780 | 6 671 |
| Recepción (llegada) | 352 | 0 | 0.0 % | 993 | 6 838 |
| Iniciar atención | 209 | 143 | 40.6 % | 964 | 6 925 |
| Leer la consulta | 209 | 0 | 0.0 % | 205 | 6 111 |
| Registrar diagnóstico | 209 | 0 | 0.0 % | 264 | 5 348 |
| Registrar receta | 209 | 0 | 0.0 % | 296 | 6 132 |
| Actualizar mascota | 209 | 0 | 0.0 % | 248 | 4 546 |
| Cerrar la consulta | 209 | 0 | 0.0 % | 253 | 5 809 |
| Caja: crear punto de cobro | 103 | 0 | 0.0 % | 966 | 6 334 |
| Caja: vincular equipo | 103 | 0 | 0.0 % | 615 | 5 089 |
| Caja: abrir la caja | 103 | 0 | 0.0 % | 674 | 5 674 |
| Cobrar | 120 | 0 | 0.0 % | 731 | 5 990 |
| Cancelar cita | 123 | 0 | 0.0 % | 991 | 6 510 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 932 | 0 (0 %) | 2 636 | 9 329 | 13 % |
| navegacion | 932 | 0 (0 %) | 628 | 6 862 | 7 % |
| apoderado | 742 | 0 (0 %) | 972 | 6 805 | 6 % |
| cumplimiento | 932 | 0 (0 %) | 552 | 5 956 | 6 % |
| notificaciones | 932 | 0 (0 %) | 437 | 5 673 | 6 % |
| cita | 742 | 12 (2 %) | 780 | 6 671 | 5 % |
| avisos_de_cumplimiento | 932 | 0 (0 %) | 410 | 6 026 | 5 % |
| mascota | 742 | 0 (0 %) | 734 | 6 556 | 5 % |
| privacidad | 932 | 0 (0 %) | 422 | 5 290 | 5 % |
| propietarios | 531 | 0 (0 %) | 770 | 6 729 | 4 % |
| lista_de_historias | 465 | 0 (0 %) | 1 172 | 8 656 | 4 % |
| empleados | 531 | 0 (0 %) | 610 | 7 197 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 7 | 430 | 0 % |
| 0.3 | 31 | 156 | 687 | 0 % |
| 0.8 | 61 | 275 | 526 | 1 % |
| 1.3 | 91 | 394 | 864 | 0 % |
| 1.8 | 121 | 421 | 2 429 | 1 % |
| 2.3 | 151 | 394 | 4 324 | 2 % |
| 2.8 | 181 | 373 | 6 219 | 1 % |
| 3.3 | 211 | 338 | 10 132 | 1 % |
| 3.8 | 241 | 431 | 9 967 | 2 % |
| 4.3 | 250 | 377 | 8 525 | 1 % |
| 4.8 | 250 | 662 | 4 621 | 2 % |
| 5.3 | 250 | 944 | 1 168 | 1 % |
| 5.8 | 250 | 706 | 2 284 | 1 % |
| 6.3 | 250 | 498 | 6 510 | 3 % |
| 6.8 | 244 | 598 | 5 204 | 2 % |

</details>

## Código nuevo: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 6499; fallidas: 67.
- Acciones hechas: panel 939, agenda 773, historia 637, reportes 81, recepcion 891, atencion 461, cobro 257, cancelar 168; sin trabajo disponible: 638.
- CPU del backend: 76 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 49 % del tiempo.
- CPU de PostgreSQL: 42 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 349 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **25 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 8.2 s.
- Correo: el servidor de pruebas recibió 891 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 236 pausas, 44.2 s en total (9 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 969 MB (tope -Xmx1g).
- Clases cargadas: 25541 en total, 229 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4511 |
| 0.5 | 86 | 87 | 17109 |
| 1.8 | 129 | 131 | 24349 |
| 2.7 | 132 | 135 | 25009 |
| 3.1 | 133 | 136 | 25277 |
| 3.7 | 134 | 137 | 25329 |
| 4.1 | 134 | 137 | 25363 |
| 4.6 | 134 | 137 | 25379 |
| 5.0 | 135 | 138 | 25386 |
| 5.5 | 135 | 138 | 25453 |
| 5.9 | 136 | 138 | 25454 |
| 6.3 | 136 | 139 | 25495 |
| 6.9 | 136 | 139 | 25540 |
| 7.2 | 136 | 139 | 25541 |
| 7.6 | 136 | 139 | 25541 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 217 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 225 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 162 | 87.3 | 538.72 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then $2 else $3 end),$4) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apo` | 1189 | 70.3 | 59.14 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1189 | 69.5 | 58.42 | 18 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 637 | 35.9 | 56.42 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 162 | 30.0 | 185.06 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1962 | 17.4 | 8.85 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 1962 | 5.7 | 2.92 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 1962 | 5.0 | 2.56 | 1 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1189 | 4.8 | 4.05 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 81 | 4.5 | 56.1 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 891 | 891 | coincide |
| Mascotas | 891 | 891 | coincide |
| Citas agendadas | 876 | 876 | coincide |
| Diagnósticos | 409 | 409 | coincide |
| Recetas | 409 | 409 | coincide |
| Pagos | 257 | 257 | coincide |
| Citas completadas | 409 | 409 | coincide |
| Citas canceladas | 168 | 168 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 891 | 0 | 0.0 % | 88 | 3 258 |
| Registrar mascota | 891 | 0 | 0.0 % | 51 | 2 060 |
| Agendar cita | 876 | 15 | 1.7 % | 71 | 2 524 |
| Recepción (llegada) | 461 | 0 | 0.0 % | 42 | 2 217 |
| Iniciar atención | 409 | 52 | 11.3 % | 59 | 1 954 |
| Leer la consulta | 409 | 0 | 0.0 % | 34 | 595 |
| Registrar diagnóstico | 409 | 0 | 0.0 % | 33 | 890 |
| Registrar receta | 409 | 0 | 0.0 % | 37 | 940 |
| Actualizar mascota | 409 | 0 | 0.0 % | 34 | 799 |
| Cerrar la consulta | 409 | 0 | 0.0 % | 58 | 836 |
| Caja: crear punto de cobro | 167 | 0 | 0.0 % | 37 | 1 724 |
| Caja: vincular equipo | 167 | 0 | 0.0 % | 35 | 1 354 |
| Caja: abrir la caja | 167 | 0 | 0.0 % | 36 | 1 486 |
| Cobrar | 257 | 0 | 0.0 % | 50 | 1 453 |
| Cancelar cita | 168 | 0 | 0.0 % | 53 | 2 285 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 250 | 0 (0 %) | 1 060 | 9 100 | 9 % |
| panel | 1189 | 0 (0 %) | 221 | 1 645 | 8 % |
| reporte_clinico | 81 | 0 (0 %) | 6 563 | 9 854 | 7 % |
| apoderado | 891 | 0 (0 %) | 88 | 3 258 | 7 % |
| navegacion | 1189 | 0 (0 %) | 26 | 2 293 | 6 % |
| cita | 891 | 15 (2 %) | 71 | 2 524 | 5 % |
| avisos_de_cumplimiento | 1189 | 0 (0 %) | 22 | 2 052 | 5 % |
| notificaciones | 1189 | 0 (0 %) | 19 | 1 852 | 5 % |
| privacidad | 1189 | 0 (0 %) | 24 | 1 791 | 5 % |
| mascota | 891 | 0 (0 %) | 51 | 2 060 | 4 % |
| cumplimiento | 1189 | 0 (0 %) | 26 | 1 384 | 4 % |
| propietarios | 773 | 0 (0 %) | 161 | 1 605 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 10 | 76 | 389 | 0 % |
| 0.4 | 40 | 192 | 382 | 1 % |
| 0.9 | 70 | 283 | 432 | 0 % |
| 1.4 | 100 | 445 | 172 | 0 % |
| 1.9 | 130 | 546 | 287 | 0 % |
| 2.4 | 160 | 635 | 198 | 0 % |
| 2.9 | 190 | 559 | 2 768 | 0 % |
| 3.4 | 220 | 826 | 3 389 | 0 % |
| 3.9 | 250 | 705 | 8 229 | 2 % |
| 4.4 | 250 | 938 | 268 | 0 % |
| 4.9 | 250 | 931 | 531 | 1 % |
| 5.4 | 250 | 980 | 206 | 0 % |
| 5.9 | 250 | 943 | 164 | 0 % |
| 6.4 | 250 | 911 | 611 | 0 % |
| 6.9 | 181 | 200 | 74 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
