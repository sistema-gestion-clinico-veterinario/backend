# Fase 3b: 350 usuarios, con y sin -Xms (1 CPU, 2 GB)

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | SIN -Xms | CON -Xms = -Xmx |
|---|---|---|
| Veredicto | SATURADO (muy lento, casi sin errores) | SATURADO (muy lento, casi sin errores) |
| Errores (%) | 1.6 % | 1.5 % |
| Ritmo medio (peticiones/s) | 35.1 | 34.3 |
| Tiempo mediano (ms) | 1 984 | 2 077 |
| Tiempo p95 (ms) | 7 883 | 8 174 |
| Usuarios sin degradación | 173 | 173 |
| CPU del backend, media (% del límite) | 51 | 49 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 81 | 83 |
| Memoria pico (MB) | 1 366 | 1 343 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 455 | 245 |
| Pausas completas | 40 | 28 |
| Pausas completas por "Allocation Failure" | 31 | 14 |
| Tiempo en pausas completas (s) | 13.3 | 8.9 |
| Tiempo en GC / vida de la JVM | 7.3 % | 6.0 % |
| Heap tras GC, máximo (MB) | 989 | 971 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5801; fallidas: 303.
- Acciones hechas: panel 1030, agenda 783, historia 654, reportes 86, recepcion 1076, atencion 510, cobro 166, cancelar 178; sin trabajo disponible: 775.
- CPU del backend: 51 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 6 % del tiempo.
- CPU de PostgreSQL: 81 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 366 MB (67 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **173 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 14.3 s.
- Correo: el servidor de pruebas recibió 1076 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 455 pausas, 40.6 s en total (7 % del tiempo de vida de la JVM); 40 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25536 en total, 128 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 19 | 19 | 4663 |
| 0.1 | 49 | 50 | 10920 |
| 0.2 | 80 | 80 | 16047 |
| 0.3 | 82 | 83 | 16400 |
| 0.3 | 88 | 88 | 17302 |
| 1.4 | 128 | 131 | 24322 |
| 2.3 | 132 | 135 | 25073 |
| 3.1 | 134 | 136 | 25330 |
| 3.8 | 134 | 137 | 25383 |
| 5.0 | 135 | 138 | 25475 |
| 5.9 | 136 | 139 | 25475 |
| 7.0 | 136 | 139 | 25477 |
| 8.0 | 136 | 139 | 25535 |
| 9.1 | 136 | 139 | 25536 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 121 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 126 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1380 | 1154.0 | 836.21 | 50 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1380 | 215.5 | 156.17 | 9 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1380 | 138.2 | 100.12 | 6 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 172 | 126.7 | 736.65 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1380 | 107.5 | 77.92 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1380 | 105.6 | 76.51 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1380 | 104.4 | 75.65 | 4 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1380 | 76.0 | 55.1 | 3 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2163 | 56.8 | 26.26 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 654 | 46.9 | 71.76 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1076 | 1076 | coincide |
| Mascotas | 1076 | 1076 | coincide |
| Citas agendadas | 1055 | 1055 | coincide |
| Diagnósticos | 228 | 228 | coincide |
| Recetas | 228 | 228 | coincide |
| Pagos | 166 | 166 | coincide |
| Citas completadas | 228 | 228 | coincide |
| Citas canceladas | 178 | 178 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1076 | 0 | 0.0 % | 2 211 | 7 877 |
| Registrar mascota | 1076 | 0 | 0.0 % | 1 974 | 8 137 |
| Agendar cita | 1055 | 21 | 2.0 % | 2 221 | 8 144 |
| Recepción (llegada) | 510 | 0 | 0.0 % | 2 442 | 8 262 |
| Iniciar atención | 228 | 282 | 55.3 % | 2 710 | 8 910 |
| Leer la consulta | 228 | 0 | 0.0 % | 973 | 6 651 |
| Registrar diagnóstico | 228 | 0 | 0.0 % | 1 089 | 6 898 |
| Registrar receta | 228 | 0 | 0.0 % | 1 186 | 7 166 |
| Actualizar mascota | 228 | 0 | 0.0 % | 916 | 7 047 |
| Cerrar la consulta | 228 | 0 | 0.0 % | 1 096 | 8 036 |
| Caja: crear punto de cobro | 124 | 0 | 0.0 % | 1 172 | 7 433 |
| Caja: vincular equipo | 124 | 0 | 0.0 % | 1 206 | 6 292 |
| Caja: abrir la caja | 124 | 0 | 0.0 % | 1 476 | 7 501 |
| Cobrar | 166 | 0 | 0.0 % | 2 071 | 8 526 |
| Cancelar cita | 178 | 0 | 0.0 % | 2 251 | 7 579 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1380 | 0 (0 %) | 2 807 | 9 167 | 9 % |
| privacidad | 1380 | 0 (0 %) | 2 210 | 6 556 | 7 % |
| navegacion | 1380 | 0 (0 %) | 1 495 | 7 716 | 6 % |
| avisos_de_cumplimiento | 1380 | 0 (0 %) | 1 397 | 7 075 | 6 % |
| notificaciones | 1380 | 0 (0 %) | 1 450 | 6 896 | 6 % |
| apoderado | 1076 | 0 (0 %) | 2 211 | 7 877 | 6 % |
| cita | 1076 | 21 (2 %) | 2 221 | 8 144 | 6 % |
| mascota | 1076 | 0 (0 %) | 1 974 | 8 137 | 6 % |
| cumplimiento | 1380 | 0 (0 %) | 1 373 | 6 676 | 6 % |
| propietarios | 783 | 0 (0 %) | 2 746 | 8 721 | 5 % |
| citas_del_mes | 783 | 0 (0 %) | 2 404 | 8 548 | 5 % |
| empleados | 783 | 0 (0 %) | 2 418 | 8 653 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 466 | 0 % |
| 0.4 | 38 | 195 | 250 | 1 % |
| 0.9 | 68 | 323 | 376 | 0 % |
| 1.4 | 98 | 422 | 305 | 0 % |
| 1.9 | 128 | 568 | 498 | 0 % |
| 2.4 | 158 | 659 | 1 315 | 0 % |
| 2.9 | 188 | 624 | 2 685 | 1 % |
| 3.4 | 218 | 667 | 1 751 | 1 % |
| 3.9 | 248 | 594 | 5 027 | 2 % |
| 4.4 | 278 | 554 | 6 293 | 2 % |
| 4.9 | 308 | 634 | 8 314 | 1 % |
| 5.4 | 338 | 567 | 9 778 | 1 % |
| 5.9 | 350 | 537 | 12 542 | 3 % |
| 6.4 | 350 | 606 | 12 883 | 2 % |
| 6.9 | 350 | 643 | 7 036 | 3 % |
| 7.4 | 350 | 715 | 7 681 | 3 % |
| 7.9 | 350 | 792 | 4 946 | 4 % |
| 8.4 | 346 | 703 | 8 351 | 2 % |

</details>

## CON -Xms = -Xmx: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5758; fallidas: 286.
- Acciones hechas: panel 966, agenda 782, historia 651, reportes 80, recepcion 1082, atencion 487, cobro 160, cancelar 196; sin trabajo disponible: 758.
- CPU del backend: 49 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 83 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 343 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **173 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 12.5 s.
- Correo: el servidor de pruebas recibió 1082 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 245 pausas, 33.0 s en total (6 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 971 MB (tope -Xmx1g).
- Clases cargadas: 25532 en total, 130 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 18 | 18 | 4507 |
| 0.3 | 86 | 87 | 17103 |
| 1.1 | 127 | 129 | 24340 |
| 2.0 | 131 | 134 | 24953 |
| 2.8 | 133 | 136 | 25295 |
| 3.4 | 134 | 137 | 25375 |
| 4.0 | 135 | 138 | 25394 |
| 4.5 | 135 | 138 | 25402 |
| 5.3 | 135 | 138 | 25469 |
| 5.9 | 136 | 139 | 25471 |
| 6.6 | 136 | 139 | 25472 |
| 7.3 | 136 | 139 | 25475 |
| 7.9 | 136 | 139 | 25515 |
| 8.5 | 136 | 139 | 25531 |
| 9.0 | 136 | 139 | 25532 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 122 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 127 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1082 | 1082 | coincide |
| Mascotas | 1082 | 1082 | coincide |
| Citas agendadas | 1062 | 1062 | coincide |
| Diagnósticos | 221 | 221 | coincide |
| Recetas | 221 | 221 | coincide |
| Pagos | 160 | 160 | coincide |
| Citas completadas | 221 | 221 | coincide |
| Citas canceladas | 196 | 196 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1082 | 0 | 0.0 % | 2 308 | 8 374 |
| Registrar mascota | 1082 | 0 | 0.0 % | 2 283 | 7 898 |
| Agendar cita | 1062 | 20 | 1.8 % | 2 434 | 8 294 |
| Recepción (llegada) | 487 | 0 | 0.0 % | 2 954 | 9 480 |
| Iniciar atención | 221 | 266 | 54.6 % | 3 107 | 9 637 |
| Leer la consulta | 221 | 0 | 0.0 % | 1 122 | 7 941 |
| Registrar diagnóstico | 221 | 0 | 0.0 % | 871 | 7 198 |
| Registrar receta | 221 | 0 | 0.0 % | 1 112 | 6 891 |
| Actualizar mascota | 221 | 0 | 0.0 % | 1 340 | 7 519 |
| Cerrar la consulta | 221 | 0 | 0.0 % | 995 | 7 446 |
| Caja: crear punto de cobro | 121 | 0 | 0.0 % | 1 593 | 6 938 |
| Caja: vincular equipo | 121 | 0 | 0.0 % | 1 290 | 7 589 |
| Caja: abrir la caja | 121 | 0 | 0.0 % | 1 578 | 7 048 |
| Cobrar | 160 | 0 | 0.0 % | 2 248 | 7 702 |
| Cancelar cita | 196 | 0 | 0.0 % | 2 511 | 9 080 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1316 | 0 (0 %) | 2 833 | 9 290 | 9 % |
| privacidad | 1316 | 0 (0 %) | 2 154 | 6 934 | 7 % |
| cita | 1082 | 20 (2 %) | 2 434 | 8 294 | 6 % |
| apoderado | 1082 | 0 (0 %) | 2 308 | 8 374 | 6 % |
| navegacion | 1316 | 0 (0 %) | 1 717 | 7 643 | 6 % |
| mascota | 1082 | 0 (0 %) | 2 283 | 7 898 | 6 % |
| avisos_de_cumplimiento | 1316 | 0 (0 %) | 1 409 | 7 060 | 6 % |
| cumplimiento | 1316 | 0 (0 %) | 1 360 | 7 175 | 5 % |
| notificaciones | 1316 | 0 (0 %) | 1 498 | 7 096 | 5 % |
| propietarios | 782 | 0 (0 %) | 2 982 | 9 551 | 5 % |
| citas_del_mes | 782 | 0 (0 %) | 2 680 | 9 909 | 5 % |
| empleados | 782 | 0 (0 %) | 2 548 | 8 329 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 62 | 524 | 0 % |
| 0.4 | 38 | 200 | 371 | 0 % |
| 0.9 | 68 | 309 | 355 | 0 % |
| 1.4 | 98 | 427 | 349 | 0 % |
| 1.9 | 128 | 555 | 872 | 0 % |
| 2.4 | 158 | 623 | 1 278 | 0 % |
| 2.9 | 188 | 648 | 1 648 | 1 % |
| 3.4 | 218 | 655 | 2 847 | 1 % |
| 3.9 | 248 | 573 | 5 879 | 1 % |
| 4.4 | 278 | 670 | 6 078 | 1 % |
| 4.9 | 308 | 650 | 6 027 | 1 % |
| 5.4 | 338 | 601 | 7 617 | 2 % |
| 5.9 | 350 | 600 | 12 513 | 2 % |
| 6.4 | 350 | 643 | 9 135 | 2 % |
| 6.9 | 350 | 692 | 7 325 | 3 % |
| 7.4 | 350 | 589 | 10 799 | 3 % |
| 7.9 | 350 | 547 | 11 659 | 3 % |
| 8.4 | 347 | 659 | 5 616 | 3 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
