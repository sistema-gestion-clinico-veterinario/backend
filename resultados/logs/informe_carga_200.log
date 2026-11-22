# Fase 3b: 200 usuarios, con y sin -Xms (1 CPU, 2 GB)

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
| Veredicto | AGUANTA | AGUANTA |
| Errores (%) | 0.2 % | 0.1 % |
| Ritmo medio (peticiones/s) | 36.3 | 37.9 |
| Tiempo mediano (ms) | 30 | 16 |
| Tiempo p95 (ms) | 601 | 169 |
| Usuarios sin degradación | 200 | 200 |
| CPU del backend, media (% del límite) | 50 | 47 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 52 | 36 |
| Memoria pico (MB) | 1 191 | 1 316 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 405 | 185 |
| Pausas completas | 39 | 23 |
| Pausas completas por "Allocation Failure" | 30 | 8 |
| Tiempo en pausas completas (s) | 11.4 | 6.5 |
| Tiempo en GC / vida de la JVM | 8.8 % | 5.6 % |
| Heap tras GC, máximo (MB) | 696 | 971 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5486; fallidas: 34.
- Acciones hechas: panel 702, agenda 605, historia 510, reportes 74, recepcion 734, atencion 370, cobro 233, cancelar 130; sin trabajo disponible: 545.
- CPU del backend: 50 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 52 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 191 MB (58 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **200 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.0 s.
- Correo: el servidor de pruebas recibió 734 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 405 pausas, 36.0 s en total (9 % del tiempo de vida de la JVM); 39 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 696 MB (tope -Xmx1g).
- Clases cargadas: 25558 en total, 211 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 17 | 18 | 4410 |
| 0.1 | 36 | 36 | 8105 |
| 0.2 | 71 | 72 | 14595 |
| 0.2 | 80 | 81 | 16100 |
| 0.3 | 86 | 87 | 17103 |
| 0.4 | 107 | 108 | 20715 |
| 1.6 | 129 | 132 | 24483 |
| 2.4 | 132 | 135 | 25134 |
| 3.2 | 134 | 137 | 25347 |
| 3.8 | 134 | 137 | 25393 |
| 4.4 | 135 | 138 | 25453 |
| 5.3 | 135 | 138 | 25511 |
| 5.9 | 136 | 139 | 25513 |
| 6.5 | 136 | 139 | 25556 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 200 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 207 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 902 | 216.2 | 239.74 | 31 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 902 | 214.4 | 237.74 | 30 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 902 | 66.2 | 73.43 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 148 | 57.0 | 384.84 | 8 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 902 | 31.0 | 34.38 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 902 | 22.0 | 24.44 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 902 | 20.9 | 23.2 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 148 | 18.1 | 122.36 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 510 | 17.3 | 33.93 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 605 | 14.7 | 24.31 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 734 | 734 | coincide |
| Mascotas | 734 | 734 | coincide |
| Citas agendadas | 714 | 714 | coincide |
| Diagnósticos | 356 | 356 | coincide |
| Recetas | 356 | 356 | coincide |
| Pagos | 233 | 233 | coincide |
| Citas completadas | 356 | 356 | coincide |
| Citas canceladas | 130 | 130 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 734 | 0 | 0.0 % | 52 | 580 |
| Registrar mascota | 734 | 0 | 0.0 % | 28 | 263 |
| Agendar cita | 714 | 20 | 2.7 % | 40 | 276 |
| Recepción (llegada) | 370 | 0 | 0.0 % | 28 | 485 |
| Iniciar atención | 356 | 14 | 3.8 % | 37 | 289 |
| Leer la consulta | 356 | 0 | 0.0 % | 23 | 169 |
| Registrar diagnóstico | 356 | 0 | 0.0 % | 21 | 192 |
| Registrar receta | 356 | 0 | 0.0 % | 23 | 191 |
| Actualizar mascota | 356 | 0 | 0.0 % | 21 | 193 |
| Cerrar la consulta | 356 | 0 | 0.0 % | 35 | 274 |
| Caja: crear punto de cobro | 145 | 0 | 0.0 % | 21 | 380 |
| Caja: vincular equipo | 145 | 0 | 0.0 % | 20 | 256 |
| Caja: abrir la caja | 145 | 0 | 0.0 % | 22 | 231 |
| Cobrar | 233 | 0 | 0.0 % | 30 | 234 |
| Cancelar cita | 130 | 0 | 0.0 % | 33 | 493 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 902 | 0 (0 %) | 440 | 2 152 | 36 % |
| reporte_clinico | 74 | 0 (0 %) | 2 866 | 4 250 | 10 % |
| login | 200 | 0 (0 %) | 354 | 1 886 | 6 % |
| apoderado | 734 | 0 (0 %) | 52 | 580 | 5 % |
| propietarios | 605 | 0 (0 %) | 90 | 452 | 4 % |
| citas_del_mes | 605 | 0 (0 %) | 61 | 529 | 4 % |
| lista_de_historias | 510 | 0 (0 %) | 61 | 559 | 3 % |
| navegacion | 902 | 0 (0 %) | 16 | 414 | 3 % |
| cita | 734 | 20 (3 %) | 40 | 276 | 3 % |
| mascota | 734 | 0 (0 %) | 28 | 263 | 2 % |
| privacidad | 902 | 0 (0 %) | 15 | 185 | 2 % |
| empleados | 605 | 0 (0 %) | 30 | 255 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 77 | 519 | 0 % |
| 0.2 | 25 | 140 | 363 | 1 % |
| 0.5 | 39 | 207 | 278 | 0 % |
| 0.7 | 54 | 250 | 286 | 0 % |
| 0.9 | 68 | 320 | 334 | 0 % |
| 1.2 | 82 | 367 | 277 | 0 % |
| 1.4 | 97 | 418 | 318 | 0 % |
| 1.7 | 111 | 519 | 283 | 0 % |
| 1.9 | 125 | 506 | 358 | 0 % |
| 2.2 | 139 | 607 | 387 | 0 % |
| 2.5 | 154 | 656 | 542 | 0 % |
| 2.7 | 168 | 670 | 1 416 | 0 % |
| 3.0 | 182 | 667 | 1 999 | 0 % |
| 3.2 | 197 | 741 | 1 008 | 0 % |
| 3.5 | 200 | 783 | 1 473 | 0 % |
| 3.7 | 200 | 787 | 1 322 | 0 % |
| 4.0 | 200 | 806 | 463 | 0 % |
| 4.2 | 200 | 743 | 843 | 0 % |
| 4.5 | 200 | 754 | 567 | 0 % |
| 4.7 | 200 | 762 | 937 | 0 % |
| 5.0 | 200 | 767 | 194 | 0 % |
| 5.2 | 200 | 759 | 157 | 0 % |
| 5.5 | 200 | 722 | 173 | 0 % |
| 5.7 | 200 | 787 | 244 | 0 % |
| 6.0 | 200 | 653 | 195 | 0 % |
| 6.2 | 162 | 231 | 41 | 0 % |

</details>

## CON -Xms = -Xmx: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5714; fallidas: 18.
- Acciones hechas: panel 741, agenda 646, historia 530, reportes 62, recepcion 748, atencion 381, cobro 239, cancelar 152; sin trabajo disponible: 558.
- CPU del backend: 47 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 4 % del tiempo.
- CPU de PostgreSQL: 36 % de media y 96 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 316 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **200 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.8 s.
- Correo: el servidor de pruebas recibió 748 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 185 pausas, 22.7 s en total (6 % del tiempo de vida de la JVM); 23 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 971 MB (tope -Xmx1g).
- Clases cargadas: 25541 en total, 216 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 18 | 18 | 4522 |
| 0.2 | 81 | 82 | 16120 |
| 0.5 | 115 | 117 | 22512 |
| 1.4 | 129 | 131 | 24554 |
| 2.2 | 132 | 134 | 24982 |
| 2.7 | 133 | 136 | 25292 |
| 3.4 | 134 | 137 | 25376 |
| 3.9 | 134 | 137 | 25393 |
| 4.2 | 134 | 137 | 25467 |
| 4.5 | 135 | 138 | 25468 |
| 5.0 | 135 | 138 | 25468 |
| 5.3 | 135 | 139 | 25509 |
| 5.6 | 136 | 139 | 25511 |
| 5.9 | 136 | 139 | 25513 |
| 6.5 | 136 | 139 | 25541 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 205 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 212 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 748 | 748 | coincide |
| Mascotas | 748 | 748 | coincide |
| Citas agendadas | 734 | 734 | coincide |
| Diagnósticos | 377 | 377 | coincide |
| Recetas | 377 | 377 | coincide |
| Pagos | 239 | 239 | coincide |
| Citas completadas | 377 | 377 | coincide |
| Citas canceladas | 152 | 152 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 748 | 0 | 0.0 % | 30 | 145 |
| Registrar mascota | 748 | 0 | 0.0 % | 16 | 55 |
| Agendar cita | 734 | 14 | 1.9 % | 23 | 72 |
| Recepción (llegada) | 381 | 0 | 0.0 % | 14 | 59 |
| Iniciar atención | 377 | 4 | 1.0 % | 19 | 54 |
| Leer la consulta | 377 | 0 | 0.0 % | 11 | 33 |
| Registrar diagnóstico | 377 | 0 | 0.0 % | 9 | 33 |
| Registrar receta | 377 | 0 | 0.0 % | 10 | 34 |
| Actualizar mascota | 377 | 0 | 0.0 % | 10 | 31 |
| Cerrar la consulta | 377 | 0 | 0.0 % | 18 | 51 |
| Caja: crear punto de cobro | 150 | 0 | 0.0 % | 10 | 81 |
| Caja: vincular equipo | 150 | 0 | 0.0 % | 9 | 32 |
| Caja: abrir la caja | 150 | 0 | 0.0 % | 11 | 38 |
| Cobrar | 239 | 0 | 0.0 % | 11 | 54 |
| Cancelar cita | 152 | 0 | 0.0 % | 18 | 100 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 941 | 0 (0 %) | 121 | 402 | 23 % |
| reporte_clinico | 62 | 0 (0 %) | 1 470 | 2 929 | 15 % |
| login | 200 | 0 (0 %) | 284 | 796 | 11 % |
| propietarios | 646 | 0 (0 %) | 48 | 168 | 6 % |
| apoderado | 748 | 0 (0 %) | 30 | 145 | 5 % |
| citas_del_mes | 646 | 0 (0 %) | 24 | 153 | 4 % |
| privacidad | 941 | 0 (0 %) | 19 | 61 | 4 % |
| lista_de_historias | 530 | 0 (0 %) | 28 | 156 | 4 % |
| cita | 748 | 14 (2 %) | 23 | 72 | 3 % |
| mascota | 748 | 0 (0 %) | 16 | 55 | 2 % |
| navegacion | 941 | 0 (0 %) | 9 | 45 | 2 % |
| historia | 530 | 0 (0 %) | 18 | 56 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 770 | 0 % |
| 0.1 | 22 | 134 | 324 | 0 % |
| 0.4 | 37 | 179 | 411 | 0 % |
| 0.7 | 51 | 234 | 237 | 0 % |
| 0.9 | 65 | 301 | 222 | 0 % |
| 1.1 | 79 | 322 | 226 | 0 % |
| 1.4 | 94 | 422 | 133 | 0 % |
| 1.6 | 108 | 451 | 158 | 0 % |
| 1.9 | 122 | 589 | 152 | 0 % |
| 2.1 | 137 | 603 | 164 | 0 % |
| 2.4 | 151 | 646 | 130 | 0 % |
| 2.6 | 165 | 721 | 136 | 0 % |
| 2.9 | 179 | 753 | 193 | 0 % |
| 3.1 | 194 | 802 | 174 | 0 % |
| 3.4 | 200 | 796 | 234 | 0 % |
| 3.6 | 200 | 787 | 220 | 0 % |
| 3.9 | 200 | 766 | 93 | 0 % |
| 4.2 | 200 | 770 | 125 | 0 % |
| 4.4 | 200 | 785 | 138 | 0 % |
| 4.7 | 200 | 868 | 260 | 0 % |
| 4.9 | 200 | 827 | 103 | 0 % |
| 5.2 | 200 | 760 | 162 | 0 % |
| 5.4 | 200 | 751 | 144 | 0 % |
| 5.7 | 200 | 824 | 100 | 0 % |
| 5.9 | 200 | 789 | 104 | 0 % |
| 6.2 | 173 | 322 | 80 | 0 % |
| 6.4 | 92 | 34 | 96 | 0 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
