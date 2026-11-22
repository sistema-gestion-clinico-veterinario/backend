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
| Veredicto | FALLA | SATURADO (muy lento, casi sin errores) |
| Errores (%) | 30.1 % | 1.5 % |
| Ritmo medio (peticiones/s) | 20.0 | 52.0 |
| Tiempo mediano (ms) | 2 123 | 443 |
| Tiempo p95 (ms) | 30 001 | 9 078 |
| Usuarios sin degradación | 156 | 282 |
| CPU del backend, media (% del límite) | 30 | 74 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 34 | 53 |
| Memoria pico (MB) | 1 348 | 1 383 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.8 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 147.3 | 25.3 |
| Panel sin carga: mediana de 19 llamadas (ms) | 286 | 126 |
| Panel sin carga: primera llamada (ms) | 501 | 491 |
| Panel bajo carga: mediana (ms) | 3 846 | 594 |
| Panel bajo carga: p95 (ms) | 30 001 | 7 885 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 141 | 536 |
| Pausas completas | 21 | 76 |
| Pausas completas por "Allocation Failure" | 6 | 63 |
| Tiempo en pausas completas (s) | 6.6 | 46.3 |
| Tiempo en GC / vida de la JVM | 2.3 % | 13.8 % |
| Heap tras GC, máximo (MB) | 945 | 989 |
| Avisos de pool agotado | 3 080 | 0 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 3132; fallidas: 742.
- Acciones hechas: panel 682, agenda 630, historia 510, reportes 54, recepcion 950, atencion 303, cobro 116, cancelar 117; sin trabajo disponible: 645.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3080 avisos en el registro.
- CPU del backend: 30 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 3 % del tiempo.
- CPU de PostgreSQL: 34 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 348 MB (66 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.6 con unos 351 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **156 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 577 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.4 %).
- GC: 141 pausas, 17.8 s en total (2 % del tiempo de vida de la JVM); 21 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 945 MB (tope -Xmx1g).
- Clases cargadas: 25551 en total, 83 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.3 | 81 | 81 | 16111 |
| 0.4 | 87 | 88 | 17217 |
| 1.1 | 125 | 127 | 23980 |
| 1.8 | 130 | 133 | 24742 |
| 2.4 | 132 | 134 | 24980 |
| 2.7 | 132 | 135 | 25149 |
| 3.3 | 134 | 137 | 25325 |
| 3.5 | 134 | 137 | 25374 |
| 4.0 | 134 | 137 | 25389 |
| 4.5 | 135 | 138 | 25400 |
| 4.8 | 135 | 138 | 25404 |
| 5.3 | 135 | 138 | 25438 |
| 5.7 | 135 | 138 | 25467 |
| 6.2 | 136 | 139 | 25468 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 30 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 34 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.validator.internal` | 3 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 752 | 379.3 | 504.43 | 36 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 752 | 326.6 | 434.27 | 31 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 752 | 108.3 | 143.99 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 76 | 49.4 | 649.95 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 752 | 44.6 | 59.33 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 752 | 40.9 | 54.37 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 752 | 38.4 | 51.06 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 337 | 19.5 | 57.88 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 76 | 14.2 | 187.0 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1162 | 3.8 | 3.31 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 554 | 577 | la base tiene más (ver nota) |
| Mascotas | 541 | 545 | la base tiene más (ver nota) |
| Citas agendadas | 518 | 520 | la base tiene más (ver nota) |
| Diagnósticos | 146 | 146 | coincide |
| Recetas | 142 | 142 | coincide |
| Pagos | 95 | 97 | la base tiene más (ver nota) |
| Citas completadas | 139 | 140 | la base tiene más (ver nota) |
| Citas canceladas | 87 | 92 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 554 | 396 | 41.7 % | 4 760 | 30 001 |
| Registrar mascota | 541 | 13 | 2.3 % | 454 | 10 461 |
| Agendar cita | 518 | 23 | 4.3 % | 499 | 9 322 |
| Recepción (llegada) | 224 | 79 | 26.1 % | 2 254 | 30 001 |
| Iniciar atención | 148 | 155 | 51.2 % | 2 405 | 30 001 |
| Leer la consulta | 146 | 2 | 1.4 % | 76 | 9 089 |
| Registrar diagnóstico | 146 | 2 | 1.4 % | 101 | 8 637 |
| Registrar receta | 142 | 6 | 4.1 % | 81 | 10 344 |
| Actualizar mascota | 142 | 6 | 4.1 % | 170 | 11 856 |
| Cerrar la consulta | 139 | 9 | 6.1 % | 157 | 30 000 |
| Caja: crear punto de cobro | 85 | 9 | 9.6 % | 925 | 30 000 |
| Caja: vincular equipo | 83 | 2 | 2.4 % | 415 | 8 448 |
| Caja: abrir la caja | 82 | 1 | 1.2 % | 382 | 10 904 |
| Cobrar | 95 | 9 | 8.7 % | 974 | 30 000 |
| Cancelar cita | 87 | 30 | 25.6 % | 2 230 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1388 | 1067 (77 %) | 30 000 | 30 001 | 22 % |
| apoderado | 950 | 396 (42 %) | 4 760 | 30 001 | 9 % |
| panel | 1003 | 272 (27 %) | 3 846 | 30 001 | 7 % |
| navegacion | 1090 | 275 (25 %) | 1 802 | 30 001 | 7 % |
| privacidad | 1090 | 294 (27 %) | 1 201 | 30 001 | 7 % |
| cumplimiento | 1063 | 283 (27 %) | 1 306 | 30 001 | 6 % |
| avisos_de_cumplimiento | 1024 | 273 (27 %) | 1 409 | 30 001 | 6 % |
| notificaciones | 1041 | 274 (26 %) | 1 286 | 30 001 | 6 % |
| empleados | 643 | 240 (37 %) | 4 259 | 30 001 | 5 % |
| propietarios | 630 | 233 (37 %) | 3 735 | 30 001 | 5 % |
| citas_del_mes | 643 | 225 (35 %) | 3 619 | 30 001 | 5 % |
| lista_de_historias | 510 | 185 (36 %) | 3 384 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 471 | 0 % |
| 0.6 | 51 | 253 | 346 | 0 % |
| 1.4 | 96 | 428 | 307 | 0 % |
| 2.1 | 141 | 611 | 310 | 0 % |
| 2.9 | 186 | 520 | 3 812 | 1 % |
| 3.6 | 231 | 656 | 3 987 | 0 % |
| 4.4 | 276 | 522 | 9 347 | 2 % |
| 5.1 | 321 | 480 | 14 412 | 2 % |
| 5.9 | 366 | 248 | 30 001 | 83 % |
| 6.6 | 411 | 155 | 30 001 | 100 % |
| 7.4 | 456 | 187 | 30 001 | 100 % |
| 8.1 | 500 | 188 | 30 001 | 100 % |
| 8.9 | 500 | 194 | 30 001 | 100 % |
| 9.6 | 500 | 186 | 30 001 | 100 % |
| 10.4 | 500 | 204 | 30 001 | 100 % |
| 11.1 | 444 | 198 | 30 001 | 100 % |
| 11.9 | 175 | 72 | 30 001 | 100 % |

</details>

## Código nuevo: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 11467; fallidas: 528.
- Acciones hechas: panel 2000, agenda 1544, historia 1288, reportes 204, recepcion 1978, atencion 990, cobro 407, cancelar 428; sin trabajo disponible: 1403.
- El servidor rechazó trabajo por falta de hilos: 54 avisos.
- CPU del backend: 74 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 30 % del tiempo.
- CPU de PostgreSQL: 53 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 383 MB (68 % de 2048 MB).
- Los fallos fueron: 5 % sin respuesta (tiempo agotado), 0 % errores 5xx del servidor, 94 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 9.7 con unos 500 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **282 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 29.4 s.
- Correo: el servidor de pruebas recibió 1924 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 135 MB (-0.8 %).
- GC: 536 pausas, 99.0 s en total (14 % del tiempo de vida de la JVM); 76 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25638 en total, 183 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4508 |
| 1.8 | 129 | 132 | 24461 |
| 3.2 | 133 | 136 | 25296 |
| 4.3 | 135 | 138 | 25438 |
| 5.3 | 136 | 139 | 25526 |
| 5.9 | 136 | 139 | 25556 |
| 6.6 | 136 | 139 | 25573 |
| 7.2 | 137 | 140 | 25574 |
| 7.8 | 137 | 140 | 25576 |
| 8.4 | 135 | 140 | 25576 |
| 9.0 | 135 | 140 | 25616 |
| 9.5 | 135 | 140 | 25616 |
| 10.1 | 135 | 140 | 25616 |
| 10.7 | 135 | 140 | 25638 |
| 11.5 | 135 | 140 | 25638 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 141 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 146 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.dao.support` | 2 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.postgresql.core.EncodingPredictor` | 1 |
| `org.postgresql.util.ServerErrorMessage` | 1 |
| `org.postgresql.util.GT` | 1 |
| `java.text.MessageFormat` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 2500 | 421.5 | 168.62 | 35 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 408 | 213.3 | 522.72 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 2500 | 147.7 | 59.1 | 12 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 2500 | 143.8 | 57.53 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1288 | 62.1 | 48.2 | 5 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 408 | 56.2 | 137.71 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 4044 | 44.5 | 10.99 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 4044 | 13.1 | 3.24 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 204 | 12.2 | 60.01 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 210282 | 10.2 | 0.05 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1976 | 1978 | la base tiene más (ver nota) |
| Mascotas | 1975 | 1976 | la base tiene más (ver nota) |
| Citas agendadas | 1945 | 1946 | la base tiene más (ver nota) |
| Diagnósticos | 499 | 499 | coincide |
| Recetas | 498 | 499 | la base tiene más (ver nota) |
| Pagos | 407 | 407 | coincide |
| Citas completadas | 499 | 499 | coincide |
| Citas canceladas | 426 | 428 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1976 | 2 | 0.1 % | 575 | 10 502 |
| Registrar mascota | 1975 | 1 | 0.1 % | 431 | 9 393 |
| Agendar cita | 1945 | 30 | 1.5 % | 530 | 9 248 |
| Recepción (llegada) | 989 | 1 | 0.1 % | 787 | 11 017 |
| Iniciar atención | 499 | 491 | 49.6 % | 731 | 10 983 |
| Leer la consulta | 499 | 0 | 0.0 % | 41 | 5 342 |
| Registrar diagnóstico | 499 | 0 | 0.0 % | 41 | 7 534 |
| Registrar receta | 498 | 1 | 0.2 % | 44 | 7 054 |
| Actualizar mascota | 499 | 0 | 0.0 % | 40 | 5 582 |
| Cerrar la consulta | 499 | 0 | 0.0 % | 69 | 6 453 |
| Caja: crear punto de cobro | 252 | 0 | 0.0 % | 70 | 7 834 |
| Caja: vincular equipo | 252 | 0 | 0.0 % | 41 | 7 512 |
| Caja: abrir la caja | 252 | 0 | 0.0 % | 57 | 9 839 |
| Cobrar | 407 | 0 | 0.0 % | 284 | 10 457 |
| Cancelar cita | 426 | 2 | 0.5 % | 644 | 9 320 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| navegacion | 2500 | 3 (0 %) | 397 | 8 741 | 7 % |
| apoderado | 1978 | 2 (0 %) | 575 | 10 502 | 6 % |
| panel | 2500 | 2 (0 %) | 594 | 7 885 | 6 % |
| privacidad | 2500 | 1 (0 %) | 300 | 8 155 | 6 % |
| cumplimiento | 2500 | 0 (0 %) | 285 | 7 978 | 6 % |
| cita | 1975 | 30 (2 %) | 530 | 9 248 | 6 % |
| mascota | 1976 | 1 (0 %) | 431 | 9 393 | 6 % |
| notificaciones | 2500 | 0 (0 %) | 305 | 7 837 | 6 % |
| avisos_de_cumplimiento | 2500 | 1 (0 %) | 299 | 7 546 | 5 % |
| citas_del_mes | 1544 | 5 (0 %) | 701 | 10 275 | 5 % |
| propietarios | 1544 | 1 (0 %) | 768 | 9 456 | 5 % |
| empleados | 1544 | 2 (0 %) | 585 | 10 135 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 93 | 322 | 0 % |
| 0.7 | 57 | 258 | 292 | 0 % |
| 1.5 | 102 | 464 | 87 | 0 % |
| 2.2 | 147 | 617 | 188 | 0 % |
| 3.0 | 192 | 751 | 188 | 0 % |
| 3.7 | 237 | 1014 | 113 | 0 % |
| 4.5 | 282 | 950 | 435 | 0 % |
| 5.2 | 327 | 854 | 2 579 | 0 % |
| 6.0 | 372 | 646 | 10 682 | 2 % |
| 6.7 | 417 | 1387 | 2 405 | 2 % |
| 7.5 | 462 | 1208 | 14 573 | 3 % |
| 8.2 | 500 | 991 | 8 251 | 1 % |
| 9.0 | 500 | 1561 | 1 578 | 3 % |
| 9.7 | 500 | 714 | 29 437 | 7 % |
| 10.5 | 500 | 587 | 16 416 | 4 % |
| 11.2 | 391 | 430 | 7 492 | 2 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
