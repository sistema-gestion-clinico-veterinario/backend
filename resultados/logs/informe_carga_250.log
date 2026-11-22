# Fase 3b: 250 usuarios, con y sin -Xms (1 CPU, 2 GB)

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
| Veredicto | DEGRADADO | DEGRADADO |
| Errores (%) | 1.1 % | 0.6 % |
| Ritmo medio (peticiones/s) | 33.3 | 39.5 |
| Tiempo mediano (ms) | 825 | 95 |
| Tiempo p95 (ms) | 4 012 | 2 316 |
| Usuarios sin degradación | 164 | 228 |
| CPU del backend, media (% del límite) | 55 | 53 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 74 | 60 |
| Memoria pico (MB) | 1 317 | 1 356 |
| Capacidad del heap en la primera pausa de GC (MB) | 30 | 989 |
| Pausas de GC en total | 473 | 233 |
| Pausas completas | 52 | 31 |
| Pausas completas por "Allocation Failure" | 41 | 17 |
| Tiempo en pausas completas (s) | 18.2 | 11.7 |
| Tiempo en GC / vida de la JVM | 10.0 % | 7.6 % |
| Heap tras GC, máximo (MB) | 952 | 981 |
| Avisos de pool agotado | 0 | 0 |

## SIN -Xms: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4997; fallidas: 158.
- Acciones hechas: panel 736, agenda 665, historia 474, reportes 80, recepcion 829, atencion 388, cobro 159, cancelar 175; sin trabajo disponible: 600.
- CPU del backend: 55 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 74 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 317 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **164 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 7.9 s.
- Correo: el servidor de pruebas recibió 829 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.3 %).
- GC: 473 pausas, 45.0 s en total (10 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 952 MB (tope -Xmx1g).
- Clases cargadas: 25531 en total, 184 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 18 | 19 | 4663 |
| 0.1 | 49 | 50 | 10927 |
| 0.2 | 80 | 81 | 16064 |
| 0.3 | 80 | 81 | 16113 |
| 0.3 | 87 | 88 | 17206 |
| 1.3 | 127 | 130 | 24252 |
| 2.4 | 132 | 135 | 25087 |
| 3.1 | 133 | 136 | 25312 |
| 3.7 | 134 | 137 | 25379 |
| 4.6 | 135 | 138 | 25409 |
| 5.4 | 135 | 138 | 25471 |
| 6.2 | 136 | 139 | 25472 |
| 6.6 | 136 | 139 | 25473 |
| 7.2 | 136 | 139 | 25531 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 177 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 182 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 986 | 826.4 | 838.17 | 50 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 986 | 147.6 | 149.72 | 9 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 160 | 131.0 | 819.06 | 8 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 986 | 95.1 | 96.42 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 986 | 69.4 | 70.36 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 986 | 69.2 | 70.23 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 986 | 59.9 | 60.77 | 4 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 986 | 43.3 | 43.88 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 160 | 34.7 | 216.98 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 474 | 33.8 | 71.34 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 829 | 829 | coincide |
| Mascotas | 829 | 829 | coincide |
| Citas agendadas | 817 | 817 | coincide |
| Diagnósticos | 242 | 242 | coincide |
| Recetas | 242 | 242 | coincide |
| Pagos | 159 | 159 | coincide |
| Citas completadas | 242 | 242 | coincide |
| Citas canceladas | 175 | 175 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 829 | 0 | 0.0 % | 791 | 3 607 |
| Registrar mascota | 829 | 0 | 0.0 % | 728 | 3 653 |
| Agendar cita | 817 | 12 | 1.4 % | 702 | 3 257 |
| Recepción (llegada) | 388 | 0 | 0.0 % | 892 | 3 953 |
| Iniciar atención | 242 | 146 | 37.6 % | 899 | 4 276 |
| Leer la consulta | 242 | 0 | 0.0 % | 609 | 3 048 |
| Registrar diagnóstico | 242 | 0 | 0.0 % | 615 | 3 509 |
| Registrar receta | 242 | 0 | 0.0 % | 536 | 3 284 |
| Actualizar mascota | 242 | 0 | 0.0 % | 505 | 3 272 |
| Cerrar la consulta | 242 | 0 | 0.0 % | 687 | 2 973 |
| Caja: crear punto de cobro | 116 | 0 | 0.0 % | 790 | 4 186 |
| Caja: vincular equipo | 116 | 0 | 0.0 % | 481 | 3 824 |
| Caja: abrir la caja | 116 | 0 | 0.0 % | 512 | 4 003 |
| Cobrar | 159 | 0 | 0.0 % | 793 | 3 964 |
| Cancelar cita | 175 | 0 | 0.0 % | 1 020 | 3 957 |

**Endpoints que más tiempo acumulan, SIN -Xms:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 986 | 0 (0 %) | 1 546 | 4 604 | 10 % |
| privacidad | 986 | 0 (0 %) | 1 523 | 3 710 | 9 % |
| navegacion | 986 | 0 (0 %) | 564 | 3 629 | 5 % |
| propietarios | 665 | 0 (0 %) | 1 218 | 4 115 | 5 % |
| avisos_de_cumplimiento | 986 | 0 (0 %) | 447 | 3 710 | 5 % |
| mascota | 829 | 0 (0 %) | 728 | 3 653 | 5 % |
| apoderado | 829 | 0 (0 %) | 791 | 3 607 | 5 % |
| citas_del_mes | 665 | 0 (0 %) | 989 | 4 317 | 5 % |
| notificaciones | 986 | 0 (0 %) | 359 | 3 665 | 5 % |
| cita | 829 | 12 (1 %) | 702 | 3 257 | 5 % |
| empleados | 665 | 0 (0 %) | 975 | 3 795 | 5 % |
| cumplimiento | 986 | 0 (0 %) | 315 | 3 141 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 110 | 338 | 0 % |
| 0.5 | 44 | 220 | 233 | 0 % |
| 1.0 | 74 | 317 | 233 | 0 % |
| 1.5 | 104 | 446 | 261 | 0 % |
| 2.0 | 134 | 559 | 557 | 0 % |
| 2.5 | 164 | 510 | 1 826 | 0 % |
| 3.0 | 194 | 629 | 2 062 | 1 % |
| 3.5 | 224 | 515 | 4 254 | 2 % |
| 4.0 | 250 | 583 | 6 725 | 2 % |
| 4.5 | 250 | 664 | 3 031 | 2 % |
| 5.0 | 250 | 718 | 3 952 | 2 % |
| 5.5 | 250 | 702 | 2 907 | 2 % |
| 6.0 | 250 | 653 | 5 198 | 2 % |
| 6.5 | 250 | 712 | 1 843 | 1 % |
| 7.0 | 170 | 74 | 112 | 0 % |

</details>

## CON -Xms = -Xmx: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 6165; fallidas: 102.
- Acciones hechas: panel 910, agenda 743, historia 573, reportes 80, recepcion 885, atencion 452, cobro 241, cancelar 186; sin trabajo disponible: 681.
- CPU del backend: 53 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 60 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 356 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **228 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 9.7 s.
- Correo: el servidor de pruebas recibió 885 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.3 %).
- GC: 233 pausas, 34.0 s en total (8 % del tiempo de vida de la JVM); 31 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 981 MB (tope -Xmx1g).
- Clases cargadas: 25561 en total, 211 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 18 | 18 | 4506 |
| 0.3 | 86 | 87 | 17104 |
| 1.3 | 127 | 129 | 24162 |
| 2.1 | 132 | 134 | 24993 |
| 2.8 | 133 | 136 | 25297 |
| 3.1 | 134 | 137 | 25349 |
| 3.8 | 134 | 137 | 25398 |
| 4.3 | 135 | 138 | 25473 |
| 4.6 | 135 | 138 | 25473 |
| 4.9 | 135 | 138 | 25473 |
| 5.5 | 136 | 139 | 25474 |
| 5.9 | 136 | 139 | 25515 |
| 6.4 | 136 | 139 | 25560 |
| 6.7 | 136 | 139 | 25561 |
| 7.1 | 136 | 139 | 25561 |

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

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 885 | 885 | coincide |
| Mascotas | 885 | 885 | coincide |
| Citas agendadas | 876 | 876 | coincide |
| Diagnósticos | 359 | 359 | coincide |
| Recetas | 359 | 359 | coincide |
| Pagos | 241 | 241 | coincide |
| Citas completadas | 359 | 359 | coincide |
| Citas canceladas | 186 | 186 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 885 | 0 | 0.0 % | 111 | 2 304 |
| Registrar mascota | 885 | 0 | 0.0 % | 52 | 2 427 |
| Agendar cita | 876 | 9 | 1.0 % | 77 | 2 107 |
| Recepción (llegada) | 452 | 0 | 0.0 % | 160 | 2 399 |
| Iniciar atención | 359 | 93 | 20.6 % | 112 | 2 345 |
| Leer la consulta | 359 | 0 | 0.0 % | 38 | 1 730 |
| Registrar diagnóstico | 359 | 0 | 0.0 % | 32 | 1 727 |
| Registrar receta | 359 | 0 | 0.0 % | 34 | 1 779 |
| Actualizar mascota | 359 | 0 | 0.0 % | 33 | 1 579 |
| Cerrar la consulta | 359 | 0 | 0.0 % | 53 | 1 754 |
| Caja: crear punto de cobro | 162 | 0 | 0.0 % | 110 | 1 767 |
| Caja: vincular equipo | 162 | 0 | 0.0 % | 49 | 2 050 |
| Caja: abrir la caja | 162 | 0 | 0.0 % | 68 | 2 657 |
| Cobrar | 241 | 0 | 0.0 % | 210 | 2 473 |
| Cancelar cita | 186 | 0 | 0.0 % | 173 | 2 140 |

**Endpoints que más tiempo acumulan, CON -Xms = -Xmx:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1160 | 0 (0 %) | 408 | 2 806 | 10 % |
| privacidad | 1160 | 0 (0 %) | 110 | 2 654 | 9 % |
| navegacion | 1160 | 0 (0 %) | 23 | 2 091 | 5 % |
| apoderado | 885 | 0 (0 %) | 111 | 2 304 | 5 % |
| propietarios | 743 | 0 (0 %) | 222 | 2 612 | 5 % |
| mascota | 885 | 0 (0 %) | 52 | 2 427 | 5 % |
| avisos_de_cumplimiento | 1160 | 0 (0 %) | 20 | 1 867 | 5 % |
| cumplimiento | 1160 | 0 (0 %) | 26 | 1 817 | 4 % |
| cita | 885 | 9 (1 %) | 77 | 2 107 | 4 % |
| citas_del_mes | 743 | 0 (0 %) | 117 | 2 256 | 4 % |
| notificaciones | 1160 | 0 (0 %) | 18 | 1 965 | 4 % |
| lista_de_historias | 573 | 0 (0 %) | 227 | 2 828 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 14 | 1 116 | 0 % |
| 0.3 | 33 | 169 | 344 | 1 % |
| 0.8 | 63 | 322 | 226 | 0 % |
| 1.3 | 93 | 448 | 214 | 0 % |
| 1.8 | 123 | 514 | 198 | 0 % |
| 2.3 | 153 | 695 | 468 | 0 % |
| 2.8 | 183 | 797 | 323 | 0 % |
| 3.3 | 213 | 900 | 194 | 0 % |
| 3.8 | 243 | 733 | 2 737 | 1 % |
| 4.3 | 250 | 782 | 6 938 | 1 % |
| 4.8 | 250 | 691 | 3 252 | 2 % |
| 5.3 | 250 | 872 | 1 196 | 1 % |
| 5.8 | 250 | 736 | 2 054 | 1 % |
| 6.3 | 250 | 813 | 1 612 | 1 % |
| 6.8 | 229 | 509 | 746 | 0 % |

</details>

> Las dos variantes corrieron en la misma máquina, una detrás de otra, y el orden alterna por nivel. La base conserva lo escrito antes, así que la segunda variante parte con algo más de datos. Una corrida por variante y nivel.
