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
| Peticiones hechas | 19 810 |
| Peticiones correctas (2xx) | 19 787 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 31 |
| Tiempo p95 (ms) | 320 |
| Tiempo p99 (ms) | 1 050 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 66 |
| Tiempo con la CPU al tope (%) | 26 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 |
| Memoria pico (MB, de 2048) | 1 285 |
| Sentencias SQL | 539 273 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 810 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 284 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7216; fallidas: 23.
- Acciones hechas: panel 989, agenda 810, historia 726, reportes 93, recepcion 970, atencion 474, cobro 313, cancelar 197; sin trabajo disponible: 731.
- CPU del backend: 66 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 26 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 99 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 285 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 970 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 483 pausas, 52.8 s en total (11 % del tiempo de vida de la JVM); 50 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 929 MB (tope -Xmx1g).
- Clases cargadas: 25561 en total, 163 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4699 |
| 0.2 | 50 | 50 | 11014 |
| 0.3 | 80 | 81 | 16081 |
| 0.3 | 82 | 83 | 16418 |
| 0.5 | 98 | 100 | 18914 |
| 2.0 | 131 | 133 | 24880 |
| 3.0 | 133 | 136 | 25313 |
| 3.6 | 134 | 137 | 25381 |
| 4.2 | 134 | 137 | 25406 |
| 4.8 | 135 | 138 | 25473 |
| 5.3 | 136 | 138 | 25474 |
| 6.0 | 136 | 139 | 25540 |
| 6.7 | 136 | 139 | 25560 |
| 7.5 | 136 | 139 | 25561 |

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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 186 | 76.1 | 409.04 | 28 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1239 | 45.8 | 36.93 | 17 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1239 | 33.8 | 27.28 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 726 | 25.7 | 35.4 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 186 | 22.1 | 118.93 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2049 | 12.0 | 5.85 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1239 | 10.4 | 8.41 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2049 | 4.2 | 2.04 | 2 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110308 | 3.7 | 0.03 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 93 | 3.5 | 37.19 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 970 | 970 | coincide |
| Mascotas | 970 | 970 | coincide |
| Citas agendadas | 959 | 959 | coincide |
| Diagnósticos | 462 | 462 | coincide |
| Recetas | 462 | 462 | coincide |
| Pagos | 313 | 313 | coincide |
| Citas completadas | 462 | 462 | coincide |
| Citas canceladas | 197 | 197 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 970 | 0 | 0.0 % | 56 | 430 | 761 |
| Registrar mascota | 970 | 0 | 0.0 % | 32 | 213 | 490 |
| Agendar cita | 959 | 11 | 1.1 % | 46 | 234 | 510 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 29 | 303 | 758 |
| Iniciar atención | 462 | 12 | 2.5 % | 40 | 194 | 494 |
| Leer la consulta | 462 | 0 | 0.0 % | 23 | 170 | 784 |
| Registrar diagnóstico | 462 | 0 | 0.0 % | 23 | 150 | 359 |
| Registrar receta | 462 | 0 | 0.0 % | 25 | 187 | 531 |
| Actualizar mascota | 462 | 0 | 0.0 % | 21 | 207 | 582 |
| Cerrar la consulta | 462 | 0 | 0.0 % | 41 | 184 | 333 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 28 | 293 | 770 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 21 | 150 | 398 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 28 | 140 | 268 |
| Cobrar | 313 | 0 | 0.0 % | 32 | 268 | 572 |
| Cancelar cita | 197 | 0 | 0.0 % | 35 | 244 | 492 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 93 | 0 (0 %) | 3 647 | 7 357 | 8 130 | 19 % |
| login | 250 | 0 (0 %) | 566 | 3 021 | 3 433 | 12 % |
| panel | 1239 | 0 (0 %) | 125 | 501 | 943 | 11 % |
| propietarios | 810 | 0 (0 %) | 109 | 385 | 779 | 6 % |
| apoderado | 970 | 0 (0 %) | 56 | 430 | 761 | 5 % |
| lista_de_historias | 726 | 0 (0 %) | 62 | 337 | 794 | 4 % |
| citas_del_mes | 810 | 0 (0 %) | 44 | 344 | 740 | 4 % |
| navegacion | 1239 | 0 (0 %) | 16 | 244 | 701 | 4 % |
| cita | 970 | 11 (1 %) | 46 | 234 | 510 | 4 % |
| mascota | 970 | 0 (0 %) | 32 | 213 | 490 | 3 % |
| privacidad | 1239 | 0 (0 %) | 20 | 141 | 320 | 3 % |
| empleados | 810 | 0 (0 %) | 32 | 201 | 606 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 42 | 350 | 359 | 0 % |
| 0.3 | 35 | 190 | 474 | 998 | 0 % |
| 0.8 | 65 | 301 | 335 | 733 | 0 % |
| 1.4 | 95 | 402 | 153 | 635 | 0 % |
| 1.9 | 125 | 558 | 185 | 553 | 0 % |
| 2.4 | 155 | 681 | 142 | 942 | 0 % |
| 2.9 | 185 | 759 | 266 | 1 042 | 0 % |
| 3.4 | 215 | 912 | 197 | 1 072 | 0 % |
| 3.9 | 245 | 1046 | 238 | 1 799 | 0 % |
| 4.3 | 250 | 893 | 721 | 1 337 | 0 % |
| 4.8 | 250 | 901 | 179 | 394 | 0 % |
| 5.3 | 250 | 974 | 187 | 347 | 0 % |
| 5.8 | 250 | 874 | 357 | 831 | 0 % |
| 6.3 | 250 | 980 | 97 | 220 | 0 % |
| 6.8 | 215 | 328 | 66 | 221 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
