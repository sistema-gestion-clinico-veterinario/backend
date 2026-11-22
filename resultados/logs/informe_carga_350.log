# Fase 3b: 350 usuarios, con y sin -Xms (1 CPU, 2 GB)

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
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
| Errores (%) | 1.5 % | 1.7 % |
| Ritmo medio (peticiones/s) | 32.7 | 30.7 |
| Tiempo mediano (ms) | 2 179 | 2 708 |
| Tiempo p95 (ms) | 9 405 | 10 113 |
| Usuarios sin degradación | 159 | 156 |
| CPU del backend, media (% del límite) | 59 | 55 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 83 | 84 |
| Memoria pico (MB) | 1 324 | 1 350 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 483 | 249 |
| Pausas completas | 45 | 31 |
| Pausas completas por "Allocation Failure" | 35 | 18 |
| Tiempo en pausas completas (s) | 16.4 | 12.8 |
| Tiempo en GC / vida de la JVM | 7.8 % | 6.5 % |
| Heap tras GC, máximo (MB) | 953 | 977 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5332; fallidas: 272.
- Acciones hechas: panel 908, agenda 766, historia 623, reportes 82, recepcion 995, atencion 460, cobro 142, cancelar 176; sin trabajo disponible: 724.
- CPU del backend: 59 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 83 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 324 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **159 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 18.1 s.
- Correo: el servidor de pruebas recibió 995 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 483 pausas, 43.6 s en total (8 % del tiempo de vida de la JVM); 45 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 953 MB (tope -Xmx1g).
- Clases cargadas: 25532 en total, 129 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4732 |
| 0.2 | 53 | 54 | 11379 |
| 0.3 | 80 | 81 | 16076 |
| 0.4 | 86 | 87 | 17086 |
| 0.5 | 105 | 106 | 20361 |
| 1.8 | 131 | 133 | 24822 |
| 2.7 | 133 | 136 | 25228 |
| 3.7 | 134 | 137 | 25376 |
| 4.4 | 135 | 138 | 25403 |
| 5.3 | 135 | 138 | 25446 |
| 6.3 | 136 | 139 | 25471 |
| 7.4 | 136 | 139 | 25473 |
| 8.1 | 136 | 139 | 25489 |

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
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1258 | 1259.3 | 1001.04 | 52 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 1258 | 214.9 | 170.84 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 164 | 137.7 | 839.9 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 1258 | 124.5 | 98.99 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1258 | 119.5 | 94.97 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 1258 | 101.4 | 80.59 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 1258 | 97.8 | 77.73 | 4 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 1258 | 75.9 | 60.33 | 3 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2024 | 72.4 | 35.79 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 623 | 51.3 | 82.39 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 995 | 995 | coincide |
| Mascotas | 995 | 995 | coincide |
| Citas agendadas | 977 | 977 | coincide |
| Diagnósticos | 206 | 206 | coincide |
| Recetas | 206 | 206 | coincide |
| Pagos | 142 | 142 | coincide |
| Citas completadas | 206 | 206 | coincide |
| Citas canceladas | 176 | 176 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 995 | 0 | 0.0 % | 2 376 | 10 227 |
| Registrar mascota | 995 | 0 | 0.0 % | 2 215 | 9 290 |
| Agendar cita | 977 | 18 | 1.8 % | 2 465 | 9 329 |
| Recepción (llegada) | 460 | 0 | 0.0 % | 2 872 | 9 924 |
| Iniciar atención | 206 | 254 | 55.2 % | 3 162 | 11 230 |
| Leer la consulta | 206 | 0 | 0.0 % | 1 035 | 7 620 |
| Registrar diagnóstico | 206 | 0 | 0.0 % | 882 | 7 675 |
| Registrar receta | 206 | 0 | 0.0 % | 963 | 9 091 |
| Actualizar mascota | 206 | 0 | 0.0 % | 887 | 7 683 |
| Cerrar la consulta | 206 | 0 | 0.0 % | 855 | 8 890 |
| Caja: crear punto de cobro | 117 | 0 | 0.0 % | 1 775 | 7 954 |
| Caja: vincular equipo | 117 | 0 | 0.0 % | 1 612 | 9 007 |
| Caja: abrir la caja | 117 | 0 | 0.0 % | 1 792 | 7 956 |
| Cobrar | 142 | 0 | 0.0 % | 1 643 | 8 938 |
| Cancelar cita | 176 | 0 | 0.0 % | 2 366 | 10 367 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1258 | 0 (0 %) | 2 734 | 10 009 | 8 % |
| privacidad | 1258 | 0 (0 %) | 2 408 | 7 734 | 7 % |
| apoderado | 995 | 0 (0 %) | 2 376 | 10 227 | 6 % |
| navegacion | 1258 | 0 (0 %) | 1 663 | 8 207 | 6 % |
| cita | 995 | 18 (2 %) | 2 465 | 9 329 | 6 % |
| cumplimiento | 1258 | 0 (0 %) | 1 547 | 8 565 | 6 % |
| notificaciones | 1258 | 0 (0 %) | 1 672 | 8 150 | 6 % |
| avisos_de_cumplimiento | 1258 | 0 (0 %) | 1 407 | 8 100 | 6 % |
| mascota | 995 | 0 (0 %) | 2 215 | 9 290 | 6 % |
| citas_del_mes | 766 | 0 (0 %) | 3 315 | 11 382 | 5 % |
| propietarios | 766 | 0 (0 %) | 3 379 | 9 686 | 5 % |
| empleados | 766 | 0 (0 %) | 2 782 | 10 326 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 69 | 769 | 0 % |
| 0.4 | 39 | 197 | 441 | 1 % |
| 0.9 | 69 | 339 | 499 | 0 % |
| 1.4 | 99 | 488 | 743 | 0 % |
| 1.9 | 129 | 518 | 1 052 | 0 % |
| 2.4 | 159 | 575 | 1 533 | 0 % |
| 2.9 | 189 | 651 | 1 690 | 1 % |
| 3.4 | 219 | 576 | 3 684 | 1 % |
| 3.9 | 249 | 663 | 5 561 | 1 % |
| 4.4 | 279 | 488 | 7 466 | 3 % |
| 4.9 | 309 | 566 | 10 509 | 1 % |
| 5.4 | 339 | 530 | 12 252 | 2 % |
| 5.9 | 350 | 477 | 11 776 | 2 % |
| 6.4 | 350 | 562 | 10 337 | 2 % |
| 6.9 | 350 | 579 | 18 061 | 3 % |
| 7.4 | 350 | 614 | 8 589 | 2 % |
| 7.9 | 350 | 619 | 6 539 | 3 % |
| 8.4 | 345 | 609 | 7 599 | 2 % |

</details>

## CON -Xms = -Xmx: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4986; fallidas: 286.
- Acciones hechas: panel 846, agenda 689, historia 597, reportes 85, recepcion 943, atencion 457, cobro 126, cancelar 172; sin trabajo disponible: 731.
- CPU del backend: 55 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 84 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 350 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **156 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 18.0 s.
- Correo: el servidor de pruebas recibió 943 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 249 pausas, 36.3 s en total (6 % del tiempo de vida de la JVM); 31 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 977 MB (tope -Xmx1g).
- Clases cargadas: 25492 en total, 100 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4535 |
| 0.3 | 87 | 88 | 17198 |
| 1.7 | 130 | 132 | 24708 |
| 2.5 | 132 | 135 | 25087 |
| 3.0 | 133 | 136 | 25311 |
| 3.9 | 134 | 137 | 25379 |
| 4.5 | 135 | 138 | 25402 |
| 5.0 | 135 | 138 | 25404 |
| 5.7 | 135 | 138 | 25411 |
| 6.2 | 136 | 139 | 25445 |
| 7.1 | 136 | 139 | 25473 |
| 7.6 | 136 | 139 | 25473 |
| 8.1 | 136 | 139 | 25491 |
| 8.8 | 136 | 139 | 25492 |
| 9.1 | 136 | 139 | 25492 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 94 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 97 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 943 | 943 | coincide |
| Mascotas | 943 | 943 | coincide |
| Citas agendadas | 926 | 926 | coincide |
| Diagnósticos | 188 | 188 | coincide |
| Recetas | 188 | 188 | coincide |
| Pagos | 126 | 126 | coincide |
| Citas completadas | 188 | 188 | coincide |
| Citas canceladas | 172 | 172 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 943 | 0 | 0.0 % | 3 136 | 9 775 |
| Registrar mascota | 943 | 0 | 0.0 % | 2 954 | 10 007 |
| Agendar cita | 926 | 17 | 1.8 % | 2 730 | 9 547 |
| Recepción (llegada) | 457 | 0 | 0.0 % | 3 293 | 10 892 |
| Iniciar atención | 188 | 269 | 58.9 % | 3 793 | 11 951 |
| Leer la consulta | 188 | 0 | 0.0 % | 1 161 | 9 072 |
| Registrar diagnóstico | 188 | 0 | 0.0 % | 1 069 | 8 971 |
| Registrar receta | 188 | 0 | 0.0 % | 1 357 | 9 544 |
| Actualizar mascota | 188 | 0 | 0.0 % | 1 177 | 10 133 |
| Cerrar la consulta | 188 | 0 | 0.0 % | 1 151 | 8 327 |
| Caja: crear punto de cobro | 97 | 0 | 0.0 % | 1 446 | 9 692 |
| Caja: vincular equipo | 97 | 0 | 0.0 % | 2 150 | 8 663 |
| Caja: abrir la caja | 97 | 0 | 0.0 % | 2 351 | 10 088 |
| Cobrar | 126 | 0 | 0.0 % | 2 006 | 10 525 |
| Cancelar cita | 172 | 0 | 0.0 % | 3 686 | 9 854 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1196 | 0 (0 %) | 3 275 | 10 920 | 8 % |
| privacidad | 1196 | 0 (0 %) | 2 730 | 8 243 | 7 % |
| navegacion | 1196 | 0 (0 %) | 2 042 | 9 214 | 6 % |
| avisos_de_cumplimiento | 1196 | 0 (0 %) | 1 946 | 9 413 | 6 % |
| apoderado | 943 | 0 (0 %) | 3 136 | 9 775 | 6 % |
| notificaciones | 1196 | 0 (0 %) | 1 960 | 9 004 | 6 % |
| cumplimiento | 1196 | 0 (0 %) | 1 717 | 9 264 | 6 % |
| mascota | 943 | 0 (0 %) | 2 954 | 10 007 | 6 % |
| cita | 943 | 17 (2 %) | 2 730 | 9 547 | 6 % |
| propietarios | 689 | 0 (0 %) | 3 779 | 11 068 | 5 % |
| citas_del_mes | 689 | 0 (0 %) | 3 841 | 11 408 | 5 % |
| empleados | 689 | 0 (0 %) | 3 574 | 10 467 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 967 | 0 % |
| 0.4 | 36 | 174 | 336 | 0 % |
| 0.9 | 66 | 313 | 563 | 0 % |
| 1.4 | 96 | 433 | 765 | 0 % |
| 1.9 | 126 | 503 | 506 | 0 % |
| 2.4 | 156 | 549 | 1 451 | 0 % |
| 2.9 | 186 | 502 | 3 463 | 1 % |
| 3.4 | 216 | 541 | 5 556 | 1 % |
| 3.9 | 246 | 490 | 6 851 | 1 % |
| 4.4 | 276 | 523 | 7 968 | 1 % |
| 4.9 | 306 | 592 | 10 049 | 1 % |
| 5.4 | 336 | 353 | 16 427 | 2 % |
| 5.9 | 350 | 511 | 13 893 | 2 % |
| 6.4 | 350 | 539 | 11 333 | 2 % |
| 6.9 | 350 | 596 | 9 893 | 2 % |
| 7.4 | 350 | 582 | 11 140 | 3 % |
| 7.9 | 350 | 540 | 10 297 | 4 % |
| 8.4 | 347 | 507 | 11 662 | 3 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
