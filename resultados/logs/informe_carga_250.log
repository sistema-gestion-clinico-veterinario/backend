# Fase 3c: índice en consentimiento_datos: 250 usuarios (1 CPU, 2 GB)

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
| Veredicto | DEGRADADO | SATURADO (muy lento, casi sin errores) |
| Errores (%) | 1.3 % | 1.2 % |
| Ritmo medio (peticiones/s) | 32.9 | 31.4 |
| Tiempo mediano (ms) | 785 | 853 |
| Tiempo p95 (ms) | 4 370 | 5 139 |
| Usuarios sin degradación | 165 | 158 |
| CPU del backend, media (% del límite) | 55 | 56 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 75 | 76 |
| Memoria pico (MB) | 1 328 | 1 353 |
| Consultas sobre consentimiento_datos (llamadas) | 5 746 | 5 540 |
| Tiempo total en PostgreSQL (s) | 11.8 | 0.4 |
| Tiempo medio por llamada (ms) | 2.0 | 0.1 |
| Filas de consentimiento_datos al terminar | 1 618 | 1 526 |
| Tamaño de la tabla con índices (MB) | 0.5 | 0.6 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 225 | 211 |
| Pausas completas | 33 | 29 |
| Pausas completas por "Allocation Failure" | 19 | 16 |
| Tiempo en pausas completas (s) | 14.7 | 12.6 |
| Tiempo en GC / vida de la JVM | 7.9 % | 7.1 % |
| Heap tras GC, máximo (MB) | 935 | 984 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4971; fallidas: 189.
- Acciones hechas: panel 711, agenda 587, historia 519, reportes 83, recepcion 809, atencion 420, cobro 165, cancelar 135; sin trabajo disponible: 630.
- CPU del backend: 55 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 75 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 328 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **165 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.1 s.
- Correo: el servidor de pruebas recibió 809 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.3 %).
- GC: 225 pausas, 35.7 s en total (8 % del tiempo de vida de la JVM); 33 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 935 MB (tope -Xmx1g).
- Clases cargadas: 25538 en total, 141 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4508 |
| 0.3 | 86 | 87 | 17085 |
| 1.3 | 127 | 130 | 24314 |
| 2.2 | 132 | 134 | 24996 |
| 2.9 | 133 | 136 | 25314 |
| 3.3 | 134 | 137 | 25382 |
| 3.9 | 134 | 137 | 25401 |
| 4.6 | 135 | 138 | 25410 |
| 5.0 | 135 | 138 | 25460 |
| 5.4 | 135 | 138 | 25476 |
| 5.7 | 136 | 138 | 25476 |
| 6.2 | 136 | 139 | 25479 |
| 6.6 | 136 | 139 | 25480 |
| 6.9 | 136 | 139 | 25535 |
| 7.2 | 136 | 139 | 25535 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 133 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 138 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 961 | 645.2 | 671.38 | 37 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 961 | 366.1 | 380.93 | 21 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 961 | 184.8 | 192.25 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 166 | 148.9 | 896.72 | 9 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 961 | 74.7 | 77.74 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 961 | 70.1 | 72.93 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 961 | 68.6 | 71.35 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 519 | 40.4 | 77.87 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 166 | 40.0 | 241.1 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 587 | 34.7 | 59.19 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 809 | 809 | coincide |
| Mascotas | 809 | 809 | coincide |
| Citas agendadas | 794 | 794 | coincide |
| Diagnósticos | 246 | 246 | coincide |
| Recetas | 246 | 246 | coincide |
| Pagos | 165 | 165 | coincide |
| Citas completadas | 246 | 246 | coincide |
| Citas canceladas | 135 | 135 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 809 | 0 | 0.0 % | 976 | 4 426 |
| Registrar mascota | 809 | 0 | 0.0 % | 660 | 3 958 |
| Agendar cita | 794 | 15 | 1.9 % | 751 | 3 966 |
| Recepción (llegada) | 420 | 0 | 0.0 % | 1 219 | 4 574 |
| Iniciar atención | 246 | 174 | 41.4 % | 1 197 | 4 882 |
| Leer la consulta | 246 | 0 | 0.0 % | 520 | 3 647 |
| Registrar diagnóstico | 246 | 0 | 0.0 % | 633 | 3 604 |
| Registrar receta | 246 | 0 | 0.0 % | 567 | 4 001 |
| Actualizar mascota | 246 | 0 | 0.0 % | 554 | 3 666 |
| Cerrar la consulta | 246 | 0 | 0.0 % | 502 | 4 306 |
| Caja: crear punto de cobro | 121 | 0 | 0.0 % | 821 | 3 630 |
| Caja: vincular equipo | 121 | 0 | 0.0 % | 516 | 3 233 |
| Caja: abrir la caja | 121 | 0 | 0.0 % | 525 | 4 220 |
| Cobrar | 165 | 0 | 0.0 % | 727 | 4 011 |
| Cancelar cita | 135 | 0 | 0.0 % | 1 271 | 4 917 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 961 | 0 (0 %) | 2 482 | 6 300 | 13 % |
| apoderado | 809 | 0 (0 %) | 976 | 4 426 | 6 % |
| navegacion | 961 | 0 (0 %) | 583 | 3 913 | 6 % |
| cita | 809 | 15 (2 %) | 751 | 3 966 | 5 % |
| citas_del_mes | 587 | 0 (0 %) | 1 202 | 4 919 | 5 % |
| cumplimiento | 961 | 0 (0 %) | 420 | 3 619 | 5 % |
| mascota | 809 | 0 (0 %) | 660 | 3 958 | 5 % |
| avisos_de_cumplimiento | 961 | 0 (0 %) | 428 | 3 378 | 5 % |
| notificaciones | 961 | 0 (0 %) | 416 | 3 291 | 4 % |
| propietarios | 587 | 0 (0 %) | 958 | 4 261 | 4 % |
| empleados | 587 | 0 (0 %) | 892 | 4 327 | 4 % |
| lista_de_historias | 519 | 0 (0 %) | 1 108 | 4 626 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 15 | 117 | 432 | 0 % |
| 0.5 | 45 | 229 | 461 | 0 % |
| 1.0 | 75 | 321 | 378 | 0 % |
| 1.5 | 105 | 490 | 302 | 0 % |
| 2.0 | 135 | 599 | 351 | 0 % |
| 2.5 | 165 | 590 | 1 781 | 0 % |
| 3.0 | 195 | 460 | 4 262 | 1 % |
| 3.5 | 225 | 498 | 5 797 | 2 % |
| 4.0 | 250 | 673 | 4 021 | 1 % |
| 4.5 | 250 | 685 | 3 299 | 2 % |
| 5.0 | 250 | 577 | 5 367 | 3 % |
| 5.5 | 250 | 512 | 5 672 | 4 % |
| 6.0 | 250 | 635 | 2 406 | 1 % |
| 6.5 | 250 | 580 | 2 989 | 1 % |
| 7.0 | 148 | 74 | 30 | 0 % |

</details>

## CON índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4403; fallidas: 169.
- Acciones hechas: panel 688, agenda 611, historia 533, reportes 76, recepcion 763, atencion 357, cobro 127, cancelar 139; sin trabajo disponible: 555.
- CPU del backend: 56 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 76 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 353 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **158 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.9 s.
- Correo: el servidor de pruebas recibió 763 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.2 %).
- GC: 211 pausas, 32.6 s en total (7 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 984 MB (tope -Xmx1g).
- Clases cargadas: 25489 en total, 116 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4514 |
| 0.3 | 82 | 83 | 16409 |
| 0.9 | 124 | 126 | 23843 |
| 2.1 | 131 | 134 | 24915 |
| 2.6 | 132 | 135 | 25121 |
| 3.0 | 133 | 136 | 25307 |
| 3.6 | 134 | 137 | 25372 |
| 4.1 | 134 | 137 | 25376 |
| 4.4 | 134 | 137 | 25397 |
| 5.0 | 135 | 138 | 25400 |
| 5.4 | 135 | 138 | 25405 |
| 5.9 | 135 | 138 | 25467 |
| 6.4 | 136 | 138 | 25468 |
| 6.9 | 136 | 139 | 25471 |
| 7.3 | 136 | 139 | 25488 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 109 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 113 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 938 | 620.2 | 661.23 | 34 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 938 | 502.0 | 535.2 | 28 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 938 | 183.8 | 195.96 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 152 | 123.3 | 811.1 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 938 | 73.9 | 78.83 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 938 | 69.0 | 73.61 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 938 | 66.9 | 71.32 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 611 | 50.0 | 81.76 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 533 | 43.0 | 80.69 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 152 | 34.7 | 228.43 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 763 | 763 | coincide |
| Mascotas | 763 | 763 | coincide |
| Citas agendadas | 751 | 751 | coincide |
| Diagnósticos | 200 | 200 | coincide |
| Recetas | 200 | 200 | coincide |
| Pagos | 127 | 127 | coincide |
| Citas completadas | 200 | 200 | coincide |
| Citas canceladas | 139 | 139 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 763 | 0 | 0.0 % | 1 246 | 5 217 |
| Registrar mascota | 763 | 0 | 0.0 % | 961 | 4 660 |
| Agendar cita | 751 | 12 | 1.6 % | 980 | 5 237 |
| Recepción (llegada) | 357 | 0 | 0.0 % | 1 309 | 5 270 |
| Iniciar atención | 200 | 157 | 44.0 % | 1 204 | 5 031 |
| Leer la consulta | 200 | 0 | 0.0 % | 429 | 3 828 |
| Registrar diagnóstico | 200 | 0 | 0.0 % | 363 | 4 725 |
| Registrar receta | 200 | 0 | 0.0 % | 536 | 5 330 |
| Actualizar mascota | 200 | 0 | 0.0 % | 603 | 4 477 |
| Cerrar la consulta | 200 | 0 | 0.0 % | 499 | 4 137 |
| Caja: crear punto de cobro | 101 | 0 | 0.0 % | 1 030 | 5 165 |
| Caja: vincular equipo | 101 | 0 | 0.0 % | 591 | 5 121 |
| Caja: abrir la caja | 101 | 0 | 0.0 % | 849 | 4 865 |
| Cobrar | 127 | 0 | 0.0 % | 1 042 | 6 122 |
| Cancelar cita | 139 | 0 | 0.0 % | 1 464 | 5 357 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 938 | 0 (0 %) | 2 784 | 7 390 | 13 % |
| apoderado | 763 | 0 (0 %) | 1 246 | 5 217 | 6 % |
| navegacion | 938 | 0 (0 %) | 600 | 4 721 | 6 % |
| cita | 763 | 12 (2 %) | 980 | 5 237 | 6 % |
| citas_del_mes | 611 | 0 (0 %) | 1 285 | 5 423 | 5 % |
| mascota | 763 | 0 (0 %) | 961 | 4 660 | 5 % |
| lista_de_historias | 533 | 0 (0 %) | 1 239 | 5 359 | 5 % |
| notificaciones | 938 | 0 (0 %) | 424 | 4 009 | 5 % |
| cumplimiento | 938 | 0 (0 %) | 441 | 3 998 | 5 % |
| propietarios | 611 | 0 (0 %) | 1 019 | 5 131 | 5 % |
| avisos_de_cumplimiento | 938 | 0 (0 %) | 444 | 3 934 | 4 % |
| empleados | 611 | 0 (0 %) | 895 | 4 688 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 63 | 627 | 0 % |
| 0.4 | 38 | 173 | 328 | 1 % |
| 0.9 | 68 | 289 | 307 | 0 % |
| 1.4 | 98 | 390 | 369 | 0 % |
| 1.9 | 128 | 543 | 519 | 0 % |
| 2.4 | 158 | 644 | 1 917 | 1 % |
| 2.9 | 188 | 560 | 3 813 | 1 % |
| 3.4 | 218 | 504 | 4 278 | 2 % |
| 3.9 | 248 | 530 | 6 329 | 1 % |
| 4.4 | 250 | 437 | 7 851 | 1 % |
| 4.9 | 250 | 625 | 5 235 | 3 % |
| 5.4 | 250 | 527 | 6 708 | 2 % |
| 5.9 | 250 | 637 | 4 149 | 3 % |
| 6.4 | 250 | 636 | 4 651 | 2 % |
| 6.9 | 204 | 301 | 262 | 0 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
