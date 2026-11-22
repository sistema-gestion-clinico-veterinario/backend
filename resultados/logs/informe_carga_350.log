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
| Ritmo medio (peticiones/s) | 38.2 | 35.2 |
| Tiempo mediano (ms) | 939 | 1 102 |
| Tiempo p95 (ms) | 8 545 | 10 799 |
| Usuarios sin degradación | 155 | 169 |
| CPU del backend, media (% del límite) | 57 | 55 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 76 | 77 |
| Memoria pico (MB) | 1 337 | 1 343 |
| Consultas sobre consentimiento_datos (llamadas) | 8 156 | 7 746 |
| Tiempo total en PostgreSQL (s) | 34.6 | 0.5 |
| Tiempo medio por llamada (ms) | 4.2 | 0.1 |
| Filas de consentimiento_datos al terminar | 2 194 | 2 128 |
| Tamaño de la tabla con índices (MB) | 0.5 | 0.7 |
| Índices de la tabla | 1 | 2 |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 265 | 256 |
| Pausas completas | 35 | 32 |
| Pausas completas por "Allocation Failure" | 20 | 19 |
| Tiempo en pausas completas (s) | 16.0 | 13.7 |
| Tiempo en GC / vida de la JVM | 7.0 % | 6.8 % |
| Heap tras GC, máximo (MB) | 959 | 969 |
| Avisos de pool agotado | 0 | 0 |

### SIN índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

### CON índice

_Sin planes lentos de consentimiento_datos en el registro de PostgreSQL (auto_explain, muestreo del 10 %, umbral 300 ms)._

## SIN índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 6544; fallidas: 282.
- Acciones hechas: panel 1072, agenda 923, historia 732, reportes 90, recepcion 1097, atencion 576, cobro 199, cancelar 193; sin trabajo disponible: 886.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 13 % del tiempo.
- CPU de PostgreSQL: 76 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 337 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **155 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 15.3 s.
- Correo: el servidor de pruebas recibió 1097 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.3 %).
- GC: 265 pausas, 39.0 s en total (7 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 959 MB (tope -Xmx1g).
- Clases cargadas: 25562 en total, 161 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.3 | 87 | 88 | 17204 |
| 1.6 | 129 | 132 | 24618 |
| 2.6 | 132 | 135 | 25151 |
| 3.3 | 134 | 137 | 25379 |
| 4.0 | 134 | 137 | 25398 |
| 4.8 | 135 | 138 | 25474 |
| 5.8 | 136 | 139 | 25474 |
| 6.7 | 136 | 139 | 25475 |
| 7.3 | 136 | 139 | 25477 |
| 7.7 | 136 | 139 | 25518 |
| 8.0 | 136 | 139 | 25520 |
| 8.4 | 136 | 139 | 25562 |
| 8.7 | 136 | 139 | 25562 |
| 9.1 | 136 | 139 | 25562 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 158 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1422 | 598.7 | 421.04 | 29 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1422 | 478.4 | 336.46 | 23 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1422 | 262.1 | 184.29 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 180 | 143.0 | 794.49 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1422 | 112.5 | 79.1 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1422 | 107.8 | 75.79 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1422 | 105.5 | 74.19 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 923 | 64.5 | 69.91 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 732 | 53.0 | 72.45 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 180 | 41.6 | 230.86 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1097 | 1097 | coincide |
| Mascotas | 1097 | 1097 | coincide |
| Citas agendadas | 1081 | 1081 | coincide |
| Diagnósticos | 310 | 310 | coincide |
| Recetas | 310 | 310 | coincide |
| Pagos | 199 | 199 | coincide |
| Citas completadas | 310 | 310 | coincide |
| Citas canceladas | 193 | 193 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1097 | 0 | 0.0 % | 1 167 | 9 854 |
| Registrar mascota | 1097 | 0 | 0.0 % | 823 | 8 538 |
| Agendar cita | 1081 | 16 | 1.5 % | 928 | 8 561 |
| Recepción (llegada) | 576 | 0 | 0.0 % | 1 029 | 9 285 |
| Iniciar atención | 310 | 266 | 46.2 % | 1 157 | 10 213 |
| Leer la consulta | 310 | 0 | 0.0 % | 369 | 5 504 |
| Registrar diagnóstico | 310 | 0 | 0.0 % | 278 | 6 424 |
| Registrar receta | 310 | 0 | 0.0 % | 361 | 5 812 |
| Actualizar mascota | 310 | 0 | 0.0 % | 359 | 5 285 |
| Cerrar la consulta | 310 | 0 | 0.0 % | 445 | 6 364 |
| Caja: crear punto de cobro | 147 | 0 | 0.0 % | 912 | 6 702 |
| Caja: vincular equipo | 147 | 0 | 0.0 % | 717 | 5 852 |
| Caja: abrir la caja | 147 | 0 | 0.0 % | 566 | 8 886 |
| Cobrar | 199 | 0 | 0.0 % | 1 012 | 6 551 |
| Cancelar cita | 193 | 0 | 0.0 % | 1 070 | 8 864 |

**Endpoints que más tiempo acumulan, SIN índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1422 | 0 (0 %) | 2 194 | 10 660 | 10 % |
| apoderado | 1097 | 0 (0 %) | 1 167 | 9 854 | 6 % |
| navegacion | 1422 | 0 (0 %) | 835 | 7 654 | 6 % |
| citas_del_mes | 923 | 0 (0 %) | 1 472 | 10 594 | 6 % |
| avisos_de_cumplimiento | 1422 | 0 (0 %) | 661 | 7 380 | 6 % |
| cumplimiento | 1422 | 0 (0 %) | 583 | 7 803 | 6 % |
| cita | 1097 | 16 (1 %) | 928 | 8 561 | 5 % |
| notificaciones | 1422 | 0 (0 %) | 582 | 7 246 | 5 % |
| mascota | 1097 | 0 (0 %) | 823 | 8 538 | 5 % |
| propietarios | 923 | 0 (0 %) | 1 333 | 9 580 | 5 % |
| empleados | 923 | 0 (0 %) | 1 039 | 8 841 | 5 % |
| privacidad | 1422 | 0 (0 %) | 565 | 5 937 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 42 | 941 | 0 % |
| 0.3 | 35 | 181 | 428 | 1 % |
| 0.8 | 65 | 325 | 485 | 0 % |
| 1.4 | 95 | 415 | 456 | 0 % |
| 1.9 | 125 | 545 | 440 | 0 % |
| 2.4 | 155 | 657 | 597 | 0 % |
| 2.9 | 185 | 717 | 2 290 | 0 % |
| 3.4 | 215 | 584 | 4 857 | 1 % |
| 3.9 | 245 | 690 | 3 548 | 2 % |
| 4.3 | 275 | 641 | 4 892 | 2 % |
| 4.8 | 305 | 571 | 7 888 | 2 % |
| 5.3 | 335 | 450 | 10 883 | 1 % |
| 5.8 | 350 | 466 | 15 275 | 3 % |
| 6.3 | 350 | 483 | 11 936 | 4 % |
| 6.8 | 350 | 1119 | 3 776 | 1 % |
| 7.3 | 350 | 890 | 5 706 | 2 % |
| 7.8 | 350 | 922 | 4 642 | 3 % |
| 8.3 | 348 | 892 | 1 521 | 1 % |

</details>

## CON índice: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5939; fallidas: 263.
- Acciones hechas: panel 1000, agenda 789, historia 664, reportes 88, recepcion 1064, atencion 499, cobro 163, cancelar 227; sin trabajo disponible: 773.
- CPU del backend: 55 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 6 % del tiempo.
- CPU de PostgreSQL: 77 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 343 MB (66 % de 2048 MB).
- Los fallos fueron: 1 % sin respuesta (tiempo agotado), 99 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **169 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 20.0 s.
- Correo: el servidor de pruebas recibió 1064 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 256 pausas, 37.4 s en total (7 % del tiempo de vida de la JVM); 32 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 969 MB (tope -Xmx1g).
- Clases cargadas: 25530 en total, 129 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.3 | 86 | 87 | 17104 |
| 1.3 | 128 | 130 | 24479 |
| 2.5 | 132 | 135 | 25120 |
| 3.3 | 134 | 137 | 25371 |
| 3.8 | 134 | 137 | 25376 |
| 4.6 | 135 | 138 | 25401 |
| 5.1 | 135 | 138 | 25443 |
| 5.8 | 136 | 139 | 25467 |
| 6.7 | 136 | 139 | 25469 |
| 7.1 | 136 | 139 | 25471 |
| 7.7 | 136 | 139 | 25472 |
| 8.1 | 136 | 139 | 25488 |
| 8.4 | 136 | 139 | 25528 |
| 8.8 | 136 | 139 | 25528 |

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
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1350 | 729.4 | 540.27 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1350 | 429.1 | 317.84 | 20 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1350 | 255.8 | 189.45 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 176 | 149.7 | 850.81 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1350 | 113.9 | 84.35 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1350 | 112.9 | 83.61 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1350 | 105.8 | 78.38 | 5 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 664 | 52.1 | 78.5 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 789 | 46.8 | 59.25 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 176 | 41.5 | 235.88 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1064 | 1064 | coincide |
| Mascotas | 1064 | 1064 | coincide |
| Citas agendadas | 1050 | 1050 | coincide |
| Diagnósticos | 250 | 250 | coincide |
| Recetas | 250 | 250 | coincide |
| Pagos | 163 | 163 | coincide |
| Citas completadas | 250 | 250 | coincide |
| Citas canceladas | 227 | 227 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1064 | 0 | 0.0 % | 1 304 | 11 832 |
| Registrar mascota | 1064 | 0 | 0.0 % | 1 225 | 10 285 |
| Agendar cita | 1050 | 14 | 1.3 % | 1 168 | 11 940 |
| Recepción (llegada) | 499 | 0 | 0.0 % | 1 408 | 12 730 |
| Iniciar atención | 250 | 249 | 49.9 % | 1 486 | 13 645 |
| Leer la consulta | 250 | 0 | 0.0 % | 351 | 8 085 |
| Registrar diagnóstico | 250 | 0 | 0.0 % | 321 | 9 283 |
| Registrar receta | 250 | 0 | 0.0 % | 301 | 6 551 |
| Actualizar mascota | 250 | 0 | 0.0 % | 266 | 8 710 |
| Cerrar la consulta | 250 | 0 | 0.0 % | 411 | 7 727 |
| Caja: crear punto de cobro | 124 | 0 | 0.0 % | 932 | 9 619 |
| Caja: vincular equipo | 124 | 0 | 0.0 % | 861 | 8 943 |
| Caja: abrir la caja | 124 | 0 | 0.0 % | 743 | 11 646 |
| Cobrar | 163 | 0 | 0.0 % | 1 127 | 12 172 |
| Cancelar cita | 227 | 0 | 0.0 % | 1 218 | 11 038 |

**Endpoints que más tiempo acumulan, CON índice:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1350 | 2 (0 %) | 2 460 | 12 593 | 10 % |
| navegacion | 1350 | 0 (0 %) | 971 | 10 685 | 7 % |
| avisos_de_cumplimiento | 1350 | 0 (0 %) | 881 | 9 777 | 6 % |
| apoderado | 1064 | 0 (0 %) | 1 304 | 11 832 | 6 % |
| cita | 1064 | 14 (1 %) | 1 168 | 11 940 | 6 % |
| cumplimiento | 1350 | 0 (0 %) | 769 | 9 955 | 6 % |
| notificaciones | 1350 | 0 (0 %) | 788 | 9 418 | 6 % |
| mascota | 1064 | 0 (0 %) | 1 225 | 10 285 | 5 % |
| citas_del_mes | 789 | 0 (0 %) | 1 393 | 11 694 | 5 % |
| privacidad | 1350 | 0 (0 %) | 681 | 7 894 | 5 % |
| propietarios | 789 | 0 (0 %) | 1 529 | 10 843 | 4 % |
| empleados | 789 | 0 (0 %) | 1 177 | 10 779 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 28 | 944 | 0 % |
| 0.3 | 34 | 174 | 421 | 1 % |
| 0.8 | 64 | 297 | 345 | 0 % |
| 1.3 | 94 | 398 | 368 | 0 % |
| 1.8 | 124 | 539 | 372 | 0 % |
| 2.3 | 154 | 611 | 541 | 0 % |
| 2.8 | 184 | 653 | 2 189 | 0 % |
| 3.3 | 214 | 658 | 2 915 | 1 % |
| 3.8 | 244 | 667 | 3 644 | 1 % |
| 4.3 | 274 | 534 | 7 143 | 2 % |
| 4.8 | 304 | 405 | 9 133 | 2 % |
| 5.3 | 334 | 449 | 19 990 | 2 % |
| 5.8 | 350 | 445 | 15 181 | 1 % |
| 6.3 | 350 | 374 | 19 422 | 2 % |
| 6.8 | 350 | 404 | 8 301 | 1 % |
| 7.3 | 350 | 717 | 6 433 | 2 % |
| 7.8 | 350 | 987 | 4 602 | 2 % |
| 8.3 | 350 | 996 | 1 263 | 1 % |

</details>

> Cada corrida parte de la misma base (copiada de una plantilla), así que el orden y los datos acumulados no influyen. Las dos variantes usan -Xms = -Xmx. Una corrida por variante y nivel.
