# Fase 3d: panel optimizado: 250 usuarios (1 CPU, 2 GB)

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| | Código anterior | Código nuevo |
|---|---|---|
| Veredicto | DEGRADADO | AGUANTA |
| Errores (%) | 1.3 % | 0.1 % |
| Ritmo medio (peticiones/s) | 32.4 | 44.9 |
| Tiempo mediano (ms) | 757 | 21 |
| Tiempo p95 (ms) | 4 702 | 188 |
| Usuarios sin degradación | 163 | 250 |
| CPU del backend, media (% del límite) | 54 | 62 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 76 | 34 |
| Memoria pico (MB) | 1 359 | 1 316 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 62.2 | 52.7 |
| Tiempo SQL por carga del panel (ms, sin carga) | 147.9 | 25.0 |
| Panel sin carga: mediana de 19 llamadas (ms) | 292 | 123 |
| Panel sin carga: primera llamada (ms) | 540 | 528 |
| Panel bajo carga: mediana (ms) | 2 509 | 90 |
| Panel bajo carga: p95 (ms) | 7 369 | 374 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 203 | 236 |
| Pausas completas | 28 | 29 |
| Pausas completas por "Allocation Failure" | 15 | 16 |
| Tiempo en pausas completas (s) | 11.9 | 12.4 |
| Tiempo en GC / vida de la JVM | 6.6 % | 7.3 % |
| Heap tras GC, máximo (MB) | 989 | 970 |
| Avisos de pool agotado | 0 | 0 |

## Código anterior: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4807; fallidas: 182.
- Acciones hechas: panel 716, agenda 543, historia 529, reportes 71, recepcion 809, atencion 391, cobro 146, cancelar 165; sin trabajo disponible: 638.
- CPU del backend: 54 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 76 % de media y 106 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 359 MB (66 % de 2048 MB).
- Los fallos fueron: 1 % errores 5xx del servidor, 99 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **163 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.7 s.
- Correo: el servidor de pruebas recibió 809 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.4 %).
- GC: 203 pausas, 30.7 s en total (7 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25562 en total, 186 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4513 |
| 0.3 | 82 | 83 | 16416 |
| 0.9 | 120 | 122 | 23025 |
| 2.1 | 131 | 134 | 24893 |
| 2.5 | 132 | 135 | 25020 |
| 3.1 | 133 | 136 | 25309 |
| 3.5 | 134 | 137 | 25376 |
| 4.1 | 134 | 137 | 25394 |
| 4.6 | 135 | 138 | 25402 |
| 5.3 | 135 | 138 | 25445 |
| 5.7 | 136 | 139 | 25500 |
| 6.2 | 136 | 139 | 25502 |
| 6.7 | 136 | 139 | 25505 |
| 7.0 | 136 | 139 | 25505 |
| 7.6 | 136 | 139 | 25562 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 147 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 152 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.dao.support` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `org.postgresql.core.EncodingPredictor` | 1 |
| `org.postgresql.util.ServerErrorMessage` | 1 |
| `org.postgresql.util.GT` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 966 | 648.4 | 671.21 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 966 | 448.2 | 463.99 | 26 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 966 | 186.7 | 193.32 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 142 | 102.5 | 722.0 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 966 | 77.0 | 79.7 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 966 | 71.9 | 74.47 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 966 | 71.3 | 73.77 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 529 | 40.0 | 75.67 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 142 | 28.5 | 201.04 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1509 | 7.5 | 4.98 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 809 | 809 | coincide |
| Mascotas | 809 | 809 | coincide |
| Citas agendadas | 792 | 792 | coincide |
| Diagnósticos | 226 | 226 | coincide |
| Recetas | 226 | 226 | coincide |
| Pagos | 146 | 146 | coincide |
| Citas completadas | 226 | 226 | coincide |
| Citas canceladas | 165 | 165 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 809 | 0 | 0.0 % | 1 118 | 4 564 |
| Registrar mascota | 809 | 0 | 0.0 % | 655 | 4 589 |
| Agendar cita | 792 | 17 | 2.1 % | 787 | 4 309 |
| Recepción (llegada) | 391 | 0 | 0.0 % | 1 252 | 4 894 |
| Iniciar atención | 226 | 165 | 42.2 % | 1 054 | 5 500 |
| Leer la consulta | 226 | 0 | 0.0 % | 343 | 4 118 |
| Registrar diagnóstico | 226 | 0 | 0.0 % | 349 | 4 043 |
| Registrar receta | 226 | 0 | 0.0 % | 441 | 4 119 |
| Actualizar mascota | 226 | 0 | 0.0 % | 431 | 4 043 |
| Cerrar la consulta | 226 | 0 | 0.0 % | 389 | 3 683 |
| Caja: crear punto de cobro | 113 | 0 | 0.0 % | 750 | 4 664 |
| Caja: vincular equipo | 113 | 0 | 0.0 % | 689 | 3 760 |
| Caja: abrir la caja | 113 | 0 | 0.0 % | 558 | 5 014 |
| Cobrar | 146 | 0 | 0.0 % | 782 | 4 102 |
| Cancelar cita | 165 | 0 | 0.0 % | 1 259 | 4 824 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 966 | 0 (0 %) | 2 509 | 7 369 | 14 % |
| apoderado | 809 | 0 (0 %) | 1 118 | 4 564 | 6 % |
| navegacion | 966 | 0 (0 %) | 586 | 4 033 | 6 % |
| mascota | 809 | 0 (0 %) | 655 | 4 589 | 5 % |
| cita | 809 | 17 (2 %) | 787 | 4 309 | 5 % |
| avisos_de_cumplimiento | 966 | 0 (0 %) | 419 | 3 894 | 5 % |
| cumplimiento | 966 | 0 (0 %) | 404 | 3 615 | 5 % |
| notificaciones | 966 | 0 (0 %) | 384 | 3 575 | 5 % |
| citas_del_mes | 543 | 0 (0 %) | 1 223 | 5 071 | 5 % |
| propietarios | 543 | 0 (0 %) | 1 070 | 4 753 | 4 % |
| lista_de_historias | 529 | 0 (0 %) | 1 074 | 4 621 | 4 % |
| privacidad | 966 | 0 (0 %) | 393 | 3 042 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 13 | 100 | 351 | 0 % |
| 0.5 | 43 | 241 | 386 | 0 % |
| 1.0 | 73 | 366 | 406 | 1 % |
| 1.5 | 103 | 429 | 438 | 0 % |
| 2.0 | 133 | 572 | 432 | 1 % |
| 2.5 | 163 | 622 | 813 | 0 % |
| 3.0 | 193 | 562 | 4 550 | 1 % |
| 3.5 | 223 | 509 | 5 478 | 2 % |
| 4.0 | 250 | 649 | 4 830 | 2 % |
| 4.5 | 250 | 455 | 6 686 | 2 % |
| 5.0 | 250 | 541 | 5 560 | 2 % |
| 5.5 | 250 | 649 | 6 606 | 3 % |
| 6.0 | 250 | 692 | 2 816 | 2 % |
| 6.5 | 250 | 719 | 4 547 | 2 % |
| 7.0 | 161 | 119 | 144 | 0 % |

</details>

## Código nuevo: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7433; fallidas: 25.
- Acciones hechas: panel 980, agenda 893, historia 662, reportes 81, recepcion 972, atencion 495, cobro 331, cancelar 209; sin trabajo disponible: 757.
- CPU del backend: 62 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 34 % de media y 88 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 316 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 972 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 236 pausas, 33.2 s en total (7 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 970 MB (tope -Xmx1g).
- Clases cargadas: 25549 en total, 182 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4501 |
| 0.3 | 86 | 87 | 17110 |
| 1.4 | 128 | 130 | 24259 |
| 2.5 | 132 | 135 | 25059 |
| 3.3 | 134 | 137 | 25333 |
| 3.7 | 134 | 137 | 25383 |
| 4.2 | 135 | 138 | 25458 |
| 4.6 | 135 | 138 | 25459 |
| 5.0 | 135 | 138 | 25499 |
| 5.3 | 136 | 139 | 25499 |
| 5.6 | 136 | 139 | 25501 |
| 6.1 | 136 | 139 | 25531 |
| 6.5 | 136 | 139 | 25531 |
| 6.8 | 136 | 139 | 25548 |
| 7.3 | 136 | 139 | 25549 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 169 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 176 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `java.util.concurrent.atomic` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 162 | 50.3 | 310.4 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1230 | 36.8 | 29.93 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1230 | 32.3 | 26.27 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 662 | 18.2 | 27.52 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 162 | 16.3 | 100.35 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2123 | 9.2 | 4.31 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1230 | 6.4 | 5.24 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2123 | 3.1 | 1.48 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2123 | 3.1 | 1.45 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 119203 | 3.0 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 972 | 972 | coincide |
| Mascotas | 972 | 972 | coincide |
| Citas agendadas | 956 | 956 | coincide |
| Diagnósticos | 486 | 486 | coincide |
| Recetas | 486 | 486 | coincide |
| Pagos | 331 | 331 | coincide |
| Citas completadas | 486 | 486 | coincide |
| Citas canceladas | 209 | 209 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 972 | 0 | 0.0 % | 43 | 230 |
| Registrar mascota | 972 | 0 | 0.0 % | 24 | 74 |
| Agendar cita | 956 | 16 | 1.6 % | 35 | 101 |
| Recepción (llegada) | 495 | 0 | 0.0 % | 19 | 129 |
| Iniciar atención | 486 | 9 | 1.8 % | 26 | 81 |
| Leer la consulta | 486 | 0 | 0.0 % | 16 | 57 |
| Registrar diagnóstico | 486 | 0 | 0.0 % | 15 | 42 |
| Registrar receta | 486 | 0 | 0.0 % | 16 | 50 |
| Actualizar mascota | 486 | 0 | 0.0 % | 17 | 44 |
| Cerrar la consulta | 486 | 0 | 0.0 % | 28 | 75 |
| Caja: crear punto de cobro | 194 | 0 | 0.0 % | 15 | 101 |
| Caja: vincular equipo | 194 | 0 | 0.0 % | 14 | 43 |
| Caja: abrir la caja | 194 | 0 | 0.0 % | 17 | 44 |
| Cobrar | 331 | 0 | 0.0 % | 23 | 101 |
| Cancelar cita | 209 | 0 | 0.0 % | 24 | 319 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| reporte_clinico | 81 | 0 (0 %) | 2 215 | 4 820 | 18 % |
| panel | 1230 | 0 (0 %) | 90 | 374 | 14 % |
| login | 250 | 0 (0 %) | 387 | 996 | 11 % |
| apoderado | 972 | 0 (0 %) | 43 | 230 | 6 % |
| propietarios | 893 | 0 (0 %) | 64 | 194 | 6 % |
| citas_del_mes | 893 | 0 (0 %) | 28 | 191 | 4 % |
| lista_de_historias | 662 | 0 (0 %) | 42 | 230 | 4 % |
| cita | 972 | 16 (2 %) | 35 | 101 | 4 % |
| navegacion | 1230 | 0 (0 %) | 12 | 118 | 3 % |
| mascota | 972 | 0 (0 %) | 24 | 74 | 3 % |
| privacidad | 1230 | 0 (0 %) | 13 | 63 | 2 % |
| empleados | 893 | 0 (0 %) | 20 | 62 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 2 | 14 | 333 | 0 % |
| 0.3 | 32 | 170 | 329 | 0 % |
| 0.8 | 62 | 288 | 261 | 0 % |
| 1.3 | 92 | 398 | 82 | 0 % |
| 1.8 | 122 | 538 | 309 | 0 % |
| 2.3 | 152 | 675 | 94 | 0 % |
| 2.8 | 182 | 735 | 121 | 0 % |
| 3.3 | 212 | 873 | 101 | 0 % |
| 3.8 | 242 | 1016 | 170 | 0 % |
| 4.3 | 250 | 957 | 647 | 0 % |
| 4.8 | 250 | 938 | 209 | 0 % |
| 5.3 | 250 | 969 | 61 | 0 % |
| 5.8 | 250 | 913 | 128 | 0 % |
| 6.3 | 250 | 873 | 152 | 0 % |
| 6.8 | 231 | 442 | 62 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
