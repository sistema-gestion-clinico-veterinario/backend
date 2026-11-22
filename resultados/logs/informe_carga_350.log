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
| Veredicto | FALLA | FALLA |
| Errores (%) | 31.5 % | 15.0 % |
| Ritmo medio (peticiones/s) | 14.2 | 24.8 |
| Tiempo mediano (ms) | 2 813 | 131 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| Usuarios sin degradación | 96 | 241 |
| CPU del backend, media (% del límite) | 34 | 50 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 34 | 25 |
| Memoria pico (MB) | 1 371 | 1 325 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.8 | 52.8 |
| Tiempo SQL por carga del panel (ms, sin carga) | 218.4 | 36.0 |
| Panel sin carga: mediana de 19 llamadas (ms) | 408 | 153 |
| Panel sin carga: primera llamada (ms) | 641 | 593 |
| Panel bajo carga: mediana (ms) | 4 259 | 203 |
| Panel bajo carga: p95 (ms) | 30 001 | 30 001 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 94 | 180 |
| Pausas completas | 12 | 27 |
| Pausas completas por "Allocation Failure" | 3 | 14 |
| Tiempo en pausas completas (s) | 4.1 | 12.0 |
| Tiempo en GC / vida de la JVM | 2.0 % | 5.1 % |
| Heap tras GC, máximo (MB) | 950 | 940 |
| Avisos de pool agotado | 2 820 | 1 773 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 1612; fallidas: 504.
- Acciones hechas: panel 346, agenda 313, historia 273, reportes 39, recepcion 599, atencion 166, cobro 52, cancelar 58; sin trabajo disponible: 432.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2820 avisos en el registro.
- CPU del backend: 34 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 34 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 371 MB (67 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.1 con unos 261 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **96 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 344 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 135 MB (+0.9 %).
- GC: 94 pausas, 12.9 s en total (2 % del tiempo de vida de la JVM); 12 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 950 MB (tope -Xmx1g).
- Clases cargadas: 25456 en total, 83 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.3 | 80 | 81 | 16053 |
| 0.4 | 81 | 82 | 16127 |
| 0.5 | 87 | 88 | 17212 |
| 1.0 | 119 | 121 | 22791 |
| 1.7 | 128 | 130 | 24507 |
| 2.2 | 129 | 132 | 24772 |
| 2.6 | 130 | 133 | 24923 |
| 2.8 | 131 | 134 | 25055 |
| 3.2 | 133 | 136 | 25263 |
| 3.6 | 133 | 136 | 25303 |
| 3.9 | 133 | 136 | 25307 |
| 4.2 | 134 | 136 | 25308 |
| 4.5 | 134 | 137 | 25361 |
| 5.0 | 134 | 137 | 25373 |
| 9.1 | 135 | 138 | 25446 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 31 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 35 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.validator.internal` | 3 |
| `org.springframework.security.core` | 2 |
| `org.springframework.dao.support` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 442 | 267.0 | 603.99 | 34 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 442 | 264.7 | 598.91 | 34 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 442 | 81.4 | 184.23 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 46 | 31.9 | 693.49 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 442 | 31.6 | 71.41 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 442 | 31.2 | 70.66 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 442 | 28.5 | 64.52 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 180 | 13.7 | 76.25 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 46 | 12.0 | 261.18 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 637 | 2.6 | 4.16 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 315 | 344 | la base tiene más (ver nota) |
| Mascotas | 291 | 293 | la base tiene más (ver nota) |
| Citas agendadas | 276 | 278 | la base tiene más (ver nota) |
| Diagnósticos | 70 | 71 | la base tiene más (ver nota) |
| Recetas | 68 | 68 | coincide |
| Pagos | 38 | 38 | coincide |
| Citas completadas | 67 | 67 | coincide |
| Citas canceladas | 45 | 47 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 315 | 284 | 47.4 % | 10 706 | 30 001 |
| Registrar mascota | 291 | 24 | 7.6 % | 638 | 30 001 |
| Agendar cita | 276 | 15 | 5.2 % | 619 | 11 468 |
| Recepción (llegada) | 119 | 47 | 28.3 % | 2 378 | 30 001 |
| Iniciar atención | 71 | 95 | 57.2 % | 2 684 | 30 001 |
| Leer la consulta | 70 | 1 | 1.4 % | 326 | 8 674 |
| Registrar diagnóstico | 70 | 1 | 1.4 % | 383 | 7 013 |
| Registrar receta | 68 | 3 | 4.2 % | 447 | 12 308 |
| Actualizar mascota | 68 | 3 | 4.2 % | 474 | 14 160 |
| Cerrar la consulta | 67 | 4 | 5.6 % | 292 | 30 000 |
| Caja: crear punto de cobro | 40 | 9 | 18.4 % | 2 480 | 30 001 |
| Caja: vincular equipo | 38 | 2 | 5.0 % | 1 500 | 30 001 |
| Caja: abrir la caja | 36 | 2 | 5.3 % | 2 413 | 30 001 |
| Cobrar | 38 | 1 | 2.6 % | 1 185 | 12 544 |
| Cancelar cita | 45 | 13 | 22.4 % | 3 824 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 813 | 574 (71 %) | 30 000 | 30 001 | 20 % |
| apoderado | 599 | 284 (47 %) | 10 706 | 30 001 | 10 % |
| panel | 585 | 153 (26 %) | 4 259 | 30 001 | 7 % |
| privacidad | 648 | 169 (26 %) | 1 361 | 30 001 | 6 % |
| cumplimiento | 636 | 168 (26 %) | 1 522 | 30 001 | 6 % |
| navegacion | 648 | 161 (25 %) | 1 655 | 30 001 | 6 % |
| notificaciones | 619 | 164 (26 %) | 1 381 | 30 001 | 6 % |
| avisos_de_cumplimiento | 604 | 158 (26 %) | 1 094 | 30 001 | 6 % |
| empleados | 325 | 134 (41 %) | 4 096 | 30 001 | 5 % |
| propietarios | 313 | 130 (42 %) | 5 721 | 30 001 | 5 % |
| citas_del_mes | 325 | 126 (39 %) | 5 185 | 30 001 | 5 % |
| lista_de_historias | 273 | 113 (41 %) | 5 757 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 41 | 437 | 0 % |
| 0.4 | 36 | 193 | 1 795 | 1 % |
| 0.9 | 66 | 299 | 533 | 0 % |
| 1.4 | 96 | 418 | 981 | 0 % |
| 1.9 | 126 | 444 | 3 084 | 1 % |
| 2.4 | 156 | 446 | 5 553 | 1 % |
| 2.9 | 186 | 400 | 7 971 | 1 % |
| 3.4 | 216 | 381 | 11 929 | 1 % |
| 3.9 | 246 | 312 | 14 129 | 1 % |
| 4.4 | 276 | 93 | 30 001 | 100 % |
| 4.9 | 306 | 84 | 30 001 | 100 % |
| 5.4 | 336 | 121 | 30 001 | 100 % |
| 5.9 | 350 | 138 | 30 001 | 100 % |
| 6.4 | 350 | 130 | 30 001 | 100 % |
| 6.9 | 350 | 152 | 30 001 | 100 % |
| 7.4 | 350 | 131 | 30 001 | 100 % |
| 7.9 | 350 | 131 | 30 001 | 100 % |
| 8.4 | 349 | 146 | 30 001 | 100 % |
| 8.9 | 239 | 101 | 30 001 | 100 % |
| 9.4 | 122 | 47 | 30 001 | 100 % |

</details>

## Código nuevo: FALLA; datos CONSISTENTES

- Escrituras correctas: 4040; fallidas: 491.
- Acciones hechas: panel 659, agenda 657, historia 516, reportes 70, recepcion 873, atencion 332, cobro 154, cancelar 140; sin trabajo disponible: 630.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 1773 avisos en el registro.
- CPU del backend: 50 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 33 % del tiempo.
- CPU de PostgreSQL: 25 % de media y 95 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 325 MB (65 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.5 con unos 346 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **241 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 710 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 137 MB (+1.3 %).
- GC: 180 pausas, 31.3 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 940 MB (tope -Xmx1g).
- Clases cargadas: 25569 en total, 115 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 19 | 4577 |
| 0.4 | 81 | 82 | 16122 |
| 0.6 | 98 | 99 | 18780 |
| 1.7 | 128 | 131 | 24429 |
| 2.5 | 131 | 134 | 24934 |
| 2.9 | 132 | 135 | 25122 |
| 3.5 | 133 | 136 | 25311 |
| 4.0 | 134 | 137 | 25363 |
| 4.4 | 134 | 137 | 25394 |
| 4.7 | 135 | 138 | 25436 |
| 5.0 | 135 | 138 | 25453 |
| 5.3 | 135 | 138 | 25454 |
| 5.6 | 136 | 139 | 25454 |
| 5.9 | 136 | 139 | 25455 |
| 6.6 | 136 | 139 | 25498 |
| 10.2 | 137 | 140 | 25569 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 70 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 76 |
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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 114 | 47.2 | 414.47 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 877 | 33.5 | 38.16 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 877 | 30.0 | 34.25 | 16 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 430 | 15.8 | 36.7 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 114 | 13.2 | 116.19 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1393 | 7.7 | 5.55 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 1393 | 3.0 | 2.13 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 1443 | 2.8 | 1.93 | 2 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 71083 | 2.1 | 0.03 | 1 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 877 | 2.0 | 2.24 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 670 | 711 | la base tiene más (ver nota) |
| Mascotas | 638 | 645 | la base tiene más (ver nota) |
| Citas agendadas | 616 | 619 | la base tiene más (ver nota) |
| Diagnósticos | 212 | 212 | coincide |
| Recetas | 209 | 209 | coincide |
| Pagos | 126 | 128 | la base tiene más (ver nota) |
| Citas completadas | 208 | 208 | coincide |
| Citas canceladas | 120 | 121 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 670 | 203 | 23.3 % | 356 | 30 001 |
| Registrar mascota | 638 | 32 | 4.8 % | 66 | 9 072 |
| Agendar cita | 616 | 22 | 3.4 % | 88 | 3 571 |
| Recepción (llegada) | 284 | 48 | 14.5 % | 205 | 30 001 |
| Iniciar atención | 214 | 118 | 35.5 % | 196 | 30 001 |
| Leer la consulta | 212 | 2 | 0.9 % | 35 | 2 473 |
| Registrar diagnóstico | 212 | 2 | 0.9 % | 38 | 3 033 |
| Registrar receta | 209 | 5 | 2.3 % | 43 | 2 596 |
| Actualizar mascota | 209 | 5 | 2.3 % | 41 | 3 369 |
| Cerrar la consulta | 208 | 6 | 2.8 % | 74 | 4 336 |
| Caja: crear punto de cobro | 109 | 15 | 12.1 % | 157 | 30 001 |
| Caja: vincular equipo | 107 | 2 | 1.8 % | 66 | 4 684 |
| Caja: abrir la caja | 106 | 1 | 0.9 % | 77 | 2 438 |
| Cobrar | 126 | 10 | 7.4 % | 121 | 30 000 |
| Cancelar cita | 120 | 20 | 14.3 % | 373 | 30 001 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| apoderado | 873 | 203 (23 %) | 356 | 30 001 | 9 % |
| login | 492 | 185 (38 %) | 3 442 | 30 001 | 8 % |
| empleados | 674 | 172 (26 %) | 332 | 30 001 | 8 % |
| propietarios | 657 | 174 (26 %) | 416 | 30 001 | 8 % |
| privacidad | 1057 | 171 (16 %) | 33 | 30 001 | 8 % |
| navegacion | 1057 | 154 (15 %) | 50 | 30 001 | 7 % |
| citas_del_mes | 674 | 148 (22 %) | 435 | 30 001 | 7 % |
| cumplimiento | 1027 | 150 (15 %) | 37 | 30 001 | 7 % |
| notificaciones | 1006 | 136 (14 %) | 29 | 30 001 | 6 % |
| avisos_de_cumplimiento | 980 | 116 (12 %) | 30 | 30 001 | 5 % |
| lista_de_historias | 516 | 109 (21 %) | 380 | 30 001 | 5 % |
| panel | 966 | 107 (11 %) | 203 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 6 | 427 | 0 % |
| 0.3 | 31 | 159 | 438 | 0 % |
| 0.8 | 61 | 262 | 325 | 0 % |
| 1.3 | 91 | 406 | 202 | 0 % |
| 1.8 | 121 | 539 | 420 | 0 % |
| 2.3 | 151 | 622 | 209 | 0 % |
| 2.8 | 181 | 725 | 167 | 0 % |
| 3.3 | 211 | 883 | 943 | 0 % |
| 3.8 | 241 | 904 | 1 090 | 0 % |
| 4.3 | 271 | 751 | 5 566 | 2 % |
| 4.8 | 301 | 1007 | 3 218 | 1 % |
| 5.3 | 331 | 129 | 10 114 | 2 % |
| 5.8 | 350 | 141 | 30 001 | 74 % |
| 6.3 | 350 | 153 | 30 001 | 96 % |
| 6.8 | 350 | 141 | 30 001 | 100 % |
| 7.3 | 350 | 130 | 30 001 | 100 % |
| 7.8 | 350 | 116 | 30 001 | 100 % |
| 8.3 | 350 | 146 | 30 001 | 100 % |
| 8.8 | 284 | 123 | 30 001 | 100 % |
| 9.3 | 182 | 77 | 30 001 | 100 % |
| 9.8 | 11 | 2 | 30 000 | 100 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
