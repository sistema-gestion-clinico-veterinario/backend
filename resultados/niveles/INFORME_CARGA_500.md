# Fase 3d: panel optimizado: 500 usuarios (1 CPU, 2 GB)

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
| Veredicto | FALLA | FALLA |
| Errores (%) | 50.5 % | 26.1 % |
| Ritmo medio (peticiones/s) | 13.3 | 23.1 |
| Tiempo mediano (ms) | 30 000 | 451 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| Usuarios sin degradación | 114 | 153 |
| CPU del backend, media (% del límite) | 26 | 46 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 21 | 20 |
| Memoria pico (MB) | 1 315 | 1 362 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.6 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 233.0 | 35.1 |
| Panel sin carga: mediana de 19 llamadas (ms) | 424 | 137 |
| Panel sin carga: primera llamada (ms) | 631 | 620 |
| Panel bajo carga: mediana (ms) | 4 540 | 370 |
| Panel bajo carga: p95 (ms) | 30 001 | 30 001 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 93 | 194 |
| Pausas completas | 17 | 32 |
| Pausas completas por "Allocation Failure" | 3 | 17 |
| Tiempo en pausas completas (s) | 4.8 | 16.3 |
| Tiempo en GC / vida de la JVM | 1.7 % | 4.8 % |
| Heap tras GC, máximo (MB) | 910 | 955 |
| Avisos de pool agotado | 4 716 | 3 272 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 1412; fallidas: 547.
- Acciones hechas: panel 387, agenda 349, historia 303, reportes 37, recepcion 590, atencion 145, cobro 58, cancelar 66; sin trabajo disponible: 422.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 4716 avisos en el registro.
- CPU del backend: 26 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 21 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 315 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.7 con unos 234 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **114 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 270 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.2 %).
- GC: 93 pausas, 12.9 s en total (2 % del tiempo de vida de la JVM); 17 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 910 MB (tope -Xmx1g).
- Clases cargadas: 25455 en total, 5 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4517 |
| 0.3 | 80 | 81 | 16056 |
| 0.4 | 82 | 83 | 16394 |
| 0.5 | 87 | 88 | 17217 |
| 0.7 | 115 | 117 | 22484 |
| 1.4 | 126 | 128 | 24218 |
| 2.0 | 130 | 132 | 24699 |
| 2.5 | 132 | 134 | 24978 |
| 2.8 | 132 | 135 | 25035 |
| 3.3 | 133 | 136 | 25289 |
| 3.6 | 133 | 136 | 25312 |
| 3.8 | 134 | 136 | 25312 |
| 4.1 | 134 | 137 | 25347 |
| 4.3 | 134 | 137 | 25347 |
| 5.5 | 135 | 138 | 25449 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 3 |
| `com.fasterxml.jackson.annotation` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 375 | 235.2 | 627.13 | 36 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 375 | 209.4 | 558.28 | 32 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 375 | 71.2 | 189.94 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 46 | 31.9 | 693.22 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 375 | 27.6 | 73.73 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 375 | 24.5 | 65.29 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 375 | 22.5 | 59.91 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 145 | 12.0 | 83.01 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 46 | 9.5 | 206.96 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 548 | 2.5 | 4.56 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 257 | 270 | la base tiene más (ver nota) |
| Mascotas | 249 | 250 | la base tiene más (ver nota) |
| Citas agendadas | 240 | 240 | coincide |
| Diagnósticos | 70 | 70 | coincide |
| Recetas | 70 | 70 | coincide |
| Pagos | 33 | 33 | coincide |
| Citas completadas | 68 | 68 | coincide |
| Citas canceladas | 38 | 40 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 257 | 333 | 56.4 % | 30 000 | 30 001 |
| Registrar mascota | 249 | 8 | 3.1 % | 105 | 10 980 |
| Agendar cita | 240 | 9 | 3.6 % | 131 | 7 212 |
| Recepción (llegada) | 88 | 57 | 39.3 % | 2 211 | 30 001 |
| Iniciar atención | 72 | 73 | 50.3 % | 3 487 | 30 001 |
| Leer la consulta | 70 | 2 | 2.8 % | 85 | 6 108 |
| Registrar diagnóstico | 70 | 2 | 2.8 % | 87 | 6 638 |
| Registrar receta | 70 | 2 | 2.8 % | 70 | 9 011 |
| Actualizar mascota | 68 | 4 | 5.6 % | 71 | 30 001 |
| Cerrar la consulta | 68 | 4 | 5.6 % | 123 | 30 000 |
| Caja: crear punto de cobro | 30 | 22 | 42.3 % | 3 705 | 30 001 |
| Caja: vincular equipo | 30 | 0 | 0.0 % | 351 | 3 893 |
| Caja: abrir la caja | 29 | 1 | 3.3 % | 106 | 18 051 |
| Cobrar | 33 | 2 | 5.7 % | 889 | 30 001 |
| Cancelar cita | 38 | 28 | 42.4 % | 1 765 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 2393 | 2191 (92 %) | 30 000 | 30 001 | 42 % |
| apoderado | 590 | 333 (56 %) | 30 000 | 30 001 | 7 % |
| privacidad | 656 | 252 (38 %) | 1 783 | 30 001 | 5 % |
| navegacion | 656 | 251 (38 %) | 1 980 | 30 001 | 5 % |
| panel | 589 | 230 (39 %) | 4 540 | 30 001 | 5 % |
| cumplimiento | 636 | 240 (38 %) | 1 206 | 30 001 | 5 % |
| notificaciones | 620 | 234 (38 %) | 1 528 | 30 001 | 5 % |
| avisos_de_cumplimiento | 606 | 233 (38 %) | 1 653 | 30 001 | 5 % |
| empleados | 358 | 190 (53 %) | 30 000 | 30 001 | 4 % |
| propietarios | 349 | 185 (53 %) | 30 000 | 30 001 | 4 % |
| citas_del_mes | 358 | 181 (51 %) | 30 000 | 30 001 | 4 % |
| lista_de_historias | 303 | 168 (55 %) | 30 000 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 66 | 459 | 0 % |
| 0.7 | 54 | 240 | 483 | 0 % |
| 1.4 | 99 | 412 | 974 | 0 % |
| 2.2 | 144 | 431 | 3 734 | 0 % |
| 2.9 | 189 | 390 | 6 503 | 1 % |
| 3.7 | 234 | 38 | 30 001 | 100 % |
| 4.4 | 279 | 113 | 30 001 | 90 % |
| 5.2 | 324 | 124 | 30 001 | 100 % |
| 5.9 | 369 | 124 | 30 001 | 100 % |
| 6.7 | 414 | 152 | 30 001 | 100 % |
| 7.4 | 459 | 155 | 30 001 | 100 % |
| 8.2 | 500 | 176 | 30 001 | 100 % |
| 8.9 | 500 | 189 | 30 001 | 100 % |
| 9.7 | 500 | 192 | 30 001 | 100 % |
| 10.4 | 500 | 188 | 30 001 | 100 % |
| 11.2 | 414 | 191 | 30 001 | 100 % |
| 11.9 | 111 | 46 | 30 001 | 100 % |

</details>

## Código nuevo: FALLA; datos CONSISTENTES

- Escrituras correctas: 4036; fallidas: 791.
- Acciones hechas: panel 794, agenda 729, historia 592, reportes 86, recepcion 1051, atencion 386, cobro 168, cancelar 144; sin trabajo disponible: 773.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3272 avisos en el registro.
- El servidor rechazó trabajo por falta de hilos: 2 avisos.
- CPU del backend: 46 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 34 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 98 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 362 MB (66 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.6 con unos 348 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **153 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 707 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.8 %).
- GC: 194 pausas, 36.9 s en total (5 % del tiempo de vida de la JVM); 32 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 955 MB (tope -Xmx1g).
- Clases cargadas: 25576 en total, 116 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.4 | 81 | 82 | 16156 |
| 0.8 | 116 | 117 | 22502 |
| 1.7 | 129 | 131 | 24521 |
| 2.5 | 132 | 134 | 24962 |
| 3.2 | 133 | 136 | 25280 |
| 3.5 | 133 | 136 | 25299 |
| 4.0 | 134 | 137 | 25364 |
| 4.4 | 134 | 137 | 25385 |
| 4.7 | 135 | 138 | 25407 |
| 5.1 | 135 | 138 | 25456 |
| 5.5 | 136 | 138 | 25456 |
| 5.7 | 136 | 139 | 25458 |
| 6.1 | 136 | 139 | 25458 |
| 6.8 | 136 | 139 | 25504 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 69 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 74 |
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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 124 | 47.1 | 379.83 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 884 | 36.8 | 41.65 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 884 | 35.3 | 39.98 | 19 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 428 | 15.7 | 36.57 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 124 | 12.5 | 101.19 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1408 | 7.9 | 5.62 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 1408 | 3.0 | 2.1 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 1426 | 2.7 | 1.88 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 71832 | 2.1 | 0.03 | 1 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 884 | 1.9 | 2.2 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 671 | 709 | la base tiene más (ver nota) |
| Mascotas | 653 | 658 | la base tiene más (ver nota) |
| Citas agendadas | 628 | 636 | la base tiene más (ver nota) |
| Diagnósticos | 212 | 212 | coincide |
| Recetas | 209 | 209 | coincide |
| Pagos | 127 | 128 | la base tiene más (ver nota) |
| Citas completadas | 208 | 208 | coincide |
| Citas canceladas | 101 | 107 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 671 | 380 | 36.2 % | 1 183 | 30 001 |
| Registrar mascota | 653 | 18 | 2.7 % | 88 | 5 024 |
| Agendar cita | 628 | 25 | 3.8 % | 115 | 5 521 |
| Recepción (llegada) | 294 | 92 | 23.8 % | 671 | 30 001 |
| Iniciar atención | 214 | 172 | 44.6 % | 629 | 30 001 |
| Leer la consulta | 213 | 1 | 0.5 % | 46 | 2 636 |
| Registrar diagnóstico | 212 | 2 | 0.9 % | 42 | 3 563 |
| Registrar receta | 209 | 5 | 2.3 % | 48 | 5 650 |
| Actualizar mascota | 208 | 6 | 2.8 % | 50 | 3 306 |
| Cerrar la consulta | 208 | 6 | 2.8 % | 88 | 3 626 |
| Caja: crear punto de cobro | 103 | 25 | 19.5 % | 399 | 30 001 |
| Caja: vincular equipo | 98 | 5 | 4.9 % | 133 | 28 663 |
| Caja: abrir la caja | 97 | 1 | 1.0 % | 73 | 4 287 |
| Cobrar | 127 | 10 | 7.3 % | 205 | 30 000 |
| Cancelar cita | 101 | 43 | 29.9 % | 957 | 30 001 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1411 | 1095 (78 %) | 30 000 | 30 001 | 24 % |
| apoderado | 1051 | 380 (36 %) | 1 183 | 30 001 | 9 % |
| privacidad | 1195 | 285 (24 %) | 176 | 30 001 | 6 % |
| navegacion | 1195 | 267 (22 %) | 312 | 30 001 | 6 % |
| cumplimiento | 1170 | 272 (23 %) | 181 | 30 001 | 6 % |
| notificaciones | 1151 | 264 (23 %) | 124 | 30 001 | 6 % |
| avisos_de_cumplimiento | 1136 | 256 (23 %) | 122 | 30 001 | 6 % |
| empleados | 747 | 239 (32 %) | 806 | 30 001 | 5 % |
| panel | 1110 | 234 (21 %) | 370 | 30 001 | 5 % |
| citas_del_mes | 747 | 220 (29 %) | 1 186 | 30 001 | 5 % |
| propietarios | 729 | 227 (31 %) | 763 | 30 001 | 5 % |
| lista_de_historias | 592 | 178 (30 %) | 937 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 399 | 0 % |
| 0.6 | 48 | 227 | 637 | 0 % |
| 1.3 | 93 | 405 | 292 | 0 % |
| 2.1 | 138 | 597 | 154 | 0 % |
| 2.8 | 183 | 714 | 216 | 0 % |
| 3.6 | 228 | 550 | 4 673 | 3 % |
| 4.3 | 273 | 984 | 2 859 | 1 % |
| 5.1 | 318 | 875 | 3 578 | 1 % |
| 5.8 | 363 | 274 | 30 001 | 85 % |
| 6.6 | 408 | 141 | 30 001 | 100 % |
| 7.3 | 453 | 182 | 30 001 | 100 % |
| 8.1 | 498 | 205 | 30 001 | 100 % |
| 8.8 | 500 | 161 | 30 001 | 100 % |
| 9.6 | 500 | 187 | 30 001 | 100 % |
| 10.3 | 500 | 182 | 30 001 | 100 % |
| 11.1 | 466 | 167 | 30 001 | 100 % |
| 11.8 | 188 | 85 | 30 001 | 100 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
