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
| Veredicto | SATURADO (muy lento, casi sin errores) | AGUANTA |
| Errores (%) | 1.1 % | 0.1 % |
| Ritmo medio (peticiones/s) | 31.3 | 45.0 |
| Tiempo mediano (ms) | 978 | 18 |
| Tiempo p95 (ms) | 5 245 | 165 |
| Usuarios sin degradación | 165 | 250 |
| CPU del backend, media (% del límite) | 55 | 58 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 76 | 37 |
| Memoria pico (MB) | 1 362 | 1 321 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 62.5 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 129.6 | 24.3 |
| Panel sin carga: mediana de 19 llamadas (ms) | 203 | 111 |
| Panel sin carga: primera llamada (ms) | 555 | 435 |
| Panel bajo carga: mediana (ms) | 2 699 | 86 |
| Panel bajo carga: p95 (ms) | 7 649 | 299 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 192 | 243 |
| Pausas completas | 20 | 28 |
| Pausas completas por "Allocation Failure" | 12 | 15 |
| Tiempo en pausas completas (s) | 8.8 | 10.9 |
| Tiempo en GC / vida de la JVM | 5.8 % | 6.9 % |
| Heap tras GC, máximo (MB) | 968 | 972 |
| Avisos de pool agotado | 0 | 0 |

## Código anterior: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4316; fallidas: 155.
- Acciones hechas: panel 715, agenda 574, historia 506, reportes 68, recepcion 734, atencion 343, cobro 121, cancelar 152; sin trabajo disponible: 516.
- CPU del backend: 55 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 76 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 362 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **165 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.6 s.
- Correo: el servidor de pruebas recibió 734 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.2 %).
- GC: 192 pausas, 26.8 s en total (6 % del tiempo de vida de la JVM); 20 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 968 MB (tope -Xmx1g).
- Clases cargadas: 25492 en total, 115 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4501 |
| 0.3 | 81 | 82 | 16121 |
| 0.8 | 119 | 121 | 22795 |
| 1.9 | 129 | 132 | 24785 |
| 2.5 | 131 | 134 | 25061 |
| 3.0 | 133 | 136 | 25296 |
| 3.4 | 134 | 137 | 25345 |
| 4.1 | 134 | 137 | 25380 |
| 4.5 | 134 | 137 | 25402 |
| 4.9 | 135 | 138 | 25403 |
| 5.5 | 135 | 138 | 25411 |
| 6.0 | 135 | 138 | 25470 |
| 6.3 | 136 | 139 | 25472 |
| 6.8 | 136 | 139 | 25472 |
| 7.3 | 136 | 139 | 25476 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 109 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 112 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 965 | 676.8 | 701.38 | 36 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 965 | 520.2 | 539.06 | 28 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 965 | 196.2 | 203.29 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 136 | 111.5 | 819.78 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 965 | 78.9 | 81.76 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 965 | 74.0 | 76.72 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 965 | 71.9 | 74.55 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 506 | 41.6 | 82.17 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 136 | 31.8 | 233.7 | 2 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 965 | 9.4 | 9.74 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 734 | 734 | coincide |
| Mascotas | 734 | 734 | coincide |
| Citas agendadas | 720 | 720 | coincide |
| Diagnósticos | 202 | 202 | coincide |
| Recetas | 202 | 202 | coincide |
| Pagos | 121 | 121 | coincide |
| Citas completadas | 202 | 202 | coincide |
| Citas canceladas | 152 | 152 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 734 | 0 | 0.0 % | 1 179 | 4 614 |
| Registrar mascota | 734 | 0 | 0.0 % | 953 | 4 978 |
| Agendar cita | 720 | 14 | 1.9 % | 952 | 5 099 |
| Recepción (llegada) | 343 | 0 | 0.0 % | 1 669 | 4 990 |
| Iniciar atención | 202 | 141 | 41.1 % | 1 606 | 5 889 |
| Leer la consulta | 202 | 0 | 0.0 % | 293 | 4 003 |
| Registrar diagnóstico | 202 | 0 | 0.0 % | 422 | 3 841 |
| Registrar receta | 202 | 0 | 0.0 % | 282 | 4 544 |
| Actualizar mascota | 202 | 0 | 0.0 % | 403 | 4 802 |
| Cerrar la consulta | 202 | 0 | 0.0 % | 416 | 4 341 |
| Caja: crear punto de cobro | 100 | 0 | 0.0 % | 1 236 | 5 201 |
| Caja: vincular equipo | 100 | 0 | 0.0 % | 823 | 3 897 |
| Caja: abrir la caja | 100 | 0 | 0.0 % | 593 | 4 940 |
| Cobrar | 121 | 0 | 0.0 % | 1 350 | 5 387 |
| Cancelar cita | 152 | 0 | 0.0 % | 1 490 | 4 278 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 965 | 0 (0 %) | 2 699 | 7 649 | 13 % |
| navegacion | 965 | 0 (0 %) | 725 | 4 404 | 6 % |
| cita | 734 | 14 (2 %) | 952 | 5 099 | 5 % |
| avisos_de_cumplimiento | 965 | 0 (0 %) | 455 | 4 317 | 5 % |
| apoderado | 734 | 0 (0 %) | 1 179 | 4 614 | 5 % |
| citas_del_mes | 574 | 0 (0 %) | 1 809 | 5 473 | 5 % |
| mascota | 734 | 0 (0 %) | 953 | 4 978 | 5 % |
| propietarios | 574 | 0 (0 %) | 1 449 | 5 304 | 5 % |
| cumplimiento | 965 | 0 (0 %) | 432 | 4 064 | 5 % |
| notificaciones | 965 | 0 (0 %) | 408 | 4 200 | 5 % |
| empleados | 574 | 0 (0 %) | 1 336 | 5 386 | 5 % |
| lista_de_historias | 506 | 0 (0 %) | 1 678 | 5 290 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 15 | 108 | 320 | 0 % |
| 0.5 | 45 | 222 | 374 | 0 % |
| 1.0 | 75 | 314 | 635 | 0 % |
| 1.5 | 105 | 438 | 497 | 0 % |
| 2.0 | 135 | 619 | 1 155 | 0 % |
| 2.5 | 165 | 633 | 1 987 | 0 % |
| 3.0 | 195 | 630 | 3 334 | 1 % |
| 3.5 | 225 | 547 | 4 810 | 1 % |
| 4.0 | 250 | 627 | 6 664 | 1 % |
| 4.5 | 250 | 488 | 7 192 | 3 % |
| 5.0 | 250 | 581 | 5 308 | 2 % |
| 5.5 | 250 | 560 | 5 320 | 2 % |
| 6.0 | 250 | 508 | 6 387 | 2 % |
| 6.5 | 250 | 748 | 4 895 | 2 % |
| 7.0 | 140 | 65 | 216 | 0 % |

</details>

## Código nuevo: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7574; fallidas: 22.
- Acciones hechas: panel 999, agenda 837, historia 684, reportes 86, recepcion 1003, atencion 501, cobro 328, cancelar 202; sin trabajo disponible: 728.
- CPU del backend: 58 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 37 % de media y 91 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 321 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 1003 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 243 pausas, 32.1 s en total (7 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 972 MB (tope -Xmx1g).
- Clases cargadas: 25554 en total, 182 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4511 |
| 0.3 | 86 | 87 | 17110 |
| 1.3 | 127 | 130 | 24332 |
| 2.3 | 132 | 134 | 24965 |
| 2.7 | 133 | 135 | 25160 |
| 3.3 | 134 | 137 | 25340 |
| 3.9 | 134 | 137 | 25390 |
| 4.5 | 135 | 138 | 25465 |
| 4.8 | 135 | 138 | 25465 |
| 5.3 | 136 | 139 | 25506 |
| 5.6 | 136 | 139 | 25506 |
| 6.0 | 136 | 139 | 25553 |
| 6.4 | 136 | 139 | 25553 |
| 7.0 | 136 | 139 | 25553 |
| 7.5 | 136 | 139 | 25554 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 170 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 177 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 172 | 47.0 | 273.31 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1249 | 33.5 | 26.78 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1249 | 30.9 | 24.71 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 684 | 17.8 | 25.98 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 172 | 13.0 | 75.67 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1249 | 10.5 | 8.42 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2086 | 8.5 | 4.07 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2086 | 3.1 | 1.48 | 2 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 113471 | 2.9 | 0.03 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2086 | 2.7 | 1.29 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1003 | 1003 | coincide |
| Mascotas | 1003 | 1003 | coincide |
| Citas agendadas | 988 | 988 | coincide |
| Diagnósticos | 494 | 494 | coincide |
| Recetas | 494 | 494 | coincide |
| Pagos | 328 | 328 | coincide |
| Citas completadas | 494 | 494 | coincide |
| Citas canceladas | 202 | 202 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1003 | 0 | 0.0 % | 38 | 187 |
| Registrar mascota | 1003 | 0 | 0.0 % | 20 | 80 |
| Agendar cita | 988 | 15 | 1.5 % | 29 | 108 |
| Recepción (llegada) | 501 | 0 | 0.0 % | 15 | 125 |
| Iniciar atención | 494 | 7 | 1.4 % | 20 | 71 |
| Leer la consulta | 494 | 0 | 0.0 % | 12 | 48 |
| Registrar diagnóstico | 494 | 0 | 0.0 % | 12 | 46 |
| Registrar receta | 494 | 0 | 0.0 % | 12 | 43 |
| Actualizar mascota | 494 | 0 | 0.0 % | 13 | 39 |
| Cerrar la consulta | 494 | 0 | 0.0 % | 23 | 68 |
| Caja: crear punto de cobro | 195 | 0 | 0.0 % | 13 | 163 |
| Caja: vincular equipo | 195 | 0 | 0.0 % | 11 | 48 |
| Caja: abrir la caja | 195 | 0 | 0.0 % | 16 | 69 |
| Cobrar | 328 | 0 | 0.0 % | 17 | 71 |
| Cancelar cita | 202 | 0 | 0.0 % | 19 | 76 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| reporte_clinico | 86 | 0 (0 %) | 1 824 | 4 841 | 19 % |
| login | 250 | 0 (0 %) | 390 | 1 958 | 14 % |
| panel | 1249 | 0 (0 %) | 86 | 299 | 14 % |
| propietarios | 837 | 0 (0 %) | 60 | 179 | 6 % |
| apoderado | 1003 | 0 (0 %) | 38 | 187 | 5 % |
| lista_de_historias | 684 | 0 (0 %) | 38 | 194 | 4 % |
| cita | 1003 | 15 (1 %) | 29 | 108 | 4 % |
| citas_del_mes | 837 | 0 (0 %) | 26 | 158 | 4 % |
| privacidad | 1249 | 0 (0 %) | 14 | 78 | 3 % |
| mascota | 1003 | 0 (0 %) | 20 | 80 | 3 % |
| navegacion | 1249 | 0 (0 %) | 11 | 94 | 3 % |
| cumplimiento | 1249 | 0 (0 %) | 10 | 47 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 320 | 0 % |
| 0.4 | 36 | 184 | 292 | 0 % |
| 0.9 | 66 | 296 | 287 | 0 % |
| 1.4 | 96 | 440 | 88 | 0 % |
| 1.9 | 126 | 527 | 293 | 0 % |
| 2.4 | 156 | 677 | 112 | 0 % |
| 2.9 | 186 | 767 | 132 | 0 % |
| 3.4 | 216 | 881 | 89 | 0 % |
| 3.9 | 246 | 1044 | 532 | 0 % |
| 4.4 | 250 | 1007 | 94 | 0 % |
| 4.9 | 250 | 926 | 232 | 0 % |
| 5.4 | 250 | 997 | 85 | 0 % |
| 5.9 | 250 | 974 | 75 | 0 % |
| 6.4 | 250 | 969 | 88 | 0 % |
| 6.9 | 207 | 332 | 47 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
