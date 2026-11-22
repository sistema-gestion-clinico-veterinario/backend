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
| Errores (%) | 40.6 % | 24.5 % |
| Ritmo medio (peticiones/s) | 15.8 | 24.1 |
| Tiempo mediano (ms) | 5 722 | 490 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| Usuarios sin degradación | 123 | 192 |
| CPU del backend, media (% del límite) | 29 | 49 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 35 | 21 |
| Memoria pico (MB) | 1 369 | 1 355 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.6 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 251.8 | 35.9 |
| Panel sin carga: mediana de 19 llamadas (ms) | 414 | 147 |
| Panel sin carga: primera llamada (ms) | 670 | 617 |
| Panel bajo carga: mediana (ms) | 6 030 | 425 |
| Panel bajo carga: p95 (ms) | 30 001 | 30 001 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 101 | 195 |
| Pausas completas | 16 | 30 |
| Pausas completas por "Allocation Failure" | 1 | 16 |
| Tiempo en pausas completas (s) | 4.0 | 13.1 |
| Tiempo en GC / vida de la JVM | 1.7 % | 4.5 % |
| Heap tras GC, máximo (MB) | 961 | 950 |
| Avisos de pool agotado | 4 089 | 3 187 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 1891; fallidas: 689.
- Acciones hechas: panel 539, agenda 441, historia 362, reportes 43, recepcion 751, atencion 212, cobro 68, cancelar 72; sin trabajo disponible: 548.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 4089 avisos en el registro.
- CPU del backend: 29 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 35 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 369 MB (67 % de 2048 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.8 con unos 303 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **123 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 393 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+0.5 %).
- GC: 101 pausas, 13.3 s en total (2 % del tiempo de vida de la JVM); 16 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 961 MB (tope -Xmx1g).
- Clases cargadas: 25461 en total, 36 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4506 |
| 0.3 | 80 | 81 | 16083 |
| 0.5 | 86 | 87 | 17091 |
| 0.6 | 94 | 95 | 18063 |
| 1.5 | 126 | 128 | 24022 |
| 2.0 | 130 | 132 | 24668 |
| 2.4 | 131 | 134 | 24957 |
| 2.7 | 132 | 135 | 25002 |
| 3.0 | 132 | 135 | 25086 |
| 3.4 | 133 | 136 | 25304 |
| 3.8 | 134 | 136 | 25308 |
| 4.4 | 134 | 137 | 25363 |
| 5.0 | 134 | 137 | 25372 |
| 6.2 | 135 | 138 | 25424 |
| 13.3 | 136 | 139 | 25461 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 18 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 19 |
| `org.apache.catalina.core` | 6 |
| `java.util.stream.ReduceOps` | 2 |
| `com.fasterxml.jackson.annotation` | 2 |
| `org.apache.catalina.AsyncDispatcher` | 1 |
| `java.util.function.IntBinaryOperator` | 1 |
| `java.util.stream.IntPipeline` | 1 |
| `org.springframework.boot.web` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 563 | 359.0 | 637.71 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 563 | 277.6 | 493.1 | 29 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 563 | 111.4 | 197.89 | 12 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 563 | 42.7 | 75.85 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 563 | 39.0 | 69.19 | 4 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 48 | 31.2 | 650.37 | 3 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 563 | 29.0 | 51.42 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 213 | 18.1 | 85.1 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 48 | 11.1 | 231.84 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 815 | 4.0 | 4.87 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 368 | 393 | la base tiene más (ver nota) |
| Mascotas | 356 | 357 | la base tiene más (ver nota) |
| Citas agendadas | 336 | 339 | la base tiene más (ver nota) |
| Diagnósticos | 80 | 81 | la base tiene más (ver nota) |
| Recetas | 78 | 79 | la base tiene más (ver nota) |
| Pagos | 40 | 40 | coincide |
| Citas completadas | 77 | 79 | la base tiene más (ver nota) |
| Citas canceladas | 51 | 52 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 368 | 383 | 51.0 % | 30 000 | 30 001 |
| Registrar mascota | 356 | 12 | 3.3 % | 433 | 13 077 |
| Agendar cita | 336 | 20 | 5.6 % | 426 | 11 488 |
| Recepción (llegada) | 141 | 71 | 33.5 % | 5 681 | 30 001 |
| Iniciar atención | 84 | 128 | 60.4 % | 5 088 | 30 001 |
| Leer la consulta | 82 | 2 | 2.4 % | 153 | 11 608 |
| Registrar diagnóstico | 80 | 4 | 4.8 % | 288 | 14 230 |
| Registrar receta | 78 | 6 | 7.1 % | 184 | 30 001 |
| Actualizar mascota | 77 | 7 | 8.3 % | 405 | 30 000 |
| Cerrar la consulta | 77 | 7 | 8.3 % | 255 | 30 000 |
| Caja: crear punto de cobro | 41 | 19 | 31.7 % | 1 936 | 30 001 |
| Caja: vincular equipo | 41 | 0 | 0.0 % | 262 | 9 815 |
| Caja: abrir la caja | 39 | 2 | 4.9 % | 503 | 15 218 |
| Cobrar | 40 | 7 | 14.9 % | 667 | 30 001 |
| Cancelar cita | 51 | 21 | 29.2 % | 3 360 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1714 | 1437 (84 %) | 30 000 | 30 001 | 28 % |
| apoderado | 751 | 383 (51 %) | 30 000 | 30 001 | 8 % |
| panel | 816 | 283 (35 %) | 6 030 | 30 001 | 7 % |
| privacidad | 901 | 310 (34 %) | 2 995 | 30 001 | 7 % |
| navegacion | 901 | 293 (33 %) | 3 346 | 30 001 | 7 % |
| cumplimiento | 878 | 300 (34 %) | 3 450 | 30 001 | 6 % |
| notificaciones | 853 | 285 (33 %) | 3 323 | 30 001 | 6 % |
| avisos_de_cumplimiento | 836 | 280 (33 %) | 3 121 | 30 001 | 6 % |
| empleados | 453 | 215 (47 %) | 9 852 | 30 001 | 4 % |
| citas_del_mes | 453 | 211 (47 %) | 9 984 | 30 001 | 4 % |
| propietarios | 441 | 213 (48 %) | 11 216 | 30 001 | 4 % |
| lista_de_historias | 362 | 164 (45 %) | 10 563 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 589 | 0 % |
| 0.6 | 48 | 267 | 575 | 0 % |
| 1.3 | 93 | 384 | 945 | 0 % |
| 2.1 | 138 | 481 | 2 234 | 1 % |
| 2.8 | 183 | 398 | 6 698 | 1 % |
| 3.6 | 228 | 318 | 14 475 | 2 % |
| 4.3 | 273 | 499 | 14 143 | 2 % |
| 5.1 | 318 | 220 | 30 001 | 91 % |
| 5.8 | 363 | 136 | 30 001 | 100 % |
| 6.6 | 408 | 181 | 30 001 | 100 % |
| 7.3 | 453 | 170 | 30 001 | 100 % |
| 8.1 | 498 | 165 | 30 001 | 100 % |
| 8.8 | 500 | 190 | 30 001 | 100 % |
| 9.6 | 500 | 190 | 30 001 | 100 % |
| 10.3 | 500 | 190 | 30 001 | 100 % |
| 11.1 | 469 | 192 | 30 001 | 100 % |
| 11.8 | 159 | 57 | 30 001 | 100 % |

</details>

## Código nuevo: FALLA; datos CONSISTENTES

- Escrituras correctas: 4383; fallidas: 755.
- Acciones hechas: panel 830, agenda 758, historia 639, reportes 77, recepcion 1077, atencion 414, cobro 177, cancelar 152; sin trabajo disponible: 775.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3187 avisos en el registro.
- El servidor rechazó trabajo por falta de hilos: 1 avisos.
- CPU del backend: 49 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 36 % del tiempo.
- CPU de PostgreSQL: 21 % de media y 99 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 355 MB (66 % de 2048 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 6.0 con unos 372 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **192 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 760 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.8 %).
- GC: 195 pausas, 35.4 s en total (5 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 950 MB (tope -Xmx1g).
- Clases cargadas: 25580 en total, 73 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4502 |
| 0.4 | 81 | 82 | 16122 |
| 0.7 | 116 | 117 | 22496 |
| 2.1 | 131 | 133 | 24829 |
| 2.7 | 132 | 135 | 25060 |
| 3.3 | 133 | 136 | 25297 |
| 3.7 | 134 | 137 | 25365 |
| 4.0 | 134 | 137 | 25368 |
| 4.4 | 134 | 137 | 25384 |
| 4.8 | 135 | 138 | 25457 |
| 5.2 | 135 | 138 | 25457 |
| 5.5 | 135 | 138 | 25457 |
| 5.8 | 136 | 139 | 25458 |
| 6.1 | 136 | 139 | 25499 |
| 6.4 | 136 | 139 | 25506 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 31 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 35 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.apache.tomcat.util` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `com.fasterxml.jackson.annotation` | 2 |
| `org.hibernate.internal.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 126 | 53.4 | 423.95 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 928 | 38.2 | 41.15 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 928 | 35.1 | 37.78 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 463 | 16.6 | 35.91 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 126 | 15.1 | 119.8 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1492 | 8.6 | 5.79 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 1492 | 3.3 | 2.2 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 1516 | 2.9 | 1.93 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 77191 | 2.6 | 0.03 | 1 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 928 | 2.2 | 2.36 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 729 | 761 | la base tiene más (ver nota) |
| Mascotas | 710 | 716 | la base tiene más (ver nota) |
| Citas agendadas | 688 | 693 | la base tiene más (ver nota) |
| Diagnósticos | 225 | 226 | la base tiene más (ver nota) |
| Recetas | 224 | 224 | coincide |
| Pagos | 138 | 141 | la base tiene más (ver nota) |
| Citas completadas | 223 | 223 | coincide |
| Citas canceladas | 111 | 119 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 729 | 348 | 32.3 % | 1 317 | 30 001 |
| Registrar mascota | 710 | 19 | 2.6 % | 164 | 5 947 |
| Agendar cita | 688 | 22 | 3.1 % | 159 | 4 303 |
| Recepción (llegada) | 331 | 83 | 20.0 % | 473 | 30 001 |
| Iniciar atención | 228 | 186 | 44.9 % | 597 | 30 001 |
| Leer la consulta | 227 | 1 | 0.4 % | 57 | 2 800 |
| Registrar diagnóstico | 225 | 3 | 1.3 % | 61 | 2 576 |
| Registrar receta | 224 | 4 | 1.8 % | 65 | 4 195 |
| Actualizar mascota | 224 | 4 | 1.8 % | 61 | 2 659 |
| Cerrar la consulta | 223 | 5 | 2.2 % | 118 | 3 775 |
| Caja: crear punto de cobro | 110 | 20 | 15.4 % | 479 | 30 001 |
| Caja: vincular equipo | 108 | 2 | 1.8 % | 133 | 4 496 |
| Caja: abrir la caja | 107 | 1 | 0.9 % | 139 | 4 437 |
| Cobrar | 138 | 16 | 10.4 % | 461 | 30 001 |
| Cancelar cita | 111 | 41 | 27.0 % | 1 471 | 30 001 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1283 | 948 (74 %) | 30 000 | 30 001 | 21 % |
| apoderado | 1077 | 348 (32 %) | 1 317 | 30 001 | 8 % |
| privacidad | 1279 | 331 (26 %) | 291 | 30 001 | 7 % |
| navegacion | 1279 | 306 (24 %) | 402 | 30 001 | 7 % |
| cumplimiento | 1248 | 312 (25 %) | 263 | 30 001 | 7 % |
| notificaciones | 1217 | 287 (24 %) | 159 | 30 001 | 6 % |
| avisos_de_cumplimiento | 1183 | 257 (22 %) | 182 | 30 001 | 6 % |
| panel | 1165 | 248 (21 %) | 425 | 30 001 | 6 % |
| empleados | 779 | 229 (29 %) | 905 | 30 001 | 5 % |
| citas_del_mes | 779 | 209 (27 %) | 826 | 30 001 | 5 % |
| propietarios | 758 | 218 (29 %) | 774 | 30 001 | 5 % |
| lista_de_historias | 639 | 197 (31 %) | 1 286 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 84 | 394 | 0 % |
| 0.7 | 57 | 280 | 398 | 0 % |
| 1.5 | 102 | 421 | 164 | 0 % |
| 2.2 | 147 | 582 | 283 | 0 % |
| 3.0 | 192 | 735 | 485 | 0 % |
| 3.7 | 237 | 951 | 888 | 0 % |
| 4.5 | 282 | 752 | 8 487 | 2 % |
| 5.2 | 327 | 749 | 5 729 | 2 % |
| 6.0 | 372 | 160 | 30 001 | 84 % |
| 6.7 | 417 | 172 | 30 001 | 100 % |
| 7.5 | 462 | 170 | 30 001 | 100 % |
| 8.2 | 500 | 196 | 30 001 | 100 % |
| 9.0 | 500 | 189 | 30 001 | 100 % |
| 9.7 | 500 | 192 | 30 001 | 100 % |
| 10.5 | 500 | 180 | 30 001 | 100 % |
| 11.2 | 419 | 201 | 30 001 | 100 % |
| 12.0 | 177 | 59 | 30 001 | 100 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
