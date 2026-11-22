# Fase 3b: 200 usuarios, con y sin -Xms (1 CPU, 2 GB)

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
| Veredicto | AGUANTA | AGUANTA |
| Errores (%) | 0.5 % | 0.1 % |
| Ritmo medio (peticiones/s) | 33.7 | 37.0 |
| Tiempo mediano (ms) | 78 | 28 |
| Tiempo p95 (ms) | 1 997 | 382 |
| Usuarios sin degradación | sin datos | 200 |
| CPU del backend, media (% del límite) | 53 | 59 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 64 | 51 |
| Memoria pico (MB) | 1 190 | 1 315 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 392 | 220 |
| Pausas completas | 38 | 29 |
| Pausas completas por "Allocation Failure" | 28 | 15 |
| Tiempo en pausas completas (s) | 12.2 | 11.4 |
| Tiempo en GC / vida de la JVM | 7.2 % | 7.6 % |
| Heap tras GC, máximo (MB) | 827 | 970 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 4933; fallidas: 72.
- Acciones hechas: panel 685, agenda 549, historia 461, reportes 55, recepcion 698, atencion 346, cobro 192, cancelar 141; sin trabajo disponible: 522.
- CPU del backend: 53 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 64 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 190 MB (58 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- El peor tiempo p95 en un tramo de 15 s fue de 3.3 s.
- Correo: el servidor de pruebas recibió 698 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 392 pausas, 29.8 s en total (7 % del tiempo de vida de la JVM); 38 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 827 MB (tope -Xmx1g).
- Clases cargadas: 25541 en total, 197 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 17 | 18 | 4408 |
| 0.1 | 36 | 36 | 8105 |
| 0.2 | 71 | 72 | 14596 |
| 0.3 | 80 | 81 | 16104 |
| 0.3 | 86 | 87 | 17086 |
| 0.4 | 93 | 94 | 18035 |
| 1.4 | 129 | 131 | 24542 |
| 2.3 | 132 | 135 | 25034 |
| 2.9 | 133 | 136 | 25293 |
| 3.6 | 134 | 137 | 25393 |
| 4.5 | 135 | 138 | 25425 |
| 5.2 | 135 | 138 | 25470 |
| 6.0 | 136 | 139 | 25512 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 186 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 193 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 885 | 417.2 | 471.42 | 34 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 885 | 398.5 | 450.31 | 33 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 885 | 102.1 | 115.41 | 8 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 110 | 80.1 | 728.15 | 7 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 885 | 47.8 | 53.97 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 885 | 32.1 | 36.24 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 885 | 29.1 | 32.92 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 461 | 26.7 | 57.95 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 110 | 21.7 | 196.84 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 549 | 21.4 | 38.93 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 698 | 698 | coincide |
| Mascotas | 698 | 698 | coincide |
| Citas agendadas | 677 | 677 | coincide |
| Diagnósticos | 295 | 295 | coincide |
| Recetas | 295 | 295 | coincide |
| Pagos | 192 | 192 | coincide |
| Citas completadas | 295 | 295 | coincide |
| Citas canceladas | 141 | 141 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 698 | 0 | 0.0 % | 94 | 1 711 |
| Registrar mascota | 698 | 0 | 0.0 % | 51 | 1 732 |
| Agendar cita | 677 | 21 | 3.0 % | 74 | 1 562 |
| Recepción (llegada) | 346 | 0 | 0.0 % | 73 | 1 692 |
| Iniciar atención | 295 | 51 | 14.7 % | 79 | 1 630 |
| Leer la consulta | 295 | 0 | 0.0 % | 39 | 1 593 |
| Registrar diagnóstico | 295 | 0 | 0.0 % | 35 | 1 260 |
| Registrar receta | 295 | 0 | 0.0 % | 36 | 1 442 |
| Actualizar mascota | 295 | 0 | 0.0 % | 32 | 1 583 |
| Cerrar la consulta | 295 | 0 | 0.0 % | 52 | 1 388 |
| Caja: crear punto de cobro | 137 | 0 | 0.0 % | 124 | 1 618 |
| Caja: vincular equipo | 137 | 0 | 0.0 % | 52 | 943 |
| Caja: abrir la caja | 137 | 0 | 0.0 % | 60 | 1 514 |
| Cobrar | 192 | 0 | 0.0 % | 83 | 1 747 |
| Cancelar cita | 141 | 0 | 0.0 % | 79 | 1 811 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 885 | 0 (0 %) | 1 390 | 3 752 | 25 % |
| navegacion | 885 | 0 (0 %) | 30 | 1 627 | 5 % |
| apoderado | 698 | 0 (0 %) | 94 | 1 711 | 5 % |
| citas_del_mes | 549 | 0 (0 %) | 139 | 1 997 | 5 % |
| reporte_clinico | 55 | 0 (0 %) | 4 209 | 6 321 | 4 % |
| cita | 698 | 21 (3 %) | 74 | 1 562 | 4 % |
| mascota | 698 | 0 (0 %) | 51 | 1 732 | 4 % |
| lista_de_historias | 461 | 0 (0 %) | 152 | 1 717 | 4 % |
| propietarios | 549 | 0 (0 %) | 164 | 1 549 | 4 % |
| cumplimiento | 885 | 0 (0 %) | 30 | 1 195 | 3 % |
| avisos_de_cumplimiento | 885 | 0 (0 %) | 22 | 1 125 | 3 % |
| privacidad | 885 | 0 (0 %) | 22 | 981 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 74 | 689 | 1 % |
| 0.2 | 25 | 143 | 301 | 2 % |
| 0.5 | 39 | 182 | 359 | 1 % |
| 0.7 | 54 | 244 | 297 | 0 % |
| 0.9 | 68 | 315 | 305 | 0 % |
| 1.2 | 82 | 348 | 295 | 0 % |
| 1.4 | 96 | 420 | 367 | 0 % |
| 1.7 | 111 | 454 | 344 | 0 % |
| 1.9 | 125 | 541 | 299 | 0 % |
| 2.2 | 139 | 594 | 546 | 0 % |
| 2.5 | 154 | 705 | 754 | 0 % |
| 2.7 | 168 | 689 | 725 | 0 % |
| 3.0 | 182 | 618 | 2 131 | 0 % |
| 3.2 | 196 | 715 | 2 203 | 0 % |
| 3.5 | 200 | 659 | 2 482 | 1 % |
| 3.7 | 200 | 684 | 2 360 | 1 % |
| 4.0 | 200 | 712 | 1 651 | 0 % |
| 4.2 | 200 | 697 | 2 582 | 1 % |
| 4.5 | 200 | 697 | 1 925 | 0 % |
| 4.7 | 200 | 770 | 350 | 0 % |
| 5.0 | 200 | 639 | 2 298 | 0 % |
| 5.2 | 200 | 613 | 3 281 | 2 % |
| 5.5 | 200 | 572 | 3 340 | 2 % |
| 5.7 | 200 | 735 | 3 061 | 0 % |
| 6.0 | 200 | 596 | 88 | 0 % |
| 6.2 | 169 | 291 | 87 | 1 % |
| 6.5 | 71 | 3 | 23 | 0 % |

</details>

## CON -Xms = -Xmx: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5493; fallidas: 21.
- Acciones hechas: panel 756, agenda 608, historia 478, reportes 83, recepcion 739, atencion 360, cobro 241, cancelar 151; sin trabajo disponible: 551.
- CPU del backend: 59 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 51 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 315 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **200 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 739 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.8 %).
- GC: 220 pausas, 31.6 s en total (8 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 970 MB (tope -Xmx1g).
- Clases cargadas: 25558 en total, 213 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4514 |
| 0.3 | 86 | 87 | 17085 |
| 1.2 | 126 | 128 | 24140 |
| 2.2 | 131 | 134 | 24961 |
| 2.8 | 133 | 136 | 25270 |
| 3.4 | 134 | 137 | 25345 |
| 3.8 | 134 | 137 | 25393 |
| 4.2 | 134 | 138 | 25395 |
| 4.6 | 135 | 138 | 25469 |
| 4.9 | 135 | 138 | 25470 |
| 5.2 | 135 | 138 | 25470 |
| 5.4 | 136 | 139 | 25511 |
| 5.9 | 136 | 139 | 25528 |
| 6.3 | 136 | 139 | 25557 |
| 6.5 | 136 | 139 | 25557 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 202 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 209 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 739 | 739 | coincide |
| Mascotas | 739 | 739 | coincide |
| Citas agendadas | 728 | 728 | coincide |
| Diagnósticos | 350 | 350 | coincide |
| Recetas | 350 | 350 | coincide |
| Pagos | 241 | 241 | coincide |
| Citas completadas | 350 | 350 | coincide |
| Citas canceladas | 151 | 151 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 739 | 0 | 0.0 % | 54 | 307 |
| Registrar mascota | 739 | 0 | 0.0 % | 29 | 110 |
| Agendar cita | 728 | 11 | 1.5 % | 41 | 158 |
| Recepción (llegada) | 360 | 0 | 0.0 % | 24 | 199 |
| Iniciar atención | 350 | 10 | 2.8 % | 34 | 99 |
| Leer la consulta | 350 | 0 | 0.0 % | 19 | 55 |
| Registrar diagnóstico | 350 | 0 | 0.0 % | 18 | 79 |
| Registrar receta | 350 | 0 | 0.0 % | 21 | 75 |
| Actualizar mascota | 350 | 0 | 0.0 % | 19 | 51 |
| Cerrar la consulta | 350 | 0 | 0.0 % | 34 | 83 |
| Caja: crear punto de cobro | 145 | 0 | 0.0 % | 19 | 268 |
| Caja: vincular equipo | 145 | 0 | 0.0 % | 16 | 84 |
| Caja: abrir la caja | 145 | 0 | 0.0 % | 23 | 86 |
| Cobrar | 241 | 0 | 0.0 % | 27 | 93 |
| Cancelar cita | 151 | 0 | 0.0 % | 29 | 310 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 956 | 0 (0 %) | 310 | 1 114 | 29 % |
| reporte_clinico | 83 | 0 (0 %) | 2 202 | 4 794 | 15 % |
| login | 200 | 0 (0 %) | 404 | 1 279 | 8 % |
| propietarios | 608 | 0 (0 %) | 103 | 321 | 6 % |
| apoderado | 739 | 0 (0 %) | 54 | 307 | 5 % |
| privacidad | 956 | 0 (0 %) | 35 | 137 | 4 % |
| lista_de_historias | 478 | 0 (0 %) | 56 | 284 | 3 % |
| cita | 739 | 11 (1 %) | 41 | 158 | 3 % |
| citas_del_mes | 608 | 0 (0 %) | 37 | 224 | 3 % |
| navegacion | 956 | 0 (0 %) | 16 | 143 | 3 % |
| mascota | 739 | 0 (0 %) | 29 | 110 | 2 % |
| empleados | 608 | 0 (0 %) | 27 | 109 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 104 | 365 | 0 % |
| 0.2 | 28 | 149 | 465 | 0 % |
| 0.5 | 42 | 196 | 427 | 0 % |
| 0.8 | 56 | 256 | 501 | 0 % |
| 1.0 | 71 | 323 | 524 | 0 % |
| 1.2 | 85 | 343 | 479 | 0 % |
| 1.5 | 99 | 456 | 510 | 0 % |
| 1.8 | 114 | 497 | 271 | 0 % |
| 2.0 | 128 | 534 | 440 | 0 % |
| 2.2 | 142 | 623 | 357 | 0 % |
| 2.5 | 156 | 620 | 533 | 0 % |
| 2.8 | 171 | 696 | 890 | 0 % |
| 3.0 | 185 | 724 | 627 | 0 % |
| 3.2 | 199 | 887 | 326 | 0 % |
| 3.5 | 200 | 799 | 204 | 0 % |
| 3.8 | 200 | 794 | 315 | 0 % |
| 4.0 | 200 | 857 | 452 | 0 % |
| 4.2 | 200 | 763 | 239 | 0 % |
| 4.5 | 200 | 786 | 500 | 0 % |
| 4.8 | 200 | 784 | 268 | 0 % |
| 5.0 | 200 | 763 | 104 | 0 % |
| 5.2 | 200 | 843 | 271 | 0 % |
| 5.5 | 200 | 680 | 192 | 0 % |
| 5.8 | 200 | 756 | 215 | 1 % |
| 6.0 | 199 | 554 | 133 | 0 % |
| 6.2 | 145 | 170 | 97 | 0 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
