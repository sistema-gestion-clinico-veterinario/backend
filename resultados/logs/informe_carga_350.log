# Fase 3c: índice en consentimiento_datos: 350 usuarios (1 CPU, 2 GB)

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
| Veredicto | SATURADO (muy lento, casi sin errores) | SATURADO (muy lento, casi sin errores) |
| Errores (%) | 1.3 % | 1.4 % |
| Ritmo medio (peticiones/s) | 34.4 | 35.7 |
| Tiempo mediano (ms) | 1 221 | 1 191 |
| Tiempo p95 (ms) | 10 306 | 8 969 |
| Usuarios sin degradación | 157 | 153 |
| CPU del backend, media (% del límite) | 49 | 60 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 80 | 79 |
| Memoria pico (MB) | 1 369 | 1 367 |
| Consultas sobre consentimiento_datos (llamadas) | 7 540 | 7 857 |
| Tiempo total en PostgreSQL (s) | 25.1 | 0.5 |
| Tiempo medio por llamada (ms) | 3.3 | 0.1 |
| Filas de consentimiento_datos al terminar | 2 058 | 2 176 |
| Tamaño de la tabla con índices (MB) | 0.5 | 0.7 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 241 | 271 |
| Pausas completas | 30 | 35 |
| Pausas completas por "Allocation Failure" | 16 | 22 |
| Tiempo en pausas completas (s) | 12.1 | 15.8 |
| Tiempo en GC / vida de la JVM | 6.4 % | 7.7 % |
| Heap tras GC, máximo (MB) | 989 | 989 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5629; fallidas: 251.
- Acciones hechas: panel 973, agenda 777, historia 683, reportes 82, recepcion 1029, atencion 469, cobro 156, cancelar 178; sin trabajo disponible: 725.
- CPU del backend: 49 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 3 % del tiempo.
- CPU de PostgreSQL: 80 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 369 MB (67 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **157 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 16.6 s.
- Correo: el servidor de pruebas recibió 1029 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 241 pausas, 35.4 s en total (6 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25531 en total, 128 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4522 |
| 0.3 | 86 | 87 | 17103 |
| 1.5 | 129 | 132 | 24594 |
| 2.4 | 132 | 135 | 25017 |
| 3.0 | 133 | 136 | 25308 |
| 3.6 | 134 | 137 | 25376 |
| 4.2 | 135 | 138 | 25403 |
| 4.7 | 135 | 138 | 25426 |
| 5.5 | 135 | 138 | 25469 |
| 6.3 | 136 | 139 | 25471 |
| 7.1 | 136 | 139 | 25471 |
| 7.7 | 136 | 139 | 25472 |
| 8.3 | 136 | 139 | 25495 |
| 8.7 | 136 | 139 | 25530 |
| 9.2 | 136 | 139 | 25531 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 122 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 127 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1323 | 781.6 | 590.78 | 34 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1323 | 465.8 | 352.1 | 21 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1323 | 287.7 | 217.46 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 164 | 147.0 | 896.2 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1323 | 120.1 | 90.81 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1323 | 112.8 | 85.27 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1323 | 112.5 | 85.04 | 5 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 683 | 60.7 | 88.9 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 777 | 54.4 | 70.03 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 164 | 41.2 | 250.95 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1029 | 1029 | coincide |
| Mascotas | 1029 | 1029 | coincide |
| Citas agendadas | 1013 | 1013 | coincide |
| Diagnósticos | 234 | 234 | coincide |
| Recetas | 234 | 234 | coincide |
| Pagos | 156 | 156 | coincide |
| Citas completadas | 234 | 234 | coincide |
| Citas canceladas | 178 | 178 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1029 | 0 | 0.0 % | 1 578 | 10 668 |
| Registrar mascota | 1029 | 0 | 0.0 % | 1 021 | 10 444 |
| Agendar cita | 1013 | 16 | 1.6 % | 1 285 | 10 009 |
| Recepción (llegada) | 469 | 0 | 0.0 % | 1 991 | 11 292 |
| Iniciar atención | 234 | 235 | 50.1 % | 2 024 | 12 568 |
| Leer la consulta | 234 | 0 | 0.0 % | 394 | 8 602 |
| Registrar diagnóstico | 234 | 0 | 0.0 % | 469 | 9 101 |
| Registrar receta | 234 | 0 | 0.0 % | 624 | 9 244 |
| Actualizar mascota | 234 | 0 | 0.0 % | 473 | 8 337 |
| Cerrar la consulta | 234 | 0 | 0.0 % | 557 | 8 082 |
| Caja: crear punto de cobro | 117 | 0 | 0.0 % | 884 | 8 360 |
| Caja: vincular equipo | 117 | 0 | 0.0 % | 894 | 10 850 |
| Caja: abrir la caja | 117 | 0 | 0.0 % | 968 | 9 637 |
| Cobrar | 156 | 0 | 0.0 % | 1 197 | 10 593 |
| Cancelar cita | 178 | 0 | 0.0 % | 2 034 | 10 399 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1323 | 0 (0 %) | 2 679 | 12 778 | 11 % |
| navegacion | 1323 | 0 (0 %) | 1 031 | 9 651 | 6 % |
| cumplimiento | 1323 | 0 (0 %) | 759 | 9 411 | 6 % |
| apoderado | 1029 | 0 (0 %) | 1 578 | 10 668 | 6 % |
| avisos_de_cumplimiento | 1323 | 0 (0 %) | 827 | 9 133 | 6 % |
| notificaciones | 1323 | 0 (0 %) | 711 | 8 809 | 6 % |
| cita | 1029 | 16 (2 %) | 1 285 | 10 009 | 6 % |
| mascota | 1029 | 0 (0 %) | 1 021 | 10 444 | 5 % |
| propietarios | 777 | 0 (0 %) | 1 637 | 11 892 | 5 % |
| citas_del_mes | 777 | 0 (0 %) | 2 104 | 10 983 | 5 % |
| privacidad | 1323 | 0 (0 %) | 679 | 7 582 | 5 % |
| empleados | 777 | 0 (0 %) | 1 268 | 11 507 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 54 | 986 | 0 % |
| 0.4 | 37 | 191 | 287 | 0 % |
| 0.9 | 67 | 297 | 364 | 0 % |
| 1.4 | 97 | 460 | 401 | 0 % |
| 1.9 | 127 | 515 | 269 | 0 % |
| 2.4 | 157 | 613 | 1 759 | 0 % |
| 2.9 | 187 | 688 | 2 304 | 1 % |
| 3.4 | 217 | 622 | 3 116 | 1 % |
| 3.9 | 247 | 654 | 5 605 | 1 % |
| 4.4 | 277 | 631 | 7 701 | 1 % |
| 4.9 | 307 | 568 | 9 680 | 1 % |
| 5.4 | 337 | 426 | 14 189 | 2 % |
| 5.9 | 350 | 362 | 16 556 | 3 % |
| 6.4 | 350 | 334 | 15 586 | 1 % |
| 6.9 | 350 | 560 | 12 993 | 2 % |
| 7.4 | 350 | 578 | 7 715 | 2 % |
| 7.9 | 350 | 709 | 7 971 | 4 % |
| 8.4 | 346 | 921 | 1 648 | 1 % |

</details>

## CON índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 6176; fallidas: 280.
- Acciones hechas: panel 987, agenda 830, historia 663, reportes 96, recepcion 1088, atencion 531, cobro 167, cancelar 211; sin trabajo disponible: 802.
- CPU del backend: 60 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 79 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 367 MB (67 % de 2048 MB).
- Los fallos fueron: 0 % errores 5xx del servidor, 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **153 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 16.7 s.
- Correo: el servidor de pruebas recibió 1088 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (-0.0 %).
- GC: 271 pausas, 43.0 s en total (8 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25590 en total, 188 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4514 |
| 0.4 | 87 | 88 | 17205 |
| 1.6 | 130 | 132 | 24647 |
| 2.4 | 132 | 135 | 25029 |
| 3.1 | 133 | 136 | 25311 |
| 3.9 | 134 | 137 | 25379 |
| 4.3 | 134 | 137 | 25395 |
| 4.9 | 135 | 138 | 25404 |
| 5.8 | 136 | 139 | 25470 |
| 6.3 | 136 | 139 | 25471 |
| 7.1 | 136 | 139 | 25504 |
| 7.6 | 136 | 139 | 25521 |
| 8.2 | 136 | 139 | 25523 |
| 8.7 | 134 | 139 | 25563 |
| 9.1 | 135 | 139 | 25590 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 146 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 153 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.postgresql.core.EncodingPredictor` | 1 |
| `org.postgresql.util.ServerErrorMessage` | 1 |
| `org.postgresql.util.GT` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1337 | 769.5 | 575.51 | 36 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1337 | 385.2 | 288.13 | 18 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1337 | 277.6 | 207.61 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 192 | 143.7 | 748.23 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1337 | 118.3 | 88.49 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1337 | 111.0 | 83.0 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1337 | 110.3 | 82.5 | 5 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 663 | 52.8 | 79.6 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 830 | 49.4 | 59.56 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 192 | 41.1 | 213.89 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1088 | 1088 | coincide |
| Mascotas | 1088 | 1088 | coincide |
| Citas agendadas | 1069 | 1069 | coincide |
| Diagnósticos | 270 | 270 | coincide |
| Recetas | 270 | 270 | coincide |
| Pagos | 167 | 167 | coincide |
| Citas completadas | 270 | 270 | coincide |
| Citas canceladas | 211 | 211 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1088 | 0 | 0.0 % | 1 514 | 9 262 |
| Registrar mascota | 1088 | 0 | 0.0 % | 1 141 | 9 451 |
| Agendar cita | 1069 | 19 | 1.7 % | 1 282 | 9 211 |
| Recepción (llegada) | 531 | 0 | 0.0 % | 1 614 | 9 557 |
| Iniciar atención | 270 | 261 | 49.2 % | 1 731 | 11 477 |
| Leer la consulta | 270 | 0 | 0.0 % | 287 | 6 685 |
| Registrar diagnóstico | 270 | 0 | 0.0 % | 319 | 6 722 |
| Registrar receta | 270 | 0 | 0.0 % | 482 | 8 217 |
| Actualizar mascota | 270 | 0 | 0.0 % | 347 | 7 356 |
| Cerrar la consulta | 270 | 0 | 0.0 % | 470 | 6 205 |
| Caja: crear punto de cobro | 134 | 0 | 0.0 % | 1 307 | 9 748 |
| Caja: vincular equipo | 134 | 0 | 0.0 % | 877 | 7 071 |
| Caja: abrir la caja | 134 | 0 | 0.0 % | 856 | 8 743 |
| Cobrar | 167 | 0 | 0.0 % | 1 207 | 9 766 |
| Cancelar cita | 211 | 0 | 0.0 % | 1 483 | 7 906 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1337 | 0 (0 %) | 2 624 | 10 823 | 11 % |
| apoderado | 1088 | 0 (0 %) | 1 514 | 9 262 | 6 % |
| navegacion | 1337 | 0 (0 %) | 978 | 7 918 | 6 % |
| cumplimiento | 1337 | 0 (0 %) | 776 | 8 296 | 6 % |
| notificaciones | 1337 | 0 (0 %) | 813 | 8 133 | 6 % |
| cita | 1088 | 19 (2 %) | 1 282 | 9 211 | 6 % |
| avisos_de_cumplimiento | 1337 | 0 (0 %) | 736 | 7 962 | 6 % |
| mascota | 1088 | 0 (0 %) | 1 141 | 9 451 | 6 % |
| citas_del_mes | 830 | 0 (0 %) | 1 751 | 10 876 | 5 % |
| propietarios | 830 | 0 (0 %) | 1 542 | 9 319 | 5 % |
| privacidad | 1337 | 0 (0 %) | 676 | 6 621 | 5 % |
| empleados | 830 | 0 (0 %) | 1 346 | 8 909 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 13 | 1 010 | 0 % |
| 0.3 | 33 | 168 | 468 | 0 % |
| 0.8 | 63 | 302 | 333 | 0 % |
| 1.3 | 93 | 398 | 930 | 0 % |
| 1.8 | 123 | 488 | 457 | 0 % |
| 2.3 | 153 | 533 | 1 799 | 0 % |
| 2.8 | 183 | 556 | 3 177 | 0 % |
| 3.3 | 213 | 534 | 5 010 | 1 % |
| 3.8 | 243 | 577 | 6 120 | 2 % |
| 4.3 | 273 | 543 | 9 003 | 2 % |
| 4.8 | 303 | 589 | 8 111 | 1 % |
| 5.3 | 333 | 445 | 10 809 | 2 % |
| 5.8 | 350 | 582 | 15 112 | 3 % |
| 6.3 | 350 | 391 | 15 186 | 5 % |
| 6.8 | 350 | 716 | 7 036 | 3 % |
| 7.3 | 350 | 598 | 7 974 | 3 % |
| 7.8 | 350 | 910 | 1 926 | 0 % |
| 8.3 | 350 | 1049 | 1 275 | 0 % |
| 8.8 | 116 | 3 | 23 | 0 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
