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
| Errores (%) | 1.0 % | 1.3 % |
| Ritmo medio (peticiones/s) | 34.0 | 30.7 |
| Tiempo mediano (ms) | 718 | 1 199 |
| Tiempo p95 (ms) | 4 117 | 5 261 |
| Usuarios sin degradación | 162 | 154 |
| CPU del backend, media (% del límite) | 54 | 51 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 76 | 74 |
| Memoria pico (MB) | 1 298 | 1 359 |
| Consultas sobre consentimiento_datos (llamadas) | 5 875 | 5 443 |
| Tiempo total en PostgreSQL (s) | 12.9 | 0.3 |
| Tiempo medio por llamada (ms) | 2.2 | 0.1 |
| Filas de consentimiento_datos al terminar | 1 564 | 1 464 |
| Tamaño de la tabla con índices (MB) | 0.5 | 0.6 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 214 | 165 |
| Pausas completas | 29 | 15 |
| Pausas completas por "Allocation Failure" | 16 | 9 |
| Tiempo en pausas completas (s) | 11.0 | 6.1 |
| Tiempo en GC / vida de la JVM | 6.7 % | 4.3 % |
| Heap tras GC, máximo (MB) | 933 | 955 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4754; fallidas: 149.
- Acciones hechas: panel 793, agenda 660, historia 527, reportes 75, recepcion 782, atencion 375, cobro 147, cancelar 152; sin trabajo disponible: 611.
- CPU del backend: 54 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 76 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 298 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **162 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.5 s.
- Correo: el servidor de pruebas recibió 782 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 214 pausas, 30.1 s en total (7 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 933 MB (tope -Xmx1g).
- Clases cargadas: 25534 en total, 157 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4575 |
| 0.3 | 86 | 87 | 17075 |
| 1.1 | 126 | 128 | 24044 |
| 2.2 | 132 | 134 | 24969 |
| 2.7 | 133 | 136 | 25193 |
| 3.1 | 133 | 136 | 25313 |
| 3.7 | 134 | 137 | 25377 |
| 4.2 | 134 | 137 | 25382 |
| 4.7 | 135 | 138 | 25406 |
| 5.1 | 135 | 138 | 25406 |
| 6.0 | 136 | 139 | 25472 |
| 6.4 | 136 | 139 | 25474 |
| 6.8 | 136 | 139 | 25475 |
| 7.1 | 136 | 139 | 25532 |
| 7.4 | 136 | 139 | 25534 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 148 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 153 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `java.util.concurrent.locks` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1043 | 685.3 | 657.03 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1043 | 385.0 | 369.15 | 21 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1043 | 191.9 | 184.03 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 150 | 123.3 | 822.29 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1043 | 82.1 | 78.7 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1043 | 76.9 | 73.76 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1043 | 75.6 | 72.51 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 660 | 41.9 | 63.46 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 527 | 41.3 | 78.43 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 150 | 35.1 | 234.02 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 782 | 782 | coincide |
| Mascotas | 782 | 782 | coincide |
| Citas agendadas | 770 | 770 | coincide |
| Diagnósticos | 238 | 238 | coincide |
| Recetas | 238 | 238 | coincide |
| Pagos | 147 | 147 | coincide |
| Citas completadas | 238 | 238 | coincide |
| Citas canceladas | 152 | 152 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 782 | 0 | 0.0 % | 890 | 3 853 |
| Registrar mascota | 782 | 0 | 0.0 % | 715 | 3 900 |
| Agendar cita | 770 | 12 | 1.5 % | 708 | 3 465 |
| Recepción (llegada) | 375 | 0 | 0.0 % | 1 090 | 3 666 |
| Iniciar atención | 238 | 137 | 36.5 % | 1 032 | 4 163 |
| Leer la consulta | 238 | 0 | 0.0 % | 435 | 3 776 |
| Registrar diagnóstico | 238 | 0 | 0.0 % | 528 | 3 278 |
| Registrar receta | 238 | 0 | 0.0 % | 410 | 4 084 |
| Actualizar mascota | 238 | 0 | 0.0 % | 443 | 3 128 |
| Cerrar la consulta | 238 | 0 | 0.0 % | 450 | 3 648 |
| Caja: crear punto de cobro | 106 | 0 | 0.0 % | 1 114 | 3 053 |
| Caja: vincular equipo | 106 | 0 | 0.0 % | 782 | 4 883 |
| Caja: abrir la caja | 106 | 0 | 0.0 % | 755 | 3 563 |
| Cobrar | 147 | 0 | 0.0 % | 1 037 | 4 315 |
| Cancelar cita | 152 | 0 | 0.0 % | 1 002 | 3 649 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1043 | 0 (0 %) | 2 264 | 6 440 | 15 % |
| navegacion | 1043 | 0 (0 %) | 583 | 3 339 | 6 % |
| apoderado | 782 | 0 (0 %) | 890 | 3 853 | 5 % |
| cumplimiento | 1043 | 0 (0 %) | 370 | 3 431 | 5 % |
| citas_del_mes | 660 | 0 (0 %) | 1 054 | 4 211 | 5 % |
| propietarios | 660 | 0 (0 %) | 967 | 4 141 | 5 % |
| avisos_de_cumplimiento | 1043 | 0 (0 %) | 385 | 3 192 | 5 % |
| mascota | 782 | 0 (0 %) | 715 | 3 900 | 5 % |
| notificaciones | 1043 | 0 (0 %) | 295 | 3 193 | 5 % |
| empleados | 660 | 0 (0 %) | 894 | 4 227 | 5 % |
| cita | 782 | 12 (2 %) | 708 | 3 465 | 5 % |
| privacidad | 1043 | 0 (0 %) | 355 | 2 851 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 85 | 527 | 0 % |
| 0.5 | 42 | 226 | 324 | 0 % |
| 1.0 | 72 | 298 | 468 | 0 % |
| 1.5 | 102 | 503 | 320 | 0 % |
| 2.0 | 132 | 514 | 777 | 0 % |
| 2.5 | 162 | 627 | 1 095 | 0 % |
| 3.0 | 192 | 629 | 4 117 | 1 % |
| 3.5 | 222 | 612 | 3 771 | 1 % |
| 4.0 | 250 | 657 | 5 127 | 1 % |
| 4.5 | 250 | 535 | 7 485 | 2 % |
| 5.0 | 250 | 602 | 4 252 | 2 % |
| 5.5 | 250 | 646 | 5 090 | 3 % |
| 6.0 | 250 | 849 | 1 727 | 1 % |
| 6.5 | 250 | 613 | 3 294 | 1 % |
| 7.0 | 183 | 172 | 98 | 0 % |

</details>

## CON índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4270; fallidas: 182.
- Acciones hechas: panel 734, agenda 546, historia 464, reportes 51, recepcion 732, atencion 363, cobro 129, cancelar 120; sin trabajo disponible: 570.
- CPU del backend: 51 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 74 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 359 MB (66 % de 2048 MB).
- Los fallos fueron: 1 % errores 5xx del servidor, 99 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **154 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.4 s.
- Correo: el servidor de pruebas recibió 732 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 165 pausas, 19.6 s en total (4 % del tiempo de vida de la JVM); 15 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 955 MB (tope -Xmx1g).
- Clases cargadas: 25542 en total, 166 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4575 |
| 0.3 | 81 | 81 | 16113 |
| 0.4 | 96 | 97 | 18394 |
| 1.6 | 129 | 131 | 24616 |
| 2.5 | 131 | 134 | 25069 |
| 3.1 | 132 | 135 | 25307 |
| 3.4 | 133 | 136 | 25345 |
| 3.9 | 134 | 137 | 25389 |
| 4.4 | 134 | 137 | 25401 |
| 4.8 | 134 | 137 | 25406 |
| 5.3 | 135 | 138 | 25426 |
| 5.7 | 135 | 138 | 25499 |
| 6.1 | 135 | 138 | 25500 |
| 6.6 | 136 | 138 | 25502 |
| 7.4 | 136 | 139 | 25502 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 128 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 133 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.dao.support` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.postgresql.core.EncodingPredictor` | 1 |
| `org.postgresql.util.ServerErrorMessage` | 1 |
| `org.postgresql.util.GT` | 1 |
| `java.text.MessageFormat` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 984 | 718.8 | 730.44 | 36 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 984 | 615.0 | 624.99 | 31 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 984 | 187.4 | 190.49 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 102 | 90.3 | 884.98 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 984 | 78.4 | 79.63 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 984 | 73.6 | 74.75 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 984 | 71.8 | 72.94 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 546 | 45.3 | 83.01 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 464 | 38.3 | 82.55 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 102 | 25.3 | 248.27 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 732 | 732 | coincide |
| Mascotas | 732 | 732 | coincide |
| Citas agendadas | 718 | 718 | coincide |
| Diagnósticos | 195 | 195 | coincide |
| Recetas | 195 | 195 | coincide |
| Pagos | 129 | 129 | coincide |
| Citas completadas | 195 | 195 | coincide |
| Citas canceladas | 120 | 120 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 732 | 0 | 0.0 % | 1 392 | 4 884 |
| Registrar mascota | 732 | 0 | 0.0 % | 1 198 | 4 930 |
| Agendar cita | 718 | 14 | 1.9 % | 1 343 | 4 991 |
| Recepción (llegada) | 363 | 0 | 0.0 % | 1 768 | 5 326 |
| Iniciar atención | 195 | 168 | 46.3 % | 1 768 | 5 578 |
| Leer la consulta | 195 | 0 | 0.0 % | 767 | 4 799 |
| Registrar diagnóstico | 195 | 0 | 0.0 % | 850 | 4 969 |
| Registrar receta | 195 | 0 | 0.0 % | 842 | 4 929 |
| Actualizar mascota | 195 | 0 | 0.0 % | 933 | 5 202 |
| Cerrar la consulta | 195 | 0 | 0.0 % | 786 | 4 435 |
| Caja: crear punto de cobro | 102 | 0 | 0.0 % | 1 297 | 4 286 |
| Caja: vincular equipo | 102 | 0 | 0.0 % | 862 | 4 877 |
| Caja: abrir la caja | 102 | 0 | 0.0 % | 1 439 | 4 535 |
| Cobrar | 129 | 0 | 0.0 % | 1 776 | 4 628 |
| Cancelar cita | 120 | 0 | 0.0 % | 1 829 | 5 231 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 984 | 0 (0 %) | 2 963 | 8 107 | 14 % |
| navegacion | 984 | 0 (0 %) | 888 | 4 251 | 6 % |
| cita | 732 | 14 (2 %) | 1 343 | 4 991 | 5 % |
| cumplimiento | 984 | 0 (0 %) | 769 | 4 520 | 5 % |
| notificaciones | 984 | 0 (0 %) | 708 | 4 300 | 5 % |
| apoderado | 732 | 0 (0 %) | 1 392 | 4 884 | 5 % |
| avisos_de_cumplimiento | 984 | 0 (0 %) | 694 | 4 450 | 5 % |
| mascota | 732 | 0 (0 %) | 1 198 | 4 930 | 5 % |
| propietarios | 546 | 0 (0 %) | 1 778 | 5 753 | 5 % |
| citas_del_mes | 546 | 0 (0 %) | 1 752 | 5 440 | 5 % |
| privacidad | 984 | 0 (0 %) | 636 | 3 477 | 4 % |
| empleados | 546 | 0 (0 %) | 1 437 | 5 628 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 34 | 943 | 0 % |
| 0.3 | 34 | 186 | 453 | 0 % |
| 0.8 | 64 | 290 | 355 | 0 % |
| 1.3 | 94 | 429 | 401 | 0 % |
| 1.8 | 124 | 549 | 298 | 0 % |
| 2.3 | 154 | 569 | 1 955 | 1 % |
| 2.8 | 184 | 657 | 2 685 | 1 % |
| 3.3 | 214 | 493 | 4 486 | 1 % |
| 3.8 | 244 | 692 | 3 994 | 1 % |
| 4.3 | 250 | 576 | 5 905 | 2 % |
| 4.8 | 250 | 512 | 6 064 | 4 % |
| 5.3 | 250 | 484 | 5 998 | 3 % |
| 5.8 | 250 | 539 | 6 868 | 1 % |
| 6.3 | 250 | 515 | 7 139 | 2 % |
| 6.8 | 226 | 402 | 1 637 | 2 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
