# Fase 3b: 250 usuarios, con y sin -Xms (1 CPU, 2 GB)

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
| Veredicto | DEGRADADO | DEGRADADO |
| Errores (%) | 1.2 % | 0.7 % |
| Ritmo medio (peticiones/s) | 32.3 | 38.3 |
| Tiempo mediano (ms) | 1 092 | 201 |
| Tiempo p95 (ms) | 4 138 | 2 714 |
| Usuarios sin degradación | 8 | 250 |
| CPU del backend, media (% del límite) | 57 | 62 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 78 | 64 |
| Memoria pico (MB) | 1 302 | 1 316 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 410 | 226 |
| Pausas completas | 40 | 30 |
| Pausas completas por "Allocation Failure" | 31 | 16 |
| Tiempo en pausas completas (s) | 14.0 | 13.9 |
| Tiempo en GC / vida de la JVM | 7.7 % | 7.3 % |
| Heap tras GC, máximo (MB) | 940 | 955 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4692; fallidas: 169.
- Acciones hechas: panel 710, agenda 635, historia 514, reportes 66, recepcion 771, atencion 387, cobro 148, cancelar 148; sin trabajo disponible: 572.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 78 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 302 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **8 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.6 s.
- Correo: el servidor de pruebas recibió 771 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.2 %).
- GC: 410 pausas, 36.9 s en total (8 % del tiempo de vida de la JVM); 40 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 940 MB (tope -Xmx1g).
- Clases cargadas: 25529 en total, 153 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 18 | 18 | 4459 |
| 0.1 | 39 | 40 | 8797 |
| 0.2 | 75 | 76 | 15421 |
| 0.3 | 80 | 81 | 16103 |
| 0.4 | 86 | 87 | 17103 |
| 0.7 | 115 | 117 | 22364 |
| 1.9 | 130 | 133 | 24738 |
| 2.5 | 132 | 135 | 25068 |
| 3.3 | 134 | 136 | 25311 |
| 4.4 | 135 | 138 | 25393 |
| 5.1 | 135 | 138 | 25407 |
| 5.8 | 136 | 138 | 25469 |
| 6.5 | 136 | 139 | 25470 |
| 7.9 | 136 | 139 | 25529 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 146 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 151 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 960 | 998.6 | 1040.19 | 53 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 960 | 164.9 | 171.76 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 132 | 116.1 | 879.21 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 960 | 96.2 | 100.2 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 960 | 76.9 | 80.07 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 960 | 74.2 | 77.32 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 960 | 73.5 | 76.52 | 4 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 960 | 49.9 | 52.01 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 514 | 46.2 | 89.88 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 1595 | 44.7 | 28.02 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 771 | 771 | coincide |
| Mascotas | 771 | 771 | coincide |
| Citas agendadas | 760 | 760 | coincide |
| Diagnósticos | 229 | 229 | coincide |
| Recetas | 229 | 229 | coincide |
| Pagos | 148 | 148 | coincide |
| Citas completadas | 229 | 229 | coincide |
| Citas canceladas | 148 | 148 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 771 | 0 | 0.0 % | 1 139 | 4 174 |
| Registrar mascota | 771 | 0 | 0.0 % | 979 | 3 950 |
| Agendar cita | 760 | 11 | 1.4 % | 1 108 | 4 040 |
| Recepción (llegada) | 387 | 0 | 0.0 % | 1 233 | 4 152 |
| Iniciar atención | 229 | 158 | 40.8 % | 1 488 | 4 877 |
| Leer la consulta | 229 | 0 | 0.0 % | 680 | 3 755 |
| Registrar diagnóstico | 229 | 0 | 0.0 % | 924 | 3 766 |
| Registrar receta | 229 | 0 | 0.0 % | 848 | 3 801 |
| Actualizar mascota | 229 | 0 | 0.0 % | 741 | 4 102 |
| Cerrar la consulta | 229 | 0 | 0.0 % | 962 | 3 443 |
| Caja: crear punto de cobro | 111 | 0 | 0.0 % | 827 | 3 866 |
| Caja: vincular equipo | 111 | 0 | 0.0 % | 1 038 | 3 358 |
| Caja: abrir la caja | 111 | 0 | 0.0 % | 1 034 | 4 211 |
| Cobrar | 148 | 0 | 0.0 % | 1 287 | 3 691 |
| Cancelar cita | 148 | 0 | 0.0 % | 1 142 | 3 518 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 960 | 0 (0 %) | 1 888 | 4 659 | 9 % |
| privacidad | 960 | 0 (0 %) | 1 798 | 3 834 | 9 % |
| apoderado | 771 | 0 (0 %) | 1 139 | 4 174 | 5 % |
| propietarios | 635 | 0 (0 %) | 1 471 | 4 340 | 5 % |
| cita | 771 | 11 (1 %) | 1 108 | 4 040 | 5 % |
| navegacion | 960 | 0 (0 %) | 718 | 3 641 | 5 % |
| avisos_de_cumplimiento | 960 | 0 (0 %) | 686 | 3 520 | 5 % |
| mascota | 771 | 0 (0 %) | 979 | 3 950 | 5 % |
| notificaciones | 960 | 0 (0 %) | 623 | 3 507 | 5 % |
| cumplimiento | 960 | 0 (0 %) | 575 | 3 561 | 5 % |
| citas_del_mes | 635 | 0 (0 %) | 1 160 | 3 951 | 5 % |
| empleados | 635 | 0 (0 %) | 1 068 | 4 164 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 937 | 0 % |
| 0.4 | 38 | 195 | 419 | 0 % |
| 0.9 | 68 | 298 | 375 | 0 % |
| 1.4 | 98 | 453 | 497 | 0 % |
| 1.9 | 128 | 502 | 955 | 0 % |
| 2.4 | 158 | 608 | 941 | 0 % |
| 2.9 | 188 | 570 | 3 185 | 1 % |
| 3.4 | 218 | 584 | 3 594 | 2 % |
| 3.9 | 248 | 622 | 4 187 | 2 % |
| 4.4 | 250 | 573 | 6 406 | 1 % |
| 4.9 | 250 | 595 | 3 958 | 2 % |
| 5.4 | 250 | 659 | 4 107 | 2 % |
| 5.9 | 250 | 700 | 2 307 | 2 % |
| 6.4 | 250 | 556 | 4 007 | 1 % |
| 6.9 | 213 | 282 | 795 | 0 % |

</details>

## CON -Xms = -Xmx: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 5820; fallidas: 127.
- Acciones hechas: panel 915, agenda 662, historia 586, reportes 79, recepcion 865, atencion 442, cobro 225, cancelar 157; sin trabajo disponible: 665.
- CPU del backend: 62 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 64 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 316 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.5 s.
- Correo: el servidor de pruebas recibió 865 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 226 pausas, 33.9 s en total (7 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 955 MB (tope -Xmx1g).
- Clases cargadas: 25556 en total, 215 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.1 | 18 | 18 | 4514 |
| 0.3 | 86 | 87 | 17103 |
| 1.3 | 128 | 130 | 24494 |
| 2.2 | 132 | 134 | 24991 |
| 3.0 | 133 | 136 | 25340 |
| 3.7 | 134 | 137 | 25391 |
| 4.1 | 134 | 137 | 25444 |
| 4.5 | 135 | 138 | 25467 |
| 4.9 | 135 | 138 | 25468 |
| 5.2 | 136 | 138 | 25468 |
| 5.7 | 136 | 139 | 25509 |
| 6.1 | 136 | 139 | 25536 |
| 6.6 | 136 | 139 | 25555 |
| 7.2 | 136 | 139 | 25556 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 204 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 211 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 865 | 865 | coincide |
| Mascotas | 865 | 865 | coincide |
| Citas agendadas | 857 | 857 | coincide |
| Diagnósticos | 323 | 323 | coincide |
| Recetas | 323 | 323 | coincide |
| Pagos | 225 | 225 | coincide |
| Citas completadas | 323 | 323 | coincide |
| Citas canceladas | 157 | 157 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 865 | 0 | 0.0 % | 237 | 2 595 |
| Registrar mascota | 865 | 0 | 0.0 % | 106 | 2 503 |
| Agendar cita | 857 | 8 | 0.9 % | 158 | 2 461 |
| Recepción (llegada) | 442 | 0 | 0.0 % | 317 | 2 875 |
| Iniciar atención | 323 | 119 | 26.9 % | 320 | 3 213 |
| Leer la consulta | 323 | 0 | 0.0 % | 66 | 2 344 |
| Registrar diagnóstico | 323 | 0 | 0.0 % | 64 | 2 295 |
| Registrar receta | 323 | 0 | 0.0 % | 67 | 2 448 |
| Actualizar mascota | 323 | 0 | 0.0 % | 58 | 2 395 |
| Cerrar la consulta | 323 | 0 | 0.0 % | 91 | 2 196 |
| Caja: crear punto de cobro | 157 | 0 | 0.0 % | 182 | 2 826 |
| Caja: vincular equipo | 157 | 0 | 0.0 % | 122 | 2 641 |
| Caja: abrir la caja | 157 | 0 | 0.0 % | 157 | 2 432 |
| Cobrar | 225 | 0 | 0.0 % | 381 | 2 809 |
| Cancelar cita | 157 | 0 | 0.0 % | 395 | 2 865 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1165 | 0 (0 %) | 825 | 3 383 | 11 % |
| privacidad | 1165 | 0 (0 %) | 150 | 3 180 | 9 % |
| propietarios | 662 | 0 (0 %) | 472 | 3 033 | 5 % |
| apoderado | 865 | 0 (0 %) | 237 | 2 595 | 5 % |
| navegacion | 1165 | 0 (0 %) | 74 | 2 067 | 5 % |
| mascota | 865 | 0 (0 %) | 106 | 2 503 | 5 % |
| cita | 865 | 8 (1 %) | 158 | 2 461 | 5 % |
| notificaciones | 1165 | 0 (0 %) | 43 | 2 169 | 4 % |
| citas_del_mes | 662 | 0 (0 %) | 369 | 2 681 | 4 % |
| avisos_de_cumplimiento | 1165 | 0 (0 %) | 45 | 1 987 | 4 % |
| lista_de_historias | 586 | 0 (0 %) | 477 | 2 908 | 4 % |
| cumplimiento | 1165 | 0 (0 %) | 51 | 2 060 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 887 | 0 % |
| 0.4 | 36 | 172 | 375 | 1 % |
| 0.9 | 66 | 275 | 286 | 0 % |
| 1.4 | 96 | 446 | 199 | 0 % |
| 1.9 | 126 | 578 | 241 | 0 % |
| 2.4 | 156 | 650 | 253 | 0 % |
| 2.9 | 186 | 782 | 540 | 0 % |
| 3.4 | 216 | 875 | 563 | 0 % |
| 3.9 | 246 | 900 | 1 354 | 1 % |
| 4.4 | 250 | 797 | 1 574 | 1 % |
| 4.9 | 250 | 683 | 3 297 | 1 % |
| 5.4 | 250 | 606 | 3 537 | 1 % |
| 5.9 | 250 | 616 | 2 875 | 1 % |
| 6.4 | 250 | 617 | 3 176 | 3 % |
| 6.9 | 213 | 387 | 1 424 | 1 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
