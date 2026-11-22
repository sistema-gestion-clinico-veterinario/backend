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
| Ritmo medio (peticiones/s) | 31.5 | 32.7 |
| Tiempo mediano (ms) | 232 | 142 |
| Tiempo p95 (ms) | 2 975 | 2 317 |
| Usuarios sin degradación | 185 | 159 |
| CPU del backend, media (% del límite) | 49 | 53 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 72 | 72 |
| Memoria pico (MB) | 1 321 | 1 345 |
| Consultas sobre consentimiento_datos (llamadas) | 4 773 | 5 079 |
| Tiempo total en PostgreSQL (s) | 1.5 | 0.3 |
| Tiempo medio por llamada (ms) | 0.3 | 0.1 |
| Filas de consentimiento_datos al terminar | 1 286 | 1 380 |
| Tamaño de la tabla con índices (MB) | 0.5 | 0.6 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 174 | 193 |
| Pausas completas | 24 | 28 |
| Pausas completas por "Allocation Failure" | 11 | 15 |
| Tiempo en pausas completas (s) | 8.8 | 11.4 |
| Tiempo en GC / vida de la JVM | 5.6 % | 6.5 % |
| Heap tras GC, máximo (MB) | 960 | 983 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4380; fallidas: 87.
- Acciones hechas: panel 642, agenda 516, historia 452, reportes 59, recepcion 643, atencion 322, cobro 164, cancelar 109; sin trabajo disponible: 483.
- CPU del backend: 49 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 72 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 321 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **185 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.8 s.
- Correo: el servidor de pruebas recibió 643 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 174 pausas, 22.8 s en total (6 % del tiempo de vida de la JVM); 24 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 960 MB (tope -Xmx1g).
- Clases cargadas: 25510 en total, 216 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4522 |
| 0.3 | 81 | 81 | 16114 |
| 0.4 | 97 | 99 | 18694 |
| 1.6 | 129 | 132 | 24575 |
| 2.3 | 132 | 134 | 24980 |
| 2.6 | 132 | 135 | 25133 |
| 3.0 | 133 | 136 | 25294 |
| 3.4 | 134 | 137 | 25343 |
| 3.7 | 134 | 137 | 25376 |
| 4.1 | 134 | 137 | 25379 |
| 4.6 | 135 | 138 | 25401 |
| 5.1 | 135 | 138 | 25468 |
| 5.5 | 135 | 138 | 25468 |
| 6.1 | 136 | 139 | 25510 |
| 6.6 | 136 | 139 | 25510 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 208 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 214 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 842 | 591.6 | 702.63 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 842 | 420.4 | 499.25 | 27 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 842 | 140.8 | 167.24 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 118 | 93.7 | 793.84 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 842 | 59.6 | 70.78 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 842 | 55.1 | 65.42 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 842 | 53.3 | 63.36 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 516 | 34.1 | 66.13 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 452 | 33.6 | 74.3 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 118 | 26.6 | 225.43 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 643 | 643 | coincide |
| Mascotas | 643 | 643 | coincide |
| Citas agendadas | 630 | 630 | coincide |
| Diagnósticos | 248 | 248 | coincide |
| Recetas | 248 | 248 | coincide |
| Pagos | 164 | 164 | coincide |
| Citas completadas | 248 | 248 | coincide |
| Citas canceladas | 109 | 109 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 643 | 0 | 0.0 % | 296 | 2 692 |
| Registrar mascota | 643 | 0 | 0.0 % | 184 | 2 491 |
| Agendar cita | 630 | 13 | 2.0 % | 203 | 2 544 |
| Recepción (llegada) | 322 | 0 | 0.0 % | 378 | 2 673 |
| Iniciar atención | 248 | 74 | 23.0 % | 280 | 3 101 |
| Leer la consulta | 248 | 0 | 0.0 % | 93 | 1 994 |
| Registrar diagnóstico | 248 | 0 | 0.0 % | 104 | 2 364 |
| Registrar receta | 248 | 0 | 0.0 % | 85 | 2 437 |
| Actualizar mascota | 248 | 0 | 0.0 % | 90 | 2 393 |
| Cerrar la consulta | 248 | 0 | 0.0 % | 115 | 2 531 |
| Caja: crear punto de cobro | 127 | 0 | 0.0 % | 345 | 2 445 |
| Caja: vincular equipo | 127 | 0 | 0.0 % | 274 | 2 406 |
| Caja: abrir la caja | 127 | 0 | 0.0 % | 311 | 2 604 |
| Cobrar | 164 | 0 | 0.0 % | 358 | 2 491 |
| Cancelar cita | 109 | 0 | 0.0 % | 355 | 3 274 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 842 | 0 (0 %) | 1 931 | 5 678 | 21 % |
| navegacion | 842 | 0 (0 %) | 114 | 2 651 | 6 % |
| apoderado | 643 | 0 (0 %) | 296 | 2 692 | 5 % |
| citas_del_mes | 516 | 0 (0 %) | 368 | 2 822 | 5 % |
| avisos_de_cumplimiento | 842 | 0 (0 %) | 64 | 2 151 | 4 % |
| cita | 643 | 13 (2 %) | 203 | 2 544 | 4 % |
| mascota | 643 | 0 (0 %) | 184 | 2 491 | 4 % |
| propietarios | 516 | 0 (0 %) | 296 | 2 982 | 4 % |
| cumplimiento | 842 | 0 (0 %) | 89 | 1 981 | 4 % |
| notificaciones | 842 | 0 (0 %) | 64 | 1 843 | 4 % |
| lista_de_historias | 452 | 0 (0 %) | 389 | 2 561 | 4 % |
| privacidad | 842 | 0 (0 %) | 57 | 1 751 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 94 | 412 | 0 % |
| 0.2 | 28 | 147 | 385 | 0 % |
| 0.5 | 42 | 194 | 347 | 0 % |
| 0.8 | 56 | 248 | 267 | 0 % |
| 1.0 | 71 | 333 | 329 | 0 % |
| 1.2 | 85 | 415 | 299 | 0 % |
| 1.5 | 99 | 402 | 326 | 0 % |
| 1.8 | 114 | 467 | 942 | 0 % |
| 2.0 | 128 | 498 | 636 | 0 % |
| 2.2 | 142 | 600 | 394 | 0 % |
| 2.5 | 156 | 605 | 478 | 0 % |
| 2.8 | 171 | 690 | 1 198 | 0 % |
| 3.0 | 185 | 650 | 1 705 | 0 % |
| 3.2 | 199 | 591 | 3 936 | 1 % |
| 3.5 | 200 | 560 | 3 665 | 2 % |
| 3.8 | 200 | 615 | 4 035 | 1 % |
| 4.0 | 200 | 688 | 1 680 | 0 % |
| 4.2 | 200 | 723 | 431 | 0 % |
| 4.5 | 200 | 774 | 1 173 | 1 % |
| 4.8 | 200 | 605 | 2 711 | 1 % |
| 5.0 | 200 | 613 | 2 910 | 1 % |
| 5.2 | 200 | 530 | 3 305 | 2 % |
| 5.5 | 200 | 625 | 3 357 | 1 % |
| 5.8 | 200 | 430 | 4 827 | 2 % |
| 6.0 | 200 | 538 | 4 622 | 2 % |
| 6.2 | 168 | 165 | 3 120 | 1 % |

</details>

## CON índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4580; fallidas: 80.
- Acciones hechas: panel 680, agenda 558, historia 435, reportes 69, recepcion 690, atencion 325, cobro 161, cancelar 133; sin trabajo disponible: 492.
- CPU del backend: 53 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 8 % del tiempo.
- CPU de PostgreSQL: 72 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 345 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **159 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 5.1 s.
- Correo: el servidor de pruebas recibió 690 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 193 pausas, 27.6 s en total (7 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 983 MB (tope -Xmx1g).
- Clases cargadas: 25530 en total, 185 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4502 |
| 0.3 | 81 | 82 | 16123 |
| 0.6 | 116 | 118 | 22538 |
| 1.6 | 130 | 132 | 24653 |
| 2.3 | 132 | 134 | 24982 |
| 2.7 | 132 | 135 | 25144 |
| 3.3 | 133 | 136 | 25311 |
| 3.8 | 134 | 137 | 25348 |
| 4.2 | 134 | 137 | 25379 |
| 4.5 | 135 | 138 | 25401 |
| 4.9 | 135 | 138 | 25451 |
| 5.6 | 136 | 138 | 25468 |
| 6.1 | 136 | 139 | 25471 |
| 6.4 | 136 | 139 | 25512 |
| 6.7 | 136 | 139 | 25513 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 177 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 182 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 880 | 549.8 | 624.82 | 39 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 880 | 316.2 | 359.34 | 22 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 880 | 143.3 | 162.86 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 138 | 105.4 | 763.85 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 880 | 60.9 | 69.25 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 880 | 56.6 | 64.31 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 880 | 56.0 | 63.64 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 558 | 40.1 | 71.95 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 435 | 32.6 | 74.91 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 138 | 31.1 | 225.48 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 690 | 690 | coincide |
| Mascotas | 690 | 690 | coincide |
| Citas agendadas | 676 | 676 | coincide |
| Diagnósticos | 259 | 259 | coincide |
| Recetas | 259 | 259 | coincide |
| Pagos | 161 | 161 | coincide |
| Citas completadas | 259 | 259 | coincide |
| Citas canceladas | 133 | 133 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 690 | 0 | 0.0 % | 196 | 2 130 |
| Registrar mascota | 690 | 0 | 0.0 % | 108 | 1 675 |
| Agendar cita | 676 | 14 | 2.0 % | 113 | 1 785 |
| Recepción (llegada) | 325 | 0 | 0.0 % | 258 | 2 616 |
| Iniciar atención | 259 | 66 | 20.3 % | 234 | 2 338 |
| Leer la consulta | 259 | 0 | 0.0 % | 77 | 2 309 |
| Registrar diagnóstico | 259 | 0 | 0.0 % | 70 | 1 417 |
| Registrar receta | 259 | 0 | 0.0 % | 64 | 2 033 |
| Actualizar mascota | 259 | 0 | 0.0 % | 61 | 1 523 |
| Cerrar la consulta | 259 | 0 | 0.0 % | 88 | 1 899 |
| Caja: crear punto de cobro | 117 | 0 | 0.0 % | 198 | 2 001 |
| Caja: vincular equipo | 117 | 0 | 0.0 % | 129 | 2 148 |
| Caja: abrir la caja | 117 | 0 | 0.0 % | 124 | 1 793 |
| Cobrar | 161 | 0 | 0.0 % | 237 | 1 938 |
| Cancelar cita | 133 | 0 | 0.0 % | 275 | 2 012 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 880 | 0 (0 %) | 1 623 | 4 876 | 23 % |
| apoderado | 690 | 0 (0 %) | 196 | 2 130 | 5 % |
| citas_del_mes | 558 | 0 (0 %) | 245 | 2 228 | 5 % |
| propietarios | 558 | 0 (0 %) | 244 | 1 984 | 4 % |
| navegacion | 880 | 0 (0 %) | 45 | 1 607 | 4 % |
| reporte_clinico | 69 | 0 (0 %) | 4 360 | 6 619 | 4 % |
| mascota | 690 | 0 (0 %) | 108 | 1 675 | 4 % |
| cita | 690 | 14 (2 %) | 113 | 1 785 | 4 % |
| lista_de_historias | 435 | 0 (0 %) | 250 | 2 185 | 4 % |
| empleados | 558 | 0 (0 %) | 108 | 1 914 | 4 % |
| cumplimiento | 880 | 0 (0 %) | 42 | 1 340 | 3 % |
| notificaciones | 880 | 0 (0 %) | 31 | 1 252 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 2 | 6 | 1 253 | 0 % |
| 0.1 | 16 | 121 | 510 | 0 % |
| 0.3 | 31 | 169 | 385 | 1 % |
| 0.6 | 45 | 231 | 354 | 0 % |
| 0.8 | 59 | 322 | 314 | 0 % |
| 1.1 | 74 | 342 | 345 | 0 % |
| 1.3 | 88 | 393 | 421 | 0 % |
| 1.6 | 102 | 421 | 363 | 0 % |
| 1.8 | 116 | 485 | 555 | 0 % |
| 2.0 | 131 | 544 | 382 | 0 % |
| 2.3 | 145 | 617 | 498 | 0 % |
| 2.5 | 159 | 642 | 1 405 | 0 % |
| 2.8 | 174 | 590 | 2 354 | 1 % |
| 3.0 | 188 | 560 | 2 910 | 2 % |
| 3.3 | 200 | 610 | 2 912 | 1 % |
| 3.5 | 200 | 610 | 3 029 | 1 % |
| 3.8 | 200 | 726 | 1 806 | 1 % |
| 4.0 | 200 | 734 | 1 204 | 0 % |
| 4.3 | 200 | 772 | 280 | 0 % |
| 4.5 | 200 | 659 | 2 394 | 0 % |
| 4.8 | 200 | 537 | 4 225 | 2 % |
| 5.0 | 200 | 547 | 5 063 | 3 % |
| 5.3 | 200 | 690 | 2 067 | 1 % |
| 5.5 | 200 | 718 | 797 | 0 % |
| 5.8 | 200 | 651 | 2 019 | 1 % |
| 6.0 | 200 | 503 | 1 771 | 0 % |
| 6.3 | 137 | 126 | 221 | 0 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
