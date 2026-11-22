# Fase 3c: índice en consentimiento_datos: 200 usuarios (1 CPU, 2 GB)

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | SIN índice | CON índice |
|---|---|---|
| Veredicto | DEGRADADO | DEGRADADO |
| Errores (%) | 0.7 % | 0.6 % |
| Ritmo medio (peticiones/s) | 29.8 | 32.3 |
| Tiempo mediano (ms) | 474 | 169 |
| Tiempo p95 (ms) | 3 659 | 2 422 |
| Usuarios sin degradación | 150 | 165 |
| CPU del backend, media (% del límite) | 51 | 50 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 72 | 69 |
| Memoria pico (MB) | 1 334 | 1 322 |
| Consultas sobre consentimiento_datos (llamadas) | 4 648 | 5 105 |
| Tiempo total en PostgreSQL (s) | 3.0 | 0.4 |
| Tiempo medio por llamada (ms) | 0.7 | 0.1 |
| Filas de consentimiento_datos al terminar | 1 254 | 1 410 |
| Tamaño de la tabla con índices (MB) | 0.6 | 0.6 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 180 | 159 |
| Pausas completas | 27 | 22 |
| Pausas completas por "Allocation Failure" | 13 | 8 |
| Tiempo en pausas completas (s) | 11.6 | 7.5 |
| Tiempo en GC / vida de la JVM | 6.8 % | 4.9 % |
| Heap tras GC, máximo (MB) | 977 | 962 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4063; fallidas: 89.
- Acciones hechas: panel 633, agenda 473, historia 409, reportes 65, recepcion 627, atencion 299, cobro 134, cancelar 123; sin trabajo disponible: 462.
- CPU del backend: 51 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 72 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 334 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **150 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.1 s.
- Correo: el servidor de pruebas recibió 627 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 180 pausas, 28.5 s en total (7 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 977 MB (tope -Xmx1g).
- Clases cargadas: 25524 en total, 182 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4527 |
| 0.3 | 81 | 81 | 16113 |
| 0.5 | 107 | 108 | 20728 |
| 1.8 | 130 | 133 | 24756 |
| 2.5 | 132 | 135 | 25055 |
| 3.1 | 133 | 136 | 25305 |
| 3.4 | 134 | 137 | 25342 |
| 3.9 | 134 | 137 | 25389 |
| 4.2 | 134 | 137 | 25391 |
| 4.6 | 135 | 138 | 25398 |
| 5.0 | 135 | 138 | 25465 |
| 5.4 | 135 | 138 | 25466 |
| 5.8 | 135 | 138 | 25466 |
| 6.3 | 136 | 139 | 25468 |
| 6.6 | 136 | 139 | 25468 |
| 6.9 | 136 | 139 | 25516 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 175 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 180 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 833 | 624.1 | 749.18 | 37 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 833 | 455.6 | 546.94 | 27 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 833 | 166.4 | 199.8 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 130 | 122.1 | 939.21 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 833 | 67.5 | 81.02 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 833 | 62.6 | 75.14 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 833 | 58.7 | 70.43 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 473 | 38.2 | 80.85 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 409 | 34.3 | 83.9 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 130 | 33.5 | 257.7 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 627 | 627 | coincide |
| Mascotas | 627 | 627 | coincide |
| Citas agendadas | 615 | 615 | coincide |
| Diagnósticos | 222 | 222 | coincide |
| Recetas | 222 | 222 | coincide |
| Pagos | 134 | 134 | coincide |
| Citas completadas | 222 | 222 | coincide |
| Citas canceladas | 123 | 123 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 627 | 0 | 0.0 % | 612 | 3 159 |
| Registrar mascota | 627 | 0 | 0.0 % | 379 | 3 257 |
| Agendar cita | 615 | 12 | 1.9 % | 402 | 3 249 |
| Recepción (llegada) | 299 | 0 | 0.0 % | 707 | 3 067 |
| Iniciar atención | 222 | 77 | 25.8 % | 709 | 3 347 |
| Leer la consulta | 222 | 0 | 0.0 % | 303 | 2 521 |
| Registrar diagnóstico | 222 | 0 | 0.0 % | 369 | 3 234 |
| Registrar receta | 222 | 0 | 0.0 % | 319 | 3 127 |
| Actualizar mascota | 222 | 0 | 0.0 % | 356 | 3 267 |
| Cerrar la consulta | 222 | 0 | 0.0 % | 386 | 2 740 |
| Caja: crear punto de cobro | 102 | 0 | 0.0 % | 662 | 2 693 |
| Caja: vincular equipo | 102 | 0 | 0.0 % | 458 | 2 490 |
| Caja: abrir la caja | 102 | 0 | 0.0 % | 551 | 2 451 |
| Cobrar | 134 | 0 | 0.0 % | 673 | 3 005 |
| Cancelar cita | 123 | 0 | 0.0 % | 818 | 3 112 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 833 | 0 (0 %) | 2 584 | 6 571 | 18 % |
| navegacion | 833 | 0 (0 %) | 431 | 3 130 | 6 % |
| apoderado | 627 | 0 (0 %) | 612 | 3 159 | 5 % |
| citas_del_mes | 473 | 0 (0 %) | 860 | 3 495 | 5 % |
| avisos_de_cumplimiento | 833 | 0 (0 %) | 218 | 2 968 | 5 % |
| propietarios | 473 | 0 (0 %) | 641 | 3 593 | 4 % |
| notificaciones | 833 | 0 (0 %) | 239 | 2 345 | 4 % |
| cita | 627 | 12 (2 %) | 402 | 3 249 | 4 % |
| mascota | 627 | 0 (0 %) | 379 | 3 257 | 4 % |
| cumplimiento | 833 | 0 (0 %) | 245 | 2 781 | 4 % |
| privacidad | 833 | 0 (0 %) | 241 | 2 528 | 4 % |
| lista_de_historias | 409 | 0 (0 %) | 756 | 3 496 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 49 | 966 | 0 % |
| 0.1 | 21 | 111 | 363 | 0 % |
| 0.4 | 36 | 181 | 422 | 0 % |
| 0.6 | 50 | 250 | 324 | 0 % |
| 0.9 | 64 | 317 | 346 | 0 % |
| 1.1 | 78 | 351 | 489 | 0 % |
| 1.4 | 93 | 386 | 328 | 0 % |
| 1.6 | 107 | 462 | 323 | 0 % |
| 1.9 | 121 | 566 | 394 | 0 % |
| 2.1 | 136 | 557 | 896 | 0 % |
| 2.4 | 150 | 535 | 761 | 0 % |
| 2.6 | 164 | 535 | 2 806 | 0 % |
| 2.9 | 178 | 598 | 3 112 | 1 % |
| 3.1 | 193 | 508 | 3 344 | 1 % |
| 3.4 | 200 | 539 | 5 568 | 0 % |
| 3.6 | 200 | 544 | 4 610 | 2 % |
| 3.9 | 200 | 742 | 2 107 | 1 % |
| 4.1 | 200 | 712 | 2 062 | 1 % |
| 4.4 | 200 | 645 | 2 274 | 1 % |
| 4.6 | 200 | 550 | 4 295 | 1 % |
| 4.9 | 200 | 499 | 3 748 | 1 % |
| 5.1 | 200 | 620 | 3 471 | 1 % |
| 5.4 | 200 | 554 | 3 569 | 2 % |
| 5.6 | 200 | 470 | 4 397 | 1 % |
| 5.9 | 200 | 432 | 6 099 | 2 % |
| 6.1 | 194 | 413 | 6 071 | 1 % |
| 6.4 | 121 | 65 | 5 285 | 0 % |

</details>

## CON índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4651; fallidas: 74.
- Acciones hechas: panel 670, agenda 544, historia 438, reportes 49, recepcion 705, atencion 325, cobro 152, cancelar 138; sin trabajo disponible: 468.
- CPU del backend: 50 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 69 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 322 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **165 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.0 s.
- Correo: el servidor de pruebas recibió 705 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.8 %).
- GC: 159 pausas, 20.3 s en total (5 % del tiempo de vida de la JVM); 22 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 962 MB (tope -Xmx1g).
- Clases cargadas: 25516 en total, 190 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4514 |
| 0.3 | 80 | 81 | 16103 |
| 0.4 | 89 | 90 | 17414 |
| 1.3 | 128 | 130 | 24460 |
| 2.2 | 131 | 134 | 24942 |
| 2.8 | 133 | 136 | 25197 |
| 3.2 | 133 | 136 | 25311 |
| 3.5 | 134 | 137 | 25330 |
| 3.9 | 134 | 137 | 25380 |
| 4.3 | 134 | 137 | 25398 |
| 4.7 | 135 | 138 | 25406 |
| 5.1 | 135 | 138 | 25472 |
| 5.5 | 136 | 139 | 25472 |
| 6.0 | 136 | 139 | 25473 |
| 6.4 | 136 | 139 | 25513 |
| 6.9 | 136 | 139 | 25516 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 183 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 188 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 870 | 544.4 | 625.79 | 37 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 870 | 366.1 | 420.76 | 25 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 870 | 157.9 | 181.44 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 98 | 77.2 | 787.95 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 870 | 63.8 | 73.38 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 870 | 59.9 | 68.82 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 870 | 59.6 | 68.51 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 544 | 38.6 | 70.93 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 438 | 34.9 | 79.74 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 98 | 22.1 | 225.49 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 705 | 705 | coincide |
| Mascotas | 705 | 705 | coincide |
| Citas agendadas | 691 | 691 | coincide |
| Diagnósticos | 265 | 265 | coincide |
| Recetas | 265 | 265 | coincide |
| Pagos | 152 | 152 | coincide |
| Citas completadas | 265 | 265 | coincide |
| Citas canceladas | 138 | 138 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 705 | 0 | 0.0 % | 199 | 2 111 |
| Registrar mascota | 705 | 0 | 0.0 % | 148 | 1 992 |
| Agendar cita | 691 | 14 | 2.0 % | 156 | 1 991 |
| Recepción (llegada) | 325 | 0 | 0.0 % | 246 | 2 116 |
| Iniciar atención | 265 | 60 | 18.5 % | 220 | 2 057 |
| Leer la consulta | 265 | 0 | 0.0 % | 109 | 1 613 |
| Registrar diagnóstico | 265 | 0 | 0.0 % | 91 | 1 461 |
| Registrar receta | 265 | 0 | 0.0 % | 103 | 1 771 |
| Actualizar mascota | 265 | 0 | 0.0 % | 83 | 1 964 |
| Cerrar la consulta | 265 | 0 | 0.0 % | 155 | 1 975 |
| Caja: crear punto de cobro | 115 | 0 | 0.0 % | 273 | 1 985 |
| Caja: vincular equipo | 115 | 0 | 0.0 % | 178 | 2 227 |
| Caja: abrir la caja | 115 | 0 | 0.0 % | 182 | 1 582 |
| Cobrar | 152 | 0 | 0.0 % | 162 | 1 853 |
| Cancelar cita | 138 | 0 | 0.0 % | 257 | 2 113 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 870 | 0 (0 %) | 1 794 | 5 142 | 23 % |
| apoderado | 705 | 0 (0 %) | 199 | 2 111 | 5 % |
| navegacion | 870 | 0 (0 %) | 55 | 1 978 | 5 % |
| citas_del_mes | 544 | 0 (0 %) | 319 | 2 400 | 5 % |
| mascota | 705 | 0 (0 %) | 148 | 1 992 | 4 % |
| cita | 705 | 14 (2 %) | 156 | 1 991 | 4 % |
| propietarios | 544 | 0 (0 %) | 253 | 2 267 | 4 % |
| lista_de_historias | 438 | 0 (0 %) | 333 | 2 328 | 4 % |
| cumplimiento | 870 | 0 (0 %) | 52 | 1 431 | 4 % |
| avisos_de_cumplimiento | 870 | 0 (0 %) | 33 | 1 682 | 4 % |
| empleados | 544 | 0 (0 %) | 135 | 1 945 | 4 % |
| privacidad | 870 | 0 (0 %) | 36 | 1 373 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 986 | 0 % |
| 0.1 | 22 | 122 | 365 | 0 % |
| 0.4 | 36 | 190 | 302 | 0 % |
| 0.7 | 51 | 223 | 389 | 0 % |
| 0.9 | 65 | 275 | 369 | 0 % |
| 1.1 | 79 | 348 | 349 | 0 % |
| 1.4 | 94 | 391 | 347 | 0 % |
| 1.6 | 108 | 455 | 498 | 0 % |
| 1.9 | 122 | 551 | 408 | 0 % |
| 2.1 | 136 | 535 | 389 | 0 % |
| 2.4 | 151 | 649 | 506 | 0 % |
| 2.6 | 165 | 533 | 1 868 | 0 % |
| 2.9 | 179 | 622 | 2 654 | 1 % |
| 3.1 | 194 | 625 | 2 513 | 1 % |
| 3.4 | 200 | 494 | 3 976 | 0 % |
| 3.6 | 200 | 680 | 3 842 | 2 % |
| 3.9 | 200 | 720 | 682 | 0 % |
| 4.2 | 200 | 726 | 1 018 | 0 % |
| 4.4 | 200 | 680 | 2 668 | 1 % |
| 4.7 | 200 | 624 | 2 664 | 0 % |
| 4.9 | 200 | 631 | 3 090 | 2 % |
| 5.2 | 200 | 641 | 3 416 | 1 % |
| 5.4 | 200 | 528 | 3 322 | 1 % |
| 5.7 | 200 | 843 | 1 165 | 1 % |
| 5.9 | 200 | 700 | 2 127 | 1 % |
| 6.2 | 183 | 360 | 397 | 0 % |
| 6.4 | 93 | 36 | 397 | 0 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
