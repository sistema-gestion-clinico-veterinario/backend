# Fase 3d: panel optimizado: 350 usuarios (1 CPU, 2 GB)

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
| Errores (%) | 1.3 % | 0.5 % |
| Ritmo medio (peticiones/s) | 36.6 | 56.0 |
| Tiempo mediano (ms) | 941 | 54 |
| Tiempo p95 (ms) | 10 128 | 1 259 |
| Usuarios sin degradación | 166 | 308 |
| CPU del backend, media (% del límite) | 56 | 73 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 79 | 46 |
| Memoria pico (MB) | 1 360 | 1 353 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 61.9 | 52.6 |
| Tiempo SQL por carga del panel (ms, sin carga) | 166.7 | 24.3 |
| Panel sin carga: mediana de 19 llamadas (ms) | 291 | 115 |
| Panel sin carga: primera llamada (ms) | 528 | 472 |
| Panel bajo carga: mediana (ms) | 2 366 | 183 |
| Panel bajo carga: p95 (ms) | 12 368 | 1 196 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 259 | 400 |
| Pausas completas | 34 | 53 |
| Pausas completas por "Allocation Failure" | 21 | 40 |
| Tiempo en pausas completas (s) | 16.1 | 29.2 |
| Tiempo en GC / vida de la JVM | 6.8 % | 12.0 % |
| Heap tras GC, máximo (MB) | 984 | 975 |
| Avisos de pool agotado | 0 | 0 |

## Código anterior: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 6149; fallidas: 257.
- Acciones hechas: panel 1057, agenda 827, historia 711, reportes 87, recepcion 1078, atencion 516, cobro 167, cancelar 198; sin trabajo disponible: 758.
- CPU del backend: 56 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 79 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 360 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **166 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 16.1 s.
- Correo: el servidor de pruebas recibió 1078 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.3 %).
- GC: 259 pausas, 38.2 s en total (7 % del tiempo de vida de la JVM); 34 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 984 MB (tope -Xmx1g).
- Clases cargadas: 25554 en total, 162 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.3 | 87 | 88 | 17211 |
| 1.6 | 129 | 132 | 24560 |
| 2.6 | 132 | 135 | 25055 |
| 3.2 | 133 | 136 | 25292 |
| 4.0 | 134 | 137 | 25372 |
| 4.8 | 135 | 138 | 25398 |
| 5.7 | 135 | 138 | 25408 |
| 6.4 | 136 | 139 | 25466 |
| 7.4 | 136 | 139 | 25468 |
| 7.7 | 136 | 139 | 25468 |
| 8.2 | 136 | 139 | 25471 |
| 8.5 | 136 | 139 | 25487 |
| 8.9 | 136 | 139 | 25528 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 152 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 159 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1407 | 661.8 | 470.35 | 31 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1407 | 537.8 | 382.24 | 25 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1407 | 275.0 | 195.46 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 174 | 134.4 | 772.38 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 1407 | 111.5 | 79.25 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1407 | 108.3 | 76.98 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 1407 | 105.1 | 74.67 | 5 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 711 | 54.1 | 76.16 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 174 | 39.0 | 224.4 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2234 | 14.6 | 6.52 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1078 | 1078 | coincide |
| Mascotas | 1078 | 1078 | coincide |
| Citas agendadas | 1060 | 1060 | coincide |
| Diagnósticos | 277 | 277 | coincide |
| Recetas | 277 | 277 | coincide |
| Pagos | 167 | 167 | coincide |
| Citas completadas | 277 | 277 | coincide |
| Citas canceladas | 198 | 198 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1078 | 0 | 0.0 % | 1 210 | 10 956 |
| Registrar mascota | 1078 | 0 | 0.0 % | 1 045 | 10 651 |
| Agendar cita | 1060 | 18 | 1.7 % | 943 | 9 845 |
| Recepción (llegada) | 516 | 0 | 0.0 % | 1 257 | 12 032 |
| Iniciar atención | 277 | 239 | 46.3 % | 1 286 | 14 051 |
| Leer la consulta | 277 | 0 | 0.0 % | 263 | 7 644 |
| Registrar diagnóstico | 277 | 0 | 0.0 % | 242 | 7 393 |
| Registrar receta | 277 | 0 | 0.0 % | 231 | 7 161 |
| Actualizar mascota | 277 | 0 | 0.0 % | 291 | 8 491 |
| Cerrar la consulta | 277 | 0 | 0.0 % | 253 | 10 246 |
| Caja: crear punto de cobro | 130 | 0 | 0.0 % | 884 | 9 741 |
| Caja: vincular equipo | 130 | 0 | 0.0 % | 537 | 8 842 |
| Caja: abrir la caja | 130 | 0 | 0.0 % | 1 012 | 10 558 |
| Cobrar | 167 | 0 | 0.0 % | 759 | 7 177 |
| Cancelar cita | 198 | 0 | 0.0 % | 1 181 | 11 394 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1407 | 0 (0 %) | 2 366 | 12 368 | 11 % |
| cumplimiento | 1407 | 0 (0 %) | 617 | 9 732 | 6 % |
| navegacion | 1407 | 0 (0 %) | 775 | 9 129 | 6 % |
| apoderado | 1078 | 0 (0 %) | 1 210 | 10 956 | 6 % |
| avisos_de_cumplimiento | 1407 | 0 (0 %) | 676 | 9 013 | 6 % |
| notificaciones | 1407 | 0 (0 %) | 636 | 9 078 | 6 % |
| mascota | 1078 | 0 (0 %) | 1 045 | 10 651 | 6 % |
| privacidad | 1407 | 0 (0 %) | 558 | 7 451 | 5 % |
| cita | 1078 | 18 (2 %) | 943 | 9 845 | 5 % |
| propietarios | 827 | 0 (0 %) | 1 090 | 11 158 | 4 % |
| citas_del_mes | 827 | 0 (0 %) | 1 017 | 10 571 | 4 % |
| empleados | 827 | 0 (0 %) | 876 | 9 929 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 13 | 332 | 0 % |
| 0.3 | 31 | 185 | 497 | 0 % |
| 0.8 | 61 | 308 | 370 | 0 % |
| 1.3 | 91 | 418 | 382 | 0 % |
| 1.8 | 121 | 543 | 615 | 0 % |
| 2.3 | 151 | 635 | 689 | 0 % |
| 2.8 | 181 | 563 | 2 783 | 1 % |
| 3.3 | 211 | 587 | 3 479 | 1 % |
| 3.8 | 241 | 630 | 4 270 | 1 % |
| 4.3 | 271 | 548 | 6 559 | 1 % |
| 4.8 | 301 | 574 | 9 781 | 1 % |
| 5.3 | 331 | 586 | 15 515 | 2 % |
| 5.8 | 350 | 494 | 15 479 | 4 % |
| 6.3 | 350 | 368 | 16 144 | 2 % |
| 6.8 | 350 | 788 | 13 895 | 2 % |
| 7.3 | 350 | 799 | 3 282 | 2 % |
| 7.8 | 350 | 1088 | 4 926 | 2 % |
| 8.3 | 350 | 1077 | 1 049 | 1 % |
| 8.8 | 131 | 7 | 8 | 0 % |

</details>

## Código nuevo: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 10999; fallidas: 164.
- Acciones hechas: panel 1565, agenda 1309, historia 1066, reportes 151, recepcion 1500, atencion 820, cobro 496, cancelar 318; sin trabajo disponible: 1088.
- CPU del backend: 73 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 34 % del tiempo.
- CPU de PostgreSQL: 46 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 353 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **308 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.0 s.
- Correo: el servidor de pruebas recibió 1500 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (-0.1 %).
- GC: 400 pausas, 67.0 s en total (12 % del tiempo de vida de la JVM); 53 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 975 MB (tope -Xmx1g).
- Clases cargadas: 25587 en total, 188 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.7 | 118 | 120 | 22748 |
| 2.2 | 132 | 134 | 24971 |
| 3.4 | 134 | 137 | 25364 |
| 4.1 | 134 | 137 | 25390 |
| 4.9 | 135 | 138 | 25456 |
| 5.4 | 136 | 139 | 25498 |
| 5.9 | 136 | 139 | 25526 |
| 6.3 | 136 | 139 | 25544 |
| 6.8 | 136 | 139 | 25544 |
| 7.2 | 136 | 139 | 25546 |
| 7.7 | 136 | 139 | 25546 |
| 8.2 | 136 | 140 | 25546 |
| 8.6 | 135 | 139 | 25587 |
| 9.2 | 135 | 139 | 25587 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 177 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 184 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 302 | 122.5 | 405.73 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1915 | 96.5 | 50.4 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1915 | 92.5 | 48.28 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1066 | 45.5 | 42.67 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 302 | 39.1 | 129.54 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1915 | 38.7 | 20.23 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 3224 | 27.4 | 8.51 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 151 | 7.9 | 52.23 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 3224 | 7.3 | 2.26 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 176412 | 6.9 | 0.04 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1500 | 1500 | coincide |
| Mascotas | 1500 | 1500 | coincide |
| Citas agendadas | 1478 | 1478 | coincide |
| Diagnósticos | 678 | 678 | coincide |
| Recetas | 678 | 678 | coincide |
| Pagos | 496 | 496 | coincide |
| Citas completadas | 678 | 678 | coincide |
| Citas canceladas | 318 | 318 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1500 | 0 | 0.0 % | 83 | 1 378 |
| Registrar mascota | 1500 | 0 | 0.0 % | 44 | 1 228 |
| Agendar cita | 1478 | 22 | 1.5 % | 61 | 1 048 |
| Recepción (llegada) | 820 | 0 | 0.0 % | 52 | 1 435 |
| Iniciar atención | 678 | 142 | 17.3 % | 61 | 1 433 |
| Leer la consulta | 678 | 0 | 0.0 % | 31 | 816 |
| Registrar diagnóstico | 678 | 0 | 0.0 % | 29 | 797 |
| Registrar receta | 678 | 0 | 0.0 % | 30 | 775 |
| Actualizar mascota | 678 | 0 | 0.0 % | 28 | 794 |
| Cerrar la consulta | 678 | 0 | 0.0 % | 49 | 728 |
| Caja: crear punto de cobro | 273 | 0 | 0.0 % | 37 | 1 511 |
| Caja: vincular equipo | 273 | 0 | 0.0 % | 28 | 904 |
| Caja: abrir la caja | 273 | 0 | 0.0 % | 36 | 1 064 |
| Cobrar | 496 | 0 | 0.0 % | 54 | 1 512 |
| Cancelar cita | 318 | 0 | 0.0 % | 70 | 2 356 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1915 | 0 (0 %) | 183 | 1 196 | 8 % |
| reporte_clinico | 151 | 0 (0 %) | 4 114 | 8 312 | 7 % |
| apoderado | 1500 | 0 (0 %) | 83 | 1 378 | 6 % |
| propietarios | 1309 | 0 (0 %) | 147 | 1 365 | 5 % |
| navegacion | 1915 | 0 (0 %) | 23 | 1 208 | 5 % |
| citas_del_mes | 1309 | 0 (0 %) | 68 | 1 532 | 5 % |
| login | 350 | 0 (0 %) | 505 | 4 183 | 5 % |
| privacidad | 1915 | 0 (0 %) | 34 | 939 | 4 % |
| mascota | 1500 | 0 (0 %) | 44 | 1 228 | 4 % |
| cita | 1500 | 22 (1 %) | 61 | 1 048 | 4 % |
| avisos_de_cumplimiento | 1915 | 0 (0 %) | 19 | 1 015 | 4 % |
| cumplimiento | 1915 | 0 (0 %) | 23 | 976 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 63 | 316 | 0 % |
| 0.4 | 38 | 192 | 462 | 0 % |
| 0.9 | 68 | 313 | 305 | 1 % |
| 1.4 | 98 | 461 | 354 | 0 % |
| 1.9 | 128 | 556 | 69 | 0 % |
| 2.4 | 158 | 674 | 86 | 0 % |
| 2.9 | 188 | 787 | 103 | 0 % |
| 3.4 | 218 | 862 | 1 347 | 0 % |
| 3.9 | 248 | 1022 | 225 | 0 % |
| 4.4 | 278 | 1068 | 1 015 | 1 % |
| 4.9 | 308 | 984 | 1 475 | 1 % |
| 5.4 | 338 | 1162 | 306 | 0 % |
| 5.9 | 350 | 1274 | 2 029 | 1 % |
| 6.4 | 350 | 1372 | 1 161 | 1 % |
| 6.9 | 350 | 1322 | 308 | 0 % |
| 7.4 | 350 | 1022 | 1 051 | 1 % |
| 7.9 | 350 | 1270 | 425 | 0 % |
| 8.4 | 343 | 845 | 196 | 0 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
