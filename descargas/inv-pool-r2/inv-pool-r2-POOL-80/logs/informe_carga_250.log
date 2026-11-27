# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Peticiones hechas | 20 009 |
| Peticiones correctas (2xx) | 19 991 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.7 |
| Tiempo mediano (ms) | 22 |
| Tiempo p95 (ms) | 212 |
| Tiempo p99 (ms) | 777 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 62 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 36 |
| Memoria pico (MB, de 2048) | 1 282 |
| Sentencias SQL | 542 912 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 20 009 | 44.7 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 9 / 12 | 0 % | 275 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7327; fallidas: 18.
- Acciones hechas: panel 997, agenda 818, historia 732, reportes 94, recepcion 983, atencion 476, cobro 323, cancelar 196; sin trabajo disponible: 734.
- CPU del backend: 62 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 36 % de media y 99 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 282 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 983 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 484 pausas, 44.6 s en total (10 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 920 MB (tope -Xmx1g).
- Clases cargadas: 25561 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4699 |
| 0.1 | 50 | 50 | 11014 |
| 0.2 | 80 | 81 | 16081 |
| 0.3 | 83 | 83 | 16486 |
| 0.5 | 102 | 104 | 19981 |
| 1.9 | 131 | 133 | 24828 |
| 2.9 | 133 | 136 | 25312 |
| 3.5 | 134 | 137 | 25380 |
| 4.0 | 134 | 137 | 25398 |
| 4.7 | 135 | 138 | 25472 |
| 5.2 | 136 | 139 | 25474 |
| 5.8 | 136 | 139 | 25542 |
| 6.5 | 136 | 139 | 25560 |
| 7.3 | 136 | 139 | 25561 |

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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 63.4 | 337.32 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1247 | 42.5 | 34.06 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1247 | 36.3 | 29.09 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 732 | 24.9 | 34.07 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 17.6 | 93.75 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 818 | 11.9 | 14.58 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2065 | 11.3 | 5.49 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1247 | 7.2 | 5.8 | 3 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111377 | 3.2 | 0.03 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2065 | 3.2 | 1.56 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 983 | 983 | coincide |
| Mascotas | 983 | 983 | coincide |
| Citas agendadas | 970 | 970 | coincide |
| Diagnósticos | 471 | 471 | coincide |
| Recetas | 471 | 471 | coincide |
| Pagos | 323 | 323 | coincide |
| Citas completadas | 471 | 471 | coincide |
| Citas canceladas | 196 | 196 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 983 | 0 | 0.0 % | 42 | 266 | 654 |
| Registrar mascota | 983 | 0 | 0.0 % | 23 | 73 | 200 |
| Agendar cita | 970 | 13 | 1.3 % | 33 | 109 | 168 |
| Recepción (llegada) | 476 | 0 | 0.0 % | 20 | 182 | 539 |
| Iniciar atención | 471 | 5 | 1.1 % | 29 | 81 | 134 |
| Leer la consulta | 471 | 0 | 0.0 % | 16 | 55 | 171 |
| Registrar diagnóstico | 471 | 0 | 0.0 % | 16 | 52 | 109 |
| Registrar receta | 471 | 0 | 0.0 % | 17 | 56 | 191 |
| Actualizar mascota | 471 | 0 | 0.0 % | 17 | 54 | 134 |
| Cerrar la consulta | 471 | 0 | 0.0 % | 30 | 84 | 201 |
| Caja: crear punto de cobro | 190 | 0 | 0.0 % | 15 | 87 | 237 |
| Caja: vincular equipo | 190 | 0 | 0.0 % | 13 | 35 | 76 |
| Caja: abrir la caja | 190 | 0 | 0.0 % | 17 | 46 | 770 |
| Cobrar | 323 | 0 | 0.0 % | 23 | 123 | 247 |
| Cancelar cita | 196 | 0 | 0.0 % | 22 | 166 | 762 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 525 | 5 279 | 6 231 | 20 % |
| panel | 1247 | 0 (0 %) | 90 | 471 | 948 | 14 % |
| login | 250 | 0 (0 %) | 390 | 1 751 | 2 707 | 12 % |
| propietarios | 818 | 0 (0 %) | 78 | 274 | 639 | 6 % |
| apoderado | 983 | 0 (0 %) | 42 | 266 | 654 | 6 % |
| lista_de_historias | 732 | 0 (0 %) | 46 | 304 | 726 | 5 % |
| citas_del_mes | 818 | 0 (0 %) | 41 | 246 | 723 | 5 % |
| navegacion | 1247 | 0 (0 %) | 12 | 150 | 496 | 3 % |
| cita | 983 | 13 (1 %) | 33 | 109 | 168 | 3 % |
| mascota | 983 | 0 (0 %) | 23 | 73 | 200 | 2 % |
| privacidad | 1247 | 0 (0 %) | 13 | 78 | 224 | 2 % |
| historia | 732 | 0 (0 %) | 30 | 88 | 186 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 1 | 421 | 421 | 0 % |
| 0.3 | 30 | 167 | 548 | 1 428 | 1 % |
| 0.8 | 60 | 271 | 283 | 904 | 0 % |
| 1.3 | 90 | 388 | 184 | 506 | 0 % |
| 1.8 | 120 | 551 | 130 | 371 | 0 % |
| 2.3 | 150 | 652 | 182 | 1 514 | 0 % |
| 2.8 | 180 | 779 | 122 | 576 | 0 % |
| 3.3 | 210 | 881 | 120 | 611 | 0 % |
| 3.8 | 240 | 1010 | 335 | 1 751 | 0 % |
| 4.3 | 250 | 996 | 377 | 895 | 0 % |
| 4.8 | 250 | 946 | 147 | 517 | 0 % |
| 5.3 | 250 | 981 | 92 | 167 | 0 % |
| 5.8 | 250 | 960 | 284 | 571 | 0 % |
| 6.3 | 250 | 985 | 116 | 386 | 0 % |
| 6.8 | 237 | 521 | 159 | 829 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
