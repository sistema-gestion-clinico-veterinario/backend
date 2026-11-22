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
| Veredicto | SATURADO (muy lento, casi sin errores) | AGUANTA |
| Errores (%) | 1.2 % | 0.3 % |
| Ritmo medio (peticiones/s) | 21.6 | 42.7 |
| Tiempo mediano (ms) | 2 911 | 54 |
| Tiempo p95 (ms) | 12 167 | 1 066 |
| Usuarios sin degradación | 117 | 250 |
| CPU del backend, media (% del límite) | 49 | 79 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 84 | 47 |
| Memoria pico (MB) | 1 348 | 1 352 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.6 | 53.4 |
| Tiempo SQL por carga del panel (ms, sin carga) | 231.1 | 34.6 |
| Panel sin carga: mediana de 19 llamadas (ms) | 417 | 148 |
| Panel sin carga: primera llamada (ms) | 629 | 633 |
| Panel bajo carga: mediana (ms) | 6 127 | 226 |
| Panel bajo carga: p95 (ms) | 16 441 | 1 166 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 131 | 247 |
| Pausas completas | 19 | 35 |
| Pausas completas por "Allocation Failure" | 5 | 22 |
| Tiempo en pausas completas (s) | 6.2 | 16.3 |
| Tiempo en GC / vida de la JVM | 3.9 % | 9.7 % |
| Heap tras GC, máximo (MB) | 974 | 986 |
| Avisos de pool agotado | 0 | 0 |

## Código anterior: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 2947; fallidas: 120.
- Acciones hechas: panel 518, agenda 349, historia 351, reportes 37, recepcion 547, atencion 228, cobro 76, cancelar 111; sin trabajo disponible: 428.
- CPU del backend: 49 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 84 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 348 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **117 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 17.1 s.
- Correo: el servidor de pruebas recibió 547 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 135 MB (+1.2 %).
- GC: 131 pausas, 19.4 s en total (4 % del tiempo de vida de la JVM); 19 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 974 MB (tope -Xmx1g).
- Clases cargadas: 25423 en total, 87 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 19 | 4577 |
| 0.4 | 81 | 81 | 16110 |
| 0.5 | 87 | 88 | 17210 |
| 1.4 | 125 | 128 | 24091 |
| 2.3 | 131 | 133 | 24847 |
| 2.7 | 132 | 135 | 25080 |
| 3.4 | 133 | 136 | 25303 |
| 4.0 | 134 | 137 | 25336 |
| 4.4 | 134 | 137 | 25373 |
| 4.8 | 134 | 137 | 25373 |
| 5.5 | 134 | 137 | 25393 |
| 6.1 | 135 | 138 | 25397 |
| 6.8 | 135 | 138 | 25399 |
| 7.5 | 135 | 138 | 25404 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 84 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 85 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 768 | 885.9 | 1153.52 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 768 | 733.5 | 955.12 | 32 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 768 | 228.7 | 297.78 | 10 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 768 | 87.1 | 113.41 | 4 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 74 | 85.5 | 1154.86 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 768 | 84.8 | 110.42 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 768 | 82.6 | 107.56 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 351 | 42.9 | 122.11 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 74 | 29.0 | 392.35 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1117 | 9.9 | 8.89 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 547 | 547 | coincide |
| Mascotas | 547 | 547 | coincide |
| Citas agendadas | 538 | 538 | coincide |
| Diagnósticos | 117 | 117 | coincide |
| Recetas | 117 | 117 | coincide |
| Pagos | 76 | 76 | coincide |
| Citas completadas | 117 | 117 | coincide |
| Citas canceladas | 111 | 111 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 547 | 0 | 0.0 % | 3 074 | 11 721 |
| Registrar mascota | 547 | 0 | 0.0 % | 2 949 | 11 009 |
| Agendar cita | 538 | 9 | 1.6 % | 3 229 | 12 057 |
| Recepción (llegada) | 228 | 0 | 0.0 % | 4 196 | 12 014 |
| Iniciar atención | 117 | 111 | 48.7 % | 4 266 | 11 686 |
| Leer la consulta | 117 | 0 | 0.0 % | 1 033 | 14 102 |
| Registrar diagnóstico | 117 | 0 | 0.0 % | 574 | 11 985 |
| Registrar receta | 117 | 0 | 0.0 % | 1 750 | 11 697 |
| Actualizar mascota | 117 | 0 | 0.0 % | 1 138 | 10 448 |
| Cerrar la consulta | 117 | 0 | 0.0 % | 1 384 | 12 071 |
| Caja: crear punto de cobro | 66 | 0 | 0.0 % | 2 784 | 9 606 |
| Caja: vincular equipo | 66 | 0 | 0.0 % | 2 120 | 9 607 |
| Caja: abrir la caja | 66 | 0 | 0.0 % | 2 272 | 11 553 |
| Cobrar | 76 | 0 | 0.0 % | 3 159 | 11 434 |
| Cancelar cita | 111 | 0 | 0.0 % | 4 631 | 11 062 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 768 | 0 (0 %) | 6 127 | 16 441 | 13 % |
| avisos_de_cumplimiento | 768 | 0 (0 %) | 2 177 | 11 209 | 6 % |
| navegacion | 768 | 0 (0 %) | 2 347 | 10 789 | 6 % |
| cumplimiento | 768 | 0 (0 %) | 1 764 | 10 725 | 6 % |
| notificaciones | 768 | 0 (0 %) | 1 532 | 10 233 | 6 % |
| cita | 547 | 9 (2 %) | 3 229 | 12 057 | 6 % |
| apoderado | 547 | 0 (0 %) | 3 074 | 11 721 | 5 % |
| mascota | 547 | 0 (0 %) | 2 949 | 11 009 | 5 % |
| privacidad | 768 | 0 (0 %) | 1 375 | 8 287 | 5 % |
| propietarios | 349 | 0 (0 %) | 4 667 | 11 980 | 4 % |
| citas_del_mes | 349 | 0 (0 %) | 3 831 | 13 538 | 4 % |
| lista_de_historias | 351 | 0 (0 %) | 3 980 | 13 239 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 91 | 566 | 0 % |
| 0.5 | 42 | 221 | 603 | 0 % |
| 1.0 | 72 | 326 | 579 | 0 % |
| 1.5 | 102 | 423 | 421 | 0 % |
| 2.0 | 132 | 446 | 2 198 | 0 % |
| 2.5 | 162 | 399 | 5 287 | 1 % |
| 3.0 | 192 | 372 | 5 576 | 1 % |
| 3.5 | 222 | 261 | 11 321 | 1 % |
| 4.0 | 250 | 287 | 16 304 | 1 % |
| 4.5 | 250 | 357 | 12 610 | 2 % |
| 5.0 | 250 | 323 | 14 795 | 2 % |
| 5.5 | 250 | 301 | 11 201 | 1 % |
| 6.0 | 250 | 258 | 15 033 | 2 % |
| 6.5 | 250 | 296 | 12 707 | 3 % |
| 7.0 | 217 | 192 | 11 899 | 4 % |
| 7.5 | 54 | 7 | 11 255 | 0 % |

</details>

## Código nuevo: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 6968; fallidas: 54.
- Acciones hechas: panel 975, agenda 785, historia 651, reportes 86, recepcion 942, atencion 479, cobro 295, cancelar 185; sin trabajo disponible: 675.
- CPU del backend: 79 % de media y 104 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 50 % del tiempo.
- CPU de PostgreSQL: 47 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 352 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 3.3 s.
- Correo: el servidor de pruebas recibió 942 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 247 pausas, 46.4 s en total (10 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 986 MB (tope -Xmx1g).
- Clases cargadas: 25547 en total, 167 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.5 | 86 | 87 | 17109 |
| 1.8 | 129 | 131 | 24528 |
| 2.7 | 132 | 135 | 25032 |
| 3.3 | 133 | 136 | 25297 |
| 4.0 | 134 | 137 | 25380 |
| 4.5 | 135 | 138 | 25439 |
| 5.0 | 135 | 138 | 25457 |
| 5.3 | 135 | 138 | 25458 |
| 5.7 | 136 | 138 | 25459 |
| 6.2 | 136 | 139 | 25500 |
| 6.5 | 136 | 139 | 25528 |
| 7.0 | 136 | 139 | 25546 |
| 7.4 | 136 | 139 | 25546 |
| 8.0 | 136 | 139 | 25547 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 155 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 162 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 172 | 98.4 | 571.96 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1225 | 74.1 | 60.49 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1225 | 73.1 | 59.66 | 18 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 651 | 37.7 | 57.98 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 172 | 34.1 | 198.06 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2010 | 18.4 | 9.16 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1225 | 7.1 | 5.82 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2010 | 6.3 | 3.15 | 2 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 86 | 5.3 | 61.48 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2010 | 5.2 | 2.58 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 942 | 942 | coincide |
| Mascotas | 942 | 942 | coincide |
| Citas agendadas | 927 | 927 | coincide |
| Diagnósticos | 440 | 440 | coincide |
| Recetas | 440 | 440 | coincide |
| Pagos | 295 | 295 | coincide |
| Citas completadas | 440 | 440 | coincide |
| Citas canceladas | 185 | 185 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 942 | 0 | 0.0 % | 84 | 1 164 |
| Registrar mascota | 942 | 0 | 0.0 % | 49 | 970 |
| Agendar cita | 927 | 15 | 1.6 % | 73 | 930 |
| Recepción (llegada) | 479 | 0 | 0.0 % | 43 | 886 |
| Iniciar atención | 440 | 39 | 8.1 % | 58 | 1 034 |
| Leer la consulta | 440 | 0 | 0.0 % | 32 | 627 |
| Registrar diagnóstico | 440 | 0 | 0.0 % | 31 | 828 |
| Registrar receta | 440 | 0 | 0.0 % | 33 | 593 |
| Actualizar mascota | 440 | 0 | 0.0 % | 33 | 656 |
| Cerrar la consulta | 440 | 0 | 0.0 % | 55 | 739 |
| Caja: crear punto de cobro | 186 | 0 | 0.0 % | 35 | 981 |
| Caja: vincular equipo | 186 | 0 | 0.0 % | 34 | 738 |
| Caja: abrir la caja | 186 | 0 | 0.0 % | 41 | 571 |
| Cobrar | 295 | 0 | 0.0 % | 49 | 829 |
| Cancelar cita | 185 | 0 | 0.0 % | 60 | 1 072 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| reporte_clinico | 86 | 0 (0 %) | 5 957 | 9 748 | 11 % |
| panel | 1225 | 0 (0 %) | 226 | 1 166 | 10 % |
| login | 250 | 0 (0 %) | 952 | 3 787 | 8 % |
| propietarios | 785 | 0 (0 %) | 172 | 1 244 | 5 % |
| apoderado | 942 | 0 (0 %) | 84 | 1 164 | 5 % |
| lista_de_historias | 651 | 0 (0 %) | 142 | 1 347 | 5 % |
| navegacion | 1225 | 0 (0 %) | 27 | 986 | 5 % |
| citas_del_mes | 785 | 0 (0 %) | 74 | 1 149 | 4 % |
| cita | 942 | 15 (2 %) | 73 | 930 | 4 % |
| privacidad | 1225 | 0 (0 %) | 27 | 811 | 4 % |
| mascota | 942 | 0 (0 %) | 49 | 970 | 4 % |
| cumplimiento | 1225 | 0 (0 %) | 28 | 767 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 77 | 408 | 0 % |
| 0.5 | 41 | 219 | 540 | 0 % |
| 0.9 | 71 | 310 | 450 | 0 % |
| 1.4 | 101 | 488 | 230 | 0 % |
| 1.9 | 131 | 592 | 340 | 0 % |
| 2.5 | 161 | 619 | 1 489 | 0 % |
| 3.0 | 191 | 758 | 432 | 0 % |
| 3.5 | 221 | 902 | 365 | 0 % |
| 4.0 | 250 | 879 | 785 | 0 % |
| 4.5 | 250 | 849 | 1 590 | 1 % |
| 5.0 | 250 | 903 | 332 | 1 % |
| 5.5 | 250 | 913 | 456 | 0 % |
| 6.0 | 250 | 931 | 266 | 0 % |
| 6.5 | 250 | 963 | 205 | 0 % |
| 7.0 | 175 | 192 | 185 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
