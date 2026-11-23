# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 250 |
| Peticiones hechas | 19 874 |
| Peticiones correctas (2xx) | 19 848 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 30 |
| Tiempo p95 (ms) | 310 |
| Tiempo p99 (ms) | 931 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 66 |
| Tiempo con la CPU al tope (%) | 17 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 42 |
| Memoria pico (MB, de 2048) | 1 314 |
| Sentencias SQL | 541 373 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 874 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 9 / 12 | 0 % | 287 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7256; fallidas: 26.
- Acciones hechas: panel 991, agenda 810, historia 728, reportes 94, recepcion 977, atencion 474, cobro 316, cancelar 195; sin trabajo disponible: 733.
- CPU del backend: 66 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 17 % del tiempo.
- CPU de PostgreSQL: 42 % de media y 95 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 314 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 977 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 460 pausas, 48.8 s en total (10 % del tiempo de vida de la JVM); 45 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 956 MB (tope -Xmx1g).
- Clases cargadas: 25565 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4660 |
| 0.1 | 49 | 50 | 10951 |
| 0.2 | 80 | 81 | 16072 |
| 0.3 | 82 | 83 | 16403 |
| 0.5 | 94 | 95 | 18104 |
| 1.8 | 130 | 133 | 24757 |
| 2.7 | 133 | 135 | 25187 |
| 3.5 | 134 | 137 | 25384 |
| 4.1 | 134 | 137 | 25402 |
| 4.8 | 135 | 138 | 25476 |
| 5.4 | 136 | 139 | 25516 |
| 6.1 | 136 | 139 | 25561 |
| 6.7 | 136 | 139 | 25563 |
| 7.6 | 136 | 139 | 25565 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 159 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 77.5 | 412.22 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1241 | 56.1 | 45.23 | 17 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1241 | 43.8 | 35.25 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 728 | 31.0 | 42.6 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 25.3 | 134.4 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1241 | 19.0 | 15.31 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2051 | 16.6 | 8.07 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2051 | 5.0 | 2.46 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2051 | 4.8 | 2.35 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110368 | 4.1 | 0.04 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 977 | 977 | coincide |
| Mascotas | 977 | 977 | coincide |
| Citas agendadas | 960 | 960 | coincide |
| Diagnósticos | 465 | 465 | coincide |
| Recetas | 465 | 465 | coincide |
| Pagos | 316 | 316 | coincide |
| Citas completadas | 465 | 465 | coincide |
| Citas canceladas | 195 | 195 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 977 | 0 | 0.0 % | 53 | 327 | 715 |
| Registrar mascota | 977 | 0 | 0.0 % | 30 | 152 | 367 |
| Agendar cita | 960 | 17 | 1.7 % | 43 | 203 | 384 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 28 | 289 | 664 |
| Iniciar atención | 465 | 9 | 1.9 % | 38 | 189 | 343 |
| Leer la consulta | 465 | 0 | 0.0 % | 24 | 74 | 445 |
| Registrar diagnóstico | 465 | 0 | 0.0 % | 24 | 121 | 352 |
| Registrar receta | 465 | 0 | 0.0 % | 25 | 110 | 249 |
| Actualizar mascota | 465 | 0 | 0.0 % | 23 | 110 | 263 |
| Cerrar la consulta | 465 | 0 | 0.0 % | 40 | 182 | 416 |
| Caja: crear punto de cobro | 189 | 0 | 0.0 % | 22 | 197 | 509 |
| Caja: vincular equipo | 189 | 0 | 0.0 % | 17 | 51 | 634 |
| Caja: abrir la caja | 189 | 0 | 0.0 % | 24 | 118 | 431 |
| Cobrar | 316 | 0 | 0.0 % | 32 | 172 | 645 |
| Cancelar cita | 195 | 0 | 0.0 % | 33 | 307 | 515 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 3 255 | 6 449 | 8 739 | 19 % |
| panel | 1241 | 0 (0 %) | 131 | 584 | 996 | 14 % |
| login | 250 | 0 (0 %) | 522 | 2 629 | 4 360 | 12 % |
| propietarios | 810 | 0 (0 %) | 108 | 399 | 725 | 7 % |
| apoderado | 977 | 0 (0 %) | 53 | 327 | 715 | 5 % |
| lista_de_historias | 728 | 0 (0 %) | 70 | 416 | 789 | 5 % |
| citas_del_mes | 810 | 0 (0 %) | 46 | 323 | 668 | 4 % |
| cita | 977 | 17 (2 %) | 43 | 203 | 384 | 4 % |
| navegacion | 1241 | 0 (0 %) | 16 | 202 | 582 | 3 % |
| privacidad | 1241 | 0 (0 %) | 20 | 166 | 324 | 3 % |
| mascota | 977 | 0 (0 %) | 30 | 152 | 367 | 3 % |
| historia | 728 | 0 (0 %) | 34 | 161 | 282 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 49 | 382 | 412 | 0 % |
| 0.4 | 36 | 183 | 388 | 917 | 1 % |
| 0.9 | 66 | 293 | 324 | 1 095 | 0 % |
| 1.4 | 96 | 420 | 144 | 603 | 0 % |
| 1.9 | 126 | 549 | 133 | 472 | 0 % |
| 2.4 | 156 | 632 | 207 | 885 | 0 % |
| 2.9 | 186 | 755 | 292 | 897 | 0 % |
| 3.4 | 216 | 946 | 137 | 703 | 0 % |
| 3.9 | 246 | 1071 | 256 | 1 335 | 0 % |
| 4.4 | 250 | 940 | 290 | 715 | 0 % |
| 4.9 | 250 | 894 | 167 | 283 | 0 % |
| 5.4 | 250 | 978 | 256 | 672 | 0 % |
| 5.9 | 250 | 885 | 452 | 866 | 0 % |
| 6.4 | 250 | 945 | 207 | 319 | 0 % |
| 6.9 | 207 | 263 | 118 | 1 253 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
