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
| Peticiones hechas | 20 066 |
| Peticiones correctas (2xx) | 20 045 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.9 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 168 |
| Tiempo p99 (ms) | 601 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 59 |
| Tiempo con la CPU al tope (%) | 14 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 34 |
| Memoria pico (MB, de 2048) | 1 267 |
| Sentencias SQL | 546 758 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 20 066 | 44.9 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 285 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7330; fallidas: 21.
- Acciones hechas: panel 1002, agenda 822, historia 734, reportes 95, recepcion 985, atencion 475, cobro 322, cancelar 202; sin trabajo disponible: 735.
- CPU del backend: 59 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 14 % del tiempo.
- CPU de PostgreSQL: 34 % de media y 94 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 267 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 985 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.4 %).
- GC: 489 pausas, 40.6 s en total (9 % del tiempo de vida de la JVM); 47 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 927 MB (tope -Xmx1g).
- Clases cargadas: 25560 en total, 163 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.1 | 53 | 54 | 11392 |
| 0.2 | 80 | 81 | 16082 |
| 0.3 | 86 | 87 | 17089 |
| 0.5 | 101 | 103 | 19668 |
| 2.0 | 131 | 134 | 24872 |
| 2.9 | 133 | 136 | 25312 |
| 3.6 | 134 | 137 | 25397 |
| 4.2 | 135 | 138 | 25456 |
| 4.9 | 135 | 138 | 25473 |
| 5.4 | 136 | 139 | 25512 |
| 6.0 | 136 | 139 | 25556 |
| 6.8 | 136 | 139 | 25559 |
| 7.6 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 158 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 190 | 59.0 | 310.56 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1252 | 36.7 | 29.33 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1252 | 30.2 | 24.1 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 734 | 22.0 | 29.96 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 190 | 19.6 | 103.11 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2074 | 10.3 | 4.97 | 5 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1252 | 9.6 | 7.64 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 95 | 3.3 | 34.89 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2074 | 3.1 | 1.49 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111885 | 3.1 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 985 | 985 | coincide |
| Mascotas | 985 | 985 | coincide |
| Citas agendadas | 968 | 968 | coincide |
| Diagnósticos | 471 | 471 | coincide |
| Recetas | 471 | 471 | coincide |
| Pagos | 322 | 322 | coincide |
| Citas completadas | 471 | 471 | coincide |
| Citas canceladas | 202 | 202 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 985 | 0 | 0.0 % | 36 | 196 | 582 |
| Registrar mascota | 985 | 0 | 0.0 % | 20 | 55 | 133 |
| Agendar cita | 968 | 17 | 1.7 % | 29 | 80 | 168 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 16 | 122 | 549 |
| Iniciar atención | 471 | 4 | 0.8 % | 24 | 67 | 196 |
| Leer la consulta | 471 | 0 | 0.0 % | 13 | 43 | 128 |
| Registrar diagnóstico | 471 | 0 | 0.0 % | 13 | 40 | 53 |
| Registrar receta | 471 | 0 | 0.0 % | 14 | 43 | 125 |
| Actualizar mascota | 471 | 0 | 0.0 % | 14 | 38 | 55 |
| Cerrar la consulta | 471 | 0 | 0.0 % | 24 | 69 | 156 |
| Caja: crear punto de cobro | 189 | 0 | 0.0 % | 13 | 135 | 538 |
| Caja: vincular equipo | 189 | 0 | 0.0 % | 12 | 40 | 135 |
| Caja: abrir la caja | 189 | 0 | 0.0 % | 16 | 46 | 62 |
| Cobrar | 322 | 0 | 0.0 % | 21 | 76 | 640 |
| Cancelar cita | 202 | 0 | 0.0 % | 20 | 117 | 370 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 95 | 0 (0 %) | 2 290 | 4 448 | 5 003 | 22 % |
| panel | 1252 | 0 (0 %) | 77 | 338 | 565 | 14 % |
| login | 250 | 0 (0 %) | 349 | 1 302 | 1 746 | 11 % |
| propietarios | 822 | 0 (0 %) | 66 | 204 | 351 | 6 % |
| apoderado | 985 | 0 (0 %) | 36 | 196 | 582 | 5 % |
| lista_de_historias | 734 | 0 (0 %) | 40 | 266 | 663 | 5 % |
| citas_del_mes | 822 | 0 (0 %) | 27 | 145 | 612 | 4 % |
| cita | 985 | 17 (2 %) | 29 | 80 | 168 | 3 % |
| navegacion | 1252 | 0 (0 %) | 9 | 98 | 424 | 3 % |
| privacidad | 1252 | 0 (0 %) | 12 | 71 | 163 | 3 % |
| mascota | 985 | 0 (0 %) | 20 | 55 | 133 | 2 % |
| empleados | 822 | 0 (0 %) | 19 | 57 | 156 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 3 | 21 | 331 | 340 | 0 % |
| 0.3 | 33 | 191 | 325 | 900 | 1 % |
| 0.8 | 63 | 278 | 264 | 476 | 0 % |
| 1.3 | 93 | 391 | 134 | 438 | 0 % |
| 1.8 | 123 | 570 | 96 | 538 | 0 % |
| 2.3 | 153 | 662 | 97 | 420 | 0 % |
| 2.8 | 183 | 782 | 129 | 645 | 0 % |
| 3.3 | 213 | 915 | 100 | 349 | 0 % |
| 3.8 | 243 | 1020 | 146 | 971 | 0 % |
| 4.3 | 250 | 975 | 203 | 641 | 0 % |
| 4.8 | 250 | 936 | 75 | 165 | 0 % |
| 5.3 | 250 | 985 | 110 | 260 | 0 % |
| 5.8 | 250 | 893 | 269 | 569 | 0 % |
| 6.3 | 250 | 976 | 61 | 124 | 0 % |
| 6.8 | 223 | 389 | 96 | 451 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
