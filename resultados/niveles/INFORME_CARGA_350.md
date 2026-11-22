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
| Veredicto | FALLA | DEGRADADO |
| Errores (%) | 17.9 % | 1.7 % |
| Ritmo medio (peticiones/s) | 20.6 | 50.1 |
| Tiempo mediano (ms) | 1 249 | 46 |
| Tiempo p95 (ms) | 30 001 | 2 015 |
| Usuarios sin degradación | 174 | 335 |
| CPU del backend, media (% del límite) | 39 | 72 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 43 | 44 |
| Memoria pico (MB) | 1 347 | 1 372 |
| Sentencias SQL por carga del panel (sonda, sin carga) | 62.2 | 53.2 |
| Tiempo SQL por carga del panel (ms, sin carga) | 119.7 | 24.3 |
| Panel sin carga: mediana de 19 llamadas (ms) | 294 | 109 |
| Panel sin carga: primera llamada (ms) | 568 | 536 |
| Panel bajo carga: mediana (ms) | 2 973 | 188 |
| Panel bajo carga: p95 (ms) | 30 001 | 1 522 |
| Respuesta del panel idéntica a la del código anterior (sin el registro reciente) | - | sí |
| Capacidad del heap en la primera pausa de GC (MB) | 989 | 989 |
| Pausas de GC en total | 173 | 380 |
| Pausas completas | 25 | 50 |
| Pausas completas por "Allocation Failure" | 10 | 37 |
| Tiempo en pausas completas (s) | 9.9 | 26.9 |
| Tiempo en GC / vida de la JVM | 4.2 % | 11.5 % |
| Heap tras GC, máximo (MB) | 963 | 989 |
| Avisos de pool agotado | 1 615 | 44 |

## Código anterior: FALLA; datos CONSISTENTES

- Escrituras correctas: 3034; fallidas: 456.
- Acciones hechas: panel 529, agenda 479, historia 442, reportes 69, recepcion 720, atencion 269, cobro 104, cancelar 109; sin trabajo disponible: 500.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 1615 avisos en el registro.
- CPU del backend: 39 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 43 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 347 MB (66 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.7 con unos 350 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **174 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 574 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 137 MB (+1.1 %).
- GC: 173 pausas, 25.6 s en total (4 % del tiempo de vida de la JVM); 25 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 963 MB (tope -Xmx1g).
- Clases cargadas: 25542 en total, 140 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4505 |
| 0.3 | 81 | 82 | 16122 |
| 0.4 | 97 | 98 | 18567 |
| 1.7 | 130 | 132 | 24661 |
| 2.4 | 132 | 134 | 24979 |
| 2.7 | 133 | 136 | 25215 |
| 3.2 | 133 | 136 | 25325 |
| 3.6 | 134 | 137 | 25371 |
| 3.9 | 134 | 137 | 25375 |
| 4.1 | 134 | 137 | 25377 |
| 4.5 | 135 | 138 | 25398 |
| 4.8 | 135 | 138 | 25400 |
| 5.2 | 135 | 138 | 25417 |
| 5.7 | 135 | 138 | 25467 |
| 7.2 | 136 | 139 | 25540 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 93 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 100 |
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
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 709 | 319.9 | 451.26 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 709 | 266.1 | 375.29 | 29 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 709 | 91.9 | 129.55 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 120 | 71.3 | 594.09 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 709 | 36.8 | 51.97 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 709 | 34.5 | 48.67 | 4 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 709 | 32.0 | 45.17 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 120 | 19.9 | 166.2 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 367 | 18.7 | 50.84 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1101 | 3.5 | 3.18 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 529 | 581 | la base tiene más (ver nota) |
| Mascotas | 522 | 525 | la base tiene más (ver nota) |
| Citas agendadas | 496 | 501 | la base tiene más (ver nota) |
| Diagnósticos | 146 | 146 | coincide |
| Recetas | 144 | 144 | coincide |
| Pagos | 87 | 89 | la base tiene más (ver nota) |
| Citas completadas | 139 | 142 | la base tiene más (ver nota) |
| Citas canceladas | 80 | 83 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 529 | 191 | 26.5 % | 1 929 | 30 001 |
| Registrar mascota | 522 | 7 | 1.3 % | 251 | 8 987 |
| Agendar cita | 496 | 26 | 5.0 % | 293 | 9 671 |
| Recepción (llegada) | 220 | 49 | 18.2 % | 1 558 | 30 001 |
| Iniciar atención | 146 | 123 | 45.7 % | 1 898 | 30 001 |
| Leer la consulta | 146 | 0 | 0.0 % | 48 | 4 278 |
| Registrar diagnóstico | 146 | 0 | 0.0 % | 47 | 6 752 |
| Registrar receta | 144 | 2 | 1.4 % | 47 | 7 209 |
| Actualizar mascota | 141 | 5 | 3.4 % | 42 | 7 085 |
| Cerrar la consulta | 139 | 7 | 4.8 % | 70 | 12 549 |
| Caja: crear punto de cobro | 80 | 9 | 10.1 % | 2 245 | 30 001 |
| Caja: vincular equipo | 79 | 1 | 1.2 % | 875 | 7 132 |
| Caja: abrir la caja | 79 | 0 | 0.0 % | 1 222 | 9 429 |
| Cobrar | 87 | 7 | 7.4 % | 2 267 | 30 001 |
| Cancelar cita | 80 | 29 | 26.6 % | 2 533 | 30 001 |

**Endpoints que más tiempo acumulan, Código anterior:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 846 | 164 (19 %) | 2 973 | 30 001 | 8 % |
| privacidad | 931 | 189 (20 %) | 1 004 | 30 001 | 8 % |
| apoderado | 720 | 191 (27 %) | 1 929 | 30 001 | 8 % |
| cumplimiento | 914 | 186 (20 %) | 1 142 | 30 001 | 8 % |
| navegacion | 931 | 175 (19 %) | 1 536 | 30 001 | 8 % |
| notificaciones | 890 | 176 (20 %) | 810 | 30 001 | 8 % |
| avisos_de_cumplimiento | 870 | 169 (19 %) | 953 | 30 001 | 8 % |
| login | 458 | 141 (31 %) | 3 763 | 30 001 | 6 % |
| empleados | 496 | 120 (24 %) | 1 911 | 30 001 | 5 % |
| citas_del_mes | 496 | 109 (22 %) | 2 147 | 30 001 | 5 % |
| propietarios | 479 | 114 (24 %) | 1 850 | 30 001 | 5 % |
| lista_de_historias | 442 | 94 (21 %) | 2 047 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 63 | 470 | 0 % |
| 0.4 | 39 | 193 | 332 | 0 % |
| 0.9 | 69 | 302 | 302 | 0 % |
| 1.4 | 99 | 457 | 457 | 0 % |
| 1.9 | 129 | 561 | 353 | 0 % |
| 2.4 | 159 | 680 | 844 | 0 % |
| 2.9 | 189 | 558 | 3 240 | 1 % |
| 3.4 | 219 | 447 | 8 696 | 0 % |
| 3.9 | 249 | 700 | 6 604 | 1 % |
| 4.4 | 279 | 471 | 8 709 | 2 % |
| 4.9 | 309 | 512 | 11 570 | 1 % |
| 5.4 | 339 | 98 | 14 997 | 4 % |
| 5.9 | 350 | 177 | 30 001 | 86 % |
| 6.4 | 350 | 179 | 30 001 | 91 % |
| 6.9 | 350 | 168 | 30 001 | 100 % |
| 7.4 | 350 | 174 | 30 001 | 100 % |
| 7.9 | 350 | 151 | 30 001 | 100 % |
| 8.4 | 348 | 160 | 30 001 | 100 % |
| 8.9 | 235 | 102 | 30 001 | 100 % |
| 9.4 | 147 | 49 | 30 001 | 100 % |

</details>

## Código nuevo: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 9152; fallidas: 335.
- Acciones hechas: panel 1481, agenda 1125, historia 964, reportes 145, recepcion 1473, atencion 710, cobro 360, cancelar 293; sin trabajo disponible: 1069.
- Se agotó el pool de conexiones a la base de datos: 44 avisos en el registro.
- El servidor rechazó trabajo por falta de hilos: 7 avisos.
- CPU del backend: 72 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 32 % del tiempo.
- CPU de PostgreSQL: 44 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 372 MB (67 % de 2048 MB).
- Los fallos fueron: 46 % sin respuesta (tiempo agotado), 54 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 5.8 con unos 350 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **335 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 1466 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (-0.1 %).
- GC: 380 pausas, 65.0 s en total (11 % del tiempo de vida de la JVM); 50 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25636 en total, 181 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4507 |
| 0.6 | 115 | 117 | 22474 |
| 2.1 | 131 | 134 | 24905 |
| 3.0 | 133 | 136 | 25294 |
| 3.9 | 134 | 137 | 25380 |
| 4.4 | 135 | 138 | 25455 |
| 4.9 | 135 | 138 | 25456 |
| 5.5 | 136 | 139 | 25497 |
| 6.1 | 136 | 139 | 25540 |
| 6.5 | 136 | 139 | 25542 |
| 7.3 | 137 | 140 | 25594 |
| 7.8 | 137 | 140 | 25595 |
| 8.4 | 137 | 140 | 25596 |
| 8.8 | 137 | 140 | 25621 |
| 9.2 | 135 | 140 | 25636 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 136 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 143 |
| `ch.qos.logback.classic` | 6 |
| `org.hibernate.validator.internal` | 3 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 290 | 109.1 | 376.32 | 21 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1831 | 92.9 | 50.76 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1831 | 85.8 | 46.85 | 17 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1829 | 43.3 | 23.65 | 8 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 963 | 41.0 | 42.55 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 290 | 35.0 | 120.58 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2956 | 23.8 | 8.04 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 145 | 7.1 | 48.76 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2956 | 6.7 | 2.28 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 153862 | 5.6 | 0.04 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1450 | 1473 | la base tiene más (ver nota) |
| Mascotas | 1439 | 1449 | la base tiene más (ver nota) |
| Citas agendadas | 1411 | 1417 | la base tiene más (ver nota) |
| Diagnósticos | 476 | 477 | la base tiene más (ver nota) |
| Recetas | 477 | 477 | coincide |
| Pagos | 346 | 349 | la base tiene más (ver nota) |
| Citas completadas | 474 | 474 | coincide |
| Citas canceladas | 287 | 292 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1450 | 23 | 1.6 % | 74 | 2 896 |
| Registrar mascota | 1439 | 11 | 0.8 % | 39 | 1 695 |
| Agendar cita | 1411 | 28 | 1.9 % | 54 | 1 536 |
| Recepción (llegada) | 697 | 13 | 1.8 % | 36 | 2 760 |
| Iniciar atención | 477 | 233 | 32.8 % | 46 | 3 323 |
| Leer la consulta | 474 | 3 | 0.6 % | 26 | 663 |
| Registrar diagnóstico | 476 | 1 | 0.2 % | 25 | 644 |
| Registrar receta | 477 | 0 | 0.0 % | 26 | 605 |
| Actualizar mascota | 477 | 0 | 0.0 % | 24 | 613 |
| Cerrar la consulta | 474 | 3 | 0.6 % | 43 | 581 |
| Caja: crear punto de cobro | 224 | 7 | 3.0 % | 27 | 2 528 |
| Caja: vincular equipo | 222 | 2 | 0.9 % | 21 | 953 |
| Caja: abrir la caja | 221 | 1 | 0.5 % | 26 | 1 181 |
| Cobrar | 346 | 4 | 1.1 % | 36 | 2 097 |
| Cancelar cita | 287 | 6 | 2.0 % | 51 | 1 925 |

**Endpoints que más tiempo acumulan, Código nuevo:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| navegacion | 1831 | 21 (1 %) | 20 | 3 077 | 9 % |
| apoderado | 1473 | 23 (2 %) | 74 | 2 896 | 8 % |
| citas_del_mes | 1125 | 20 (2 %) | 55 | 3 528 | 7 % |
| privacidad | 1831 | 13 (1 %) | 38 | 2 015 | 7 % |
| lista_de_historias | 964 | 12 (1 %) | 101 | 3 401 | 6 % |
| mascota | 1450 | 11 (1 %) | 39 | 1 695 | 5 % |
| historia | 953 | 11 (1 %) | 45 | 2 532 | 4 % |
| empleados | 1125 | 10 (1 %) | 37 | 2 364 | 4 % |
| reporte_clinico | 145 | 6 (4 %) | 3 524 | 23 832 | 4 % |
| login | 362 | 12 (3 %) | 501 | 8 797 | 4 % |
| panel | 1831 | 0 (0 %) | 188 | 1 522 | 4 % |
| checkin | 710 | 13 (2 %) | 36 | 2 760 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 38 | 330 | 0 % |
| 0.3 | 35 | 190 | 322 | 0 % |
| 0.8 | 65 | 307 | 388 | 0 % |
| 1.4 | 95 | 403 | 222 | 0 % |
| 1.9 | 125 | 526 | 97 | 0 % |
| 2.4 | 155 | 679 | 100 | 0 % |
| 2.9 | 185 | 809 | 206 | 0 % |
| 3.4 | 215 | 857 | 1 212 | 0 % |
| 3.9 | 245 | 1029 | 358 | 0 % |
| 4.3 | 275 | 1260 | 267 | 0 % |
| 4.8 | 305 | 1159 | 677 | 0 % |
| 5.3 | 335 | 955 | 765 | 0 % |
| 5.8 | 350 | 23 | 30 001 | 100 % |
| 6.3 | 350 | 1071 | 7 145 | 3 % |
| 6.8 | 350 | 1273 | 246 | 2 % |
| 7.3 | 350 | 1168 | 191 | 2 % |
| 7.8 | 350 | 1060 | 2 502 | 3 % |
| 8.3 | 350 | 870 | 5 623 | 2 % |

</details>

> Mismo servidor, misma imagen y misma base de partida; solo cambia la bandera app.dashboard.consultas-optimizadas. Una corrida por variante y nivel.
