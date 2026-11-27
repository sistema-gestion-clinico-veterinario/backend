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
| Peticiones hechas | 19 936 |
| Peticiones correctas (2xx) | 19 907 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 21 |
| Tiempo p95 (ms) | 230 |
| Tiempo p99 (ms) | 776 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 62 |
| Tiempo con la CPU al tope (%) | 21 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 35 |
| Memoria pico (MB, de 2048) | 1 289 |
| Sentencias SQL | 542 811 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 936 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 283 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7246; fallidas: 29.
- Acciones hechas: panel 999, agenda 815, historia 730, reportes 94, recepcion 983, atencion 475, cobro 317, cancelar 198; sin trabajo disponible: 731.
- CPU del backend: 62 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 21 % del tiempo.
- CPU de PostgreSQL: 35 % de media y 101 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 289 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 983 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 458 pausas, 44.1 s en total (10 % del tiempo de vida de la JVM); 46 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 913 MB (tope -Xmx1g).
- Clases cargadas: 25564 en total, 165 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4660 |
| 0.1 | 49 | 50 | 10951 |
| 0.2 | 80 | 81 | 16057 |
| 0.3 | 86 | 87 | 17090 |
| 0.9 | 123 | 125 | 23723 |
| 2.2 | 131 | 134 | 24937 |
| 3.0 | 133 | 136 | 25315 |
| 3.7 | 134 | 137 | 25400 |
| 4.3 | 135 | 138 | 25475 |
| 4.9 | 135 | 138 | 25477 |
| 5.5 | 136 | 139 | 25516 |
| 6.1 | 136 | 139 | 25561 |
| 6.8 | 136 | 139 | 25563 |
| 7.6 | 136 | 139 | 25564 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 152 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 159 |
| `com.fasterxml.jackson.databind` | 2 |
| `java.util.concurrent.locks` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 58.9 | 313.25 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1249 | 34.6 | 27.69 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1249 | 30.9 | 24.75 | 14 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 18.7 | 99.36 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 730 | 18.5 | 25.31 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2064 | 9.0 | 4.35 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1249 | 6.0 | 4.79 | 3 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.2 | 34.27 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2064 | 3.0 | 1.48 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111016 | 3.0 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 983 | 983 | coincide |
| Mascotas | 983 | 983 | coincide |
| Citas agendadas | 969 | 969 | coincide |
| Diagnósticos | 460 | 460 | coincide |
| Recetas | 460 | 460 | coincide |
| Pagos | 317 | 317 | coincide |
| Citas completadas | 460 | 460 | coincide |
| Citas canceladas | 198 | 198 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 983 | 0 | 0.0 % | 39 | 268 | 716 |
| Registrar mascota | 983 | 0 | 0.0 % | 22 | 94 | 344 |
| Agendar cita | 969 | 14 | 1.4 % | 32 | 137 | 597 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 20 | 246 | 677 |
| Iniciar atención | 460 | 15 | 3.2 % | 27 | 132 | 452 |
| Leer la consulta | 460 | 0 | 0.0 % | 16 | 79 | 180 |
| Registrar diagnóstico | 460 | 0 | 0.0 % | 15 | 80 | 267 |
| Registrar receta | 460 | 0 | 0.0 % | 17 | 78 | 306 |
| Actualizar mascota | 460 | 0 | 0.0 % | 16 | 73 | 232 |
| Cerrar la consulta | 460 | 0 | 0.0 % | 28 | 130 | 312 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 15 | 205 | 525 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 14 | 75 | 210 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 17 | 104 | 243 |
| Cobrar | 317 | 0 | 0.0 % | 21 | 122 | 496 |
| Cancelar cita | 198 | 0 | 0.0 % | 23 | 215 | 707 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 379 | 6 222 | 6 801 | 20 % |
| panel | 1249 | 0 (0 %) | 87 | 376 | 693 | 12 % |
| login | 250 | 0 (0 %) | 372 | 1 765 | 2 480 | 11 % |
| propietarios | 815 | 0 (0 %) | 70 | 282 | 783 | 6 % |
| apoderado | 983 | 0 (0 %) | 39 | 268 | 716 | 5 % |
| citas_del_mes | 815 | 0 (0 %) | 29 | 341 | 700 | 4 % |
| lista_de_historias | 730 | 0 (0 %) | 43 | 234 | 688 | 4 % |
| cita | 983 | 14 (1 %) | 32 | 137 | 597 | 4 % |
| navegacion | 1249 | 0 (0 %) | 11 | 159 | 595 | 3 % |
| mascota | 983 | 0 (0 %) | 22 | 94 | 344 | 3 % |
| privacidad | 1249 | 0 (0 %) | 13 | 79 | 231 | 2 % |
| empleados | 815 | 0 (0 %) | 22 | 111 | 277 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 21 | 354 | 358 | 0 % |
| 0.3 | 32 | 189 | 481 | 1 411 | 0 % |
| 0.8 | 62 | 273 | 264 | 1 003 | 0 % |
| 1.3 | 92 | 384 | 156 | 713 | 0 % |
| 1.8 | 122 | 574 | 107 | 501 | 0 % |
| 2.3 | 152 | 686 | 170 | 878 | 0 % |
| 2.8 | 182 | 775 | 174 | 762 | 0 % |
| 3.3 | 212 | 899 | 103 | 540 | 0 % |
| 3.8 | 242 | 1015 | 180 | 886 | 0 % |
| 4.3 | 250 | 965 | 590 | 1 461 | 1 % |
| 4.8 | 250 | 938 | 235 | 521 | 0 % |
| 5.3 | 250 | 980 | 133 | 708 | 0 % |
| 5.8 | 250 | 891 | 212 | 796 | 0 % |
| 6.3 | 250 | 993 | 79 | 168 | 0 % |
| 6.8 | 227 | 366 | 64 | 968 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
