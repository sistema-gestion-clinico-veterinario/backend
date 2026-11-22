# Fase 3d: panel optimizado: 500 usuarios (1 CPU, 2 GB)

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
| Veredicto | FALLA | FALLA |
| Errores (%) | 29.9 % | 10.1 % |
| Ritmo medio (peticiones/s) | 20.2 | 40.0 |
| Tiempo mediano (ms) | 2 508 | 89 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| Usuarios sin degradación | 144 | 329 |
| CPU del backend, media (% del límite) | 32 | 58 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 37 | 33 |
| Memoria pico (MB) | 1 352 | 1 390 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.7 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 159.6 | 24.7 |
| Panel sin carga: mediana de 19 llamadas (ms) | 302 | 119 |
| Panel sin carga: primera llamada (ms) | 543 | 505 |
| Panel bajo carga: mediana (ms) | 4 058 | 195 |
| Panel bajo carga: p95 (ms) | 30 001 | 30 001 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 140 | 353 |
| Pausas completas | 20 | 43 |
| Pausas completas por "Allocation Failure" | 5 | 29 |
| Tiempo en pausas completas (s) | 6.4 | 21.9 |
| Tiempo en GC / vida de la JVM | 2.4 % | 7.5 % |
| Heap tras GC, máximo (MB) | 947 | 987 |
| Avisos de pool agotado | 3 141 | 904 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 3214; fallidas: 761.
- Acciones hechas: panel 694, agenda 591, historia 532, reportes 60, recepcion 963, atencion 342, cobro 106, cancelar 120; sin trabajo disponible: 677.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3141 avisos en el registro.
- CPU del backend: 32 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 37 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 352 MB (66 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.9 con unos 369 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **144 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 633 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.7 %).
- GC: 140 pausas, 17.9 s en total (2 % del tiempo de vida de la JVM); 20 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 947 MB (tope -Xmx1g).
- Clases cargadas: 25543 en total, 118 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4513 |
| 0.3 | 81 | 81 | 16108 |
| 0.4 | 87 | 88 | 17214 |
| 1.0 | 122 | 124 | 23468 |
| 1.7 | 129 | 132 | 24543 |
| 2.3 | 131 | 134 | 24948 |
| 2.8 | 133 | 135 | 25150 |
| 3.3 | 133 | 136 | 25307 |
| 3.7 | 134 | 137 | 25375 |
| 4.1 | 134 | 137 | 25393 |
| 4.4 | 135 | 138 | 25394 |
| 4.7 | 135 | 138 | 25397 |
| 5.2 | 135 | 138 | 25411 |
| 5.9 | 136 | 138 | 25425 |
| 6.3 | 136 | 139 | 25469 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 71 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 78 |
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
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 770 | 417.8 | 542.55 | 37 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 770 | 342.7 | 445.11 | 30 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 770 | 121.7 | 158.06 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 76 | 54.0 | 710.7 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 770 | 50.8 | 65.93 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 770 | 45.9 | 59.64 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 770 | 39.5 | 51.34 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 357 | 22.8 | 63.75 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 76 | 14.1 | 185.89 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1127 | 3.7 | 3.28 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 611 | 633 | la base tiene más (ver nota) |
| Mascotas | 591 | 598 | la base tiene más (ver nota) |
| Citas agendadas | 571 | 574 | la base tiene más (ver nota) |
| Diagnósticos | 134 | 134 | coincide |
| Recetas | 133 | 133 | coincide |
| Pagos | 83 | 84 | la base tiene más (ver nota) |
| Citas completadas | 128 | 128 | coincide |
| Citas canceladas | 94 | 96 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 611 | 352 | 36.6 % | 4 236 | 30 001 |
| Registrar mascota | 591 | 20 | 3.3 % | 811 | 9 781 |
| Agendar cita | 571 | 20 | 3.4 % | 813 | 7 734 |
| Recepción (llegada) | 240 | 102 | 29.8 % | 3 405 | 30 001 |
| Iniciar atención | 134 | 208 | 60.8 % | 3 887 | 30 001 |
| Leer la consulta | 134 | 0 | 0.0 % | 230 | 5 691 |
| Registrar diagnóstico | 134 | 0 | 0.0 % | 281 | 5 454 |
| Registrar receta | 133 | 1 | 0.7 % | 280 | 7 664 |
| Actualizar mascota | 131 | 3 | 2.2 % | 261 | 6 280 |
| Cerrar la consulta | 128 | 6 | 4.5 % | 481 | 10 694 |
| Caja: crear punto de cobro | 78 | 16 | 17.0 % | 1 233 | 30 001 |
| Caja: vincular equipo | 77 | 1 | 1.3 % | 597 | 7 331 |
| Caja: abrir la caja | 75 | 2 | 2.6 % | 630 | 14 032 |
| Cobrar | 83 | 4 | 4.6 % | 1 254 | 8 762 |
| Cancelar cita | 94 | 26 | 21.7 % | 2 768 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1334 | 1006 (75 %) | 30 000 | 30 001 | 20 % |
| apoderado | 963 | 352 (37 %) | 4 236 | 30 001 | 8 % |
| panel | 1022 | 262 (26 %) | 4 058 | 30 001 | 7 % |
| privacidad | 1106 | 296 (27 %) | 1 386 | 30 001 | 7 % |
| cumplimiento | 1085 | 290 (27 %) | 1 700 | 30 001 | 6 % |
| navegacion | 1106 | 276 (25 %) | 1 747 | 30 001 | 6 % |
| notificaciones | 1064 | 280 (26 %) | 1 656 | 30 001 | 6 % |
| avisos_de_cumplimiento | 1051 | 277 (26 %) | 1 628 | 30 001 | 6 % |
| empleados | 613 | 261 (43 %) | 5 338 | 30 001 | 6 % |
| citas_del_mes | 613 | 236 (38 %) | 5 027 | 30 001 | 5 % |
| propietarios | 591 | 245 (41 %) | 4 852 | 30 001 | 5 % |
| lista_de_historias | 532 | 187 (35 %) | 3 574 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 66 | 437 | 0 % |
| 0.7 | 54 | 277 | 526 | 0 % |
| 1.4 | 99 | 459 | 460 | 0 % |
| 2.2 | 144 | 619 | 535 | 0 % |
| 2.9 | 189 | 591 | 2 542 | 1 % |
| 3.7 | 234 | 593 | 6 761 | 1 % |
| 4.4 | 279 | 547 | 6 799 | 2 % |
| 5.2 | 324 | 586 | 10 832 | 3 % |
| 5.9 | 369 | 273 | 30 001 | 79 % |
| 6.7 | 414 | 144 | 30 001 | 100 % |
| 7.4 | 459 | 172 | 30 001 | 100 % |
| 8.2 | 500 | 188 | 30 001 | 100 % |
| 8.9 | 500 | 179 | 30 001 | 100 % |
| 9.7 | 500 | 193 | 30 001 | 100 % |
| 10.4 | 500 | 186 | 30 001 | 100 % |
| 11.2 | 433 | 179 | 30 001 | 100 % |
| 11.9 | 181 | 77 | 30 001 | 100 % |

</details>

## Código nuevo: FALLA; datos CONSISTENTES

- Escrituras correctas: 8937; fallidas: 738.
- Acciones hechas: panel 1479, agenda 1276, historia 1075, reportes 128, recepcion 1643, atencion 765, cobro 385, cancelar 270; sin trabajo disponible: 1208.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 904 avisos en el registro.
- El servidor rechazó trabajo por falta de hilos: 5 avisos.
- CPU del backend: 58 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 25 % del tiempo.
- CPU de PostgreSQL: 33 % de media y 101 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 390 MB (68 % de 2048 MB).
- Los fallos fueron: 93 % sin respuesta (tiempo agotado), 7 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 8.2 con unos 500 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **329 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 1538 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.8 %).
- GC: 353 pausas, 56.7 s en total (7 % del tiempo de vida de la JVM); 43 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 987 MB (tope -Xmx1g).
- Clases cargadas: 25625 en total, 125 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.5 | 102 | 104 | 20013 |
| 2.2 | 131 | 134 | 24909 |
| 3.2 | 134 | 136 | 25333 |
| 4.1 | 134 | 137 | 25392 |
| 4.9 | 135 | 138 | 25458 |
| 5.7 | 136 | 139 | 25528 |
| 6.2 | 136 | 139 | 25531 |
| 6.7 | 136 | 139 | 25532 |
| 7.1 | 136 | 139 | 25549 |
| 7.5 | 136 | 140 | 25549 |
| 8.0 | 137 | 140 | 25550 |
| 8.5 | 137 | 140 | 25557 |
| 9.3 | 137 | 140 | 25596 |
| 12.6 | 137 | 140 | 25625 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 74 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 80 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 244 | 94.7 | 388.04 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1851 | 75.6 | 40.85 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1851 | 69.0 | 37.3 | 17 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1983 | 32.2 | 16.25 | 8 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1029 | 30.6 | 29.73 | 7 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 244 | 23.1 | 94.68 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 3030 | 18.4 | 6.09 | 4 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 160328 | 6.0 | 0.04 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 3030 | 5.7 | 1.89 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 118 | 4.6 | 39.3 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1410 | 1554 | la base tiene más (ver nota) |
| Mascotas | 1365 | 1400 | la base tiene más (ver nota) |
| Citas agendadas | 1328 | 1339 | la base tiene más (ver nota) |
| Diagnósticos | 476 | 477 | la base tiene más (ver nota) |
| Recetas | 474 | 475 | la base tiene más (ver nota) |
| Pagos | 357 | 361 | la base tiene más (ver nota) |
| Citas completadas | 469 | 476 | la base tiene más (ver nota) |
| Citas canceladas | 251 | 263 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1410 | 233 | 14.2 % | 212 | 30 001 |
| Registrar mascota | 1365 | 45 | 3.2 % | 50 | 9 162 |
| Agendar cita | 1328 | 37 | 2.7 % | 64 | 5 919 |
| Recepción (llegada) | 696 | 69 | 9.0 % | 180 | 30 000 |
| Iniciar atención | 478 | 287 | 37.5 % | 137 | 30 001 |
| Leer la consulta | 477 | 1 | 0.2 % | 23 | 2 273 |
| Registrar diagnóstico | 476 | 2 | 0.4 % | 24 | 2 483 |
| Registrar receta | 474 | 4 | 0.8 % | 25 | 2 390 |
| Actualizar mascota | 474 | 4 | 0.8 % | 24 | 2 501 |
| Cerrar la consulta | 469 | 9 | 1.9 % | 42 | 2 467 |
| Caja: crear punto de cobro | 232 | 12 | 4.9 % | 27 | 28 481 |
| Caja: vincular equipo | 225 | 7 | 3.0 % | 22 | 6 929 |
| Caja: abrir la caja | 225 | 0 | 0.0 % | 28 | 2 326 |
| Cobrar | 357 | 9 | 2.5 % | 54 | 8 087 |
| Cancelar cita | 251 | 19 | 7.0 % | 166 | 30 000 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| apoderado | 1643 | 233 (14 %) | 212 | 30 001 | 8 % |
| navegacion | 2081 | 211 (10 %) | 43 | 30 001 | 8 % |
| privacidad | 2081 | 234 (11 %) | 58 | 30 001 | 8 % |
| cumplimiento | 2048 | 234 (11 %) | 34 | 30 001 | 7 % |
| empleados | 1300 | 230 (18 %) | 221 | 30 001 | 7 % |
| citas_del_mes | 1300 | 197 (15 %) | 282 | 30 001 | 7 % |
| notificaciones | 2019 | 219 (11 %) | 24 | 30 001 | 7 % |
| propietarios | 1276 | 217 (17 %) | 314 | 30 001 | 7 % |
| avisos_de_cumplimiento | 1991 | 191 (10 %) | 26 | 30 001 | 6 % |
| panel | 1955 | 171 (9 %) | 195 | 30 001 | 6 % |
| lista_de_historias | 1075 | 136 (13 %) | 220 | 30 001 | 5 % |
| login | 576 | 100 (17 %) | 857 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 101 | 332 | 0 % |
| 0.8 | 59 | 279 | 313 | 1 % |
| 1.5 | 104 | 457 | 129 | 0 % |
| 2.2 | 149 | 631 | 105 | 0 % |
| 3.0 | 194 | 811 | 96 | 0 % |
| 3.8 | 239 | 958 | 368 | 1 % |
| 4.5 | 284 | 1269 | 150 | 0 % |
| 5.2 | 329 | 1237 | 560 | 0 % |
| 6.0 | 374 | 1071 | 5 401 | 2 % |
| 6.8 | 419 | 574 | 12 923 | 2 % |
| 7.5 | 464 | 1111 | 11 161 | 2 % |
| 8.2 | 500 | 62 | 30 001 | 100 % |
| 9.0 | 500 | 319 | 30 001 | 85 % |
| 9.8 | 500 | 186 | 30 001 | 100 % |
| 10.5 | 500 | 212 | 30 001 | 100 % |
| 11.2 | 435 | 188 | 30 001 | 100 % |
| 12.0 | 160 | 57 | 30 001 | 100 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
