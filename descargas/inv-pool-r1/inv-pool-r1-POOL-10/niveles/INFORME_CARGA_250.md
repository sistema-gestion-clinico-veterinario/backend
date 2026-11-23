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
| Peticiones hechas | 19 865 |
| Peticiones correctas (2xx) | 19 829 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 44.3 |
| Tiempo mediano (ms) | 26 |
| Tiempo p95 (ms) | 299 |
| Tiempo p99 (ms) | 942 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 66 |
| Tiempo con la CPU al tope (%) | 28 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 |
| Memoria pico (MB, de 2048) | 1 279 |
| Sentencias SQL | 540 829 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 865 | 44.3 | no aplica (ramping-vus) | 0.2 % | 0 | 3 / 9 / 12 | 0 % | 284 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7225; fallidas: 36.
- Acciones hechas: panel 993, agenda 811, historia 728, reportes 93, recepcion 980, atencion 473, cobro 318, cancelar 196; sin trabajo disponible: 737.
- CPU del backend: 66 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 28 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 279 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 980 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 495 pausas, 50.2 s en total (11 % del tiempo de vida de la JVM); 50 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 934 MB (tope -Xmx1g).
- Clases cargadas: 25564 en total, 165 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.2 | 53 | 54 | 11392 |
| 0.2 | 80 | 81 | 16083 |
| 0.3 | 86 | 87 | 17090 |
| 0.6 | 111 | 113 | 21545 |
| 2.0 | 131 | 134 | 24874 |
| 3.0 | 133 | 136 | 25316 |
| 3.5 | 134 | 137 | 25384 |
| 4.1 | 135 | 138 | 25409 |
| 4.8 | 135 | 138 | 25476 |
| 5.4 | 136 | 139 | 25516 |
| 6.0 | 136 | 139 | 25546 |
| 6.7 | 136 | 139 | 25563 |
| 7.5 | 136 | 139 | 25564 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 153 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 160 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 186 | 66.0 | 354.67 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1243 | 43.7 | 35.19 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1243 | 37.7 | 30.32 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 728 | 22.8 | 31.31 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 186 | 21.4 | 114.88 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1243 | 11.7 | 9.42 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2054 | 10.0 | 4.86 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 93 | 3.6 | 39.21 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2054 | 3.2 | 1.57 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110485 | 3.2 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 980 | 980 | coincide |
| Mascotas | 980 | 980 | coincide |
| Citas agendadas | 957 | 957 | coincide |
| Diagnósticos | 460 | 460 | coincide |
| Recetas | 460 | 460 | coincide |
| Pagos | 318 | 318 | coincide |
| Citas completadas | 460 | 460 | coincide |
| Citas canceladas | 196 | 196 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 980 | 0 | 0.0 % | 48 | 415 | 891 |
| Registrar mascota | 980 | 0 | 0.0 % | 26 | 161 | 461 |
| Agendar cita | 957 | 23 | 2.3 % | 38 | 191 | 480 |
| Recepción (llegada) | 473 | 0 | 0.0 % | 22 | 277 | 686 |
| Iniciar atención | 460 | 13 | 2.7 % | 30 | 170 | 388 |
| Leer la consulta | 460 | 0 | 0.0 % | 19 | 108 | 464 |
| Registrar diagnóstico | 460 | 0 | 0.0 % | 18 | 95 | 253 |
| Registrar receta | 460 | 0 | 0.0 % | 20 | 138 | 491 |
| Actualizar mascota | 460 | 0 | 0.0 % | 19 | 128 | 420 |
| Cerrar la consulta | 460 | 0 | 0.0 % | 33 | 218 | 430 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 17 | 207 | 387 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 17 | 116 | 307 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 20 | 99 | 324 |
| Cobrar | 318 | 0 | 0.0 % | 25 | 231 | 755 |
| Cancelar cita | 196 | 0 | 0.0 % | 28 | 329 | 726 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 93 | 0 (0 %) | 2 831 | 6 240 | 6 961 | 19 % |
| panel | 1243 | 0 (0 %) | 107 | 473 | 848 | 13 % |
| login | 250 | 0 (0 %) | 436 | 2 027 | 3 211 | 11 % |
| propietarios | 811 | 0 (0 %) | 81 | 351 | 980 | 6 % |
| apoderado | 980 | 0 (0 %) | 48 | 415 | 891 | 6 % |
| lista_de_historias | 728 | 0 (0 %) | 50 | 388 | 718 | 4 % |
| navegacion | 1243 | 0 (0 %) | 14 | 297 | 628 | 4 % |
| citas_del_mes | 811 | 0 (0 %) | 35 | 336 | 731 | 4 % |
| cita | 980 | 23 (2 %) | 38 | 191 | 480 | 4 % |
| mascota | 980 | 0 (0 %) | 26 | 161 | 461 | 3 % |
| privacidad | 1243 | 0 (0 %) | 17 | 115 | 212 | 2 % |
| historia | 728 | 0 (0 %) | 30 | 133 | 302 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 7 | 372 | 372 | 0 % |
| 0.3 | 31 | 171 | 436 | 1 428 | 1 % |
| 0.8 | 61 | 285 | 268 | 719 | 0 % |
| 1.3 | 91 | 386 | 140 | 587 | 0 % |
| 1.8 | 121 | 544 | 133 | 605 | 0 % |
| 2.3 | 151 | 655 | 304 | 1 787 | 0 % |
| 2.8 | 181 | 799 | 175 | 845 | 0 % |
| 3.3 | 211 | 867 | 135 | 512 | 0 % |
| 3.8 | 241 | 1025 | 301 | 1 790 | 0 % |
| 4.3 | 250 | 1002 | 622 | 1 419 | 0 % |
| 4.8 | 250 | 942 | 403 | 1 151 | 0 % |
| 5.3 | 250 | 962 | 134 | 344 | 0 % |
| 5.8 | 250 | 932 | 310 | 755 | 0 % |
| 6.3 | 250 | 1020 | 83 | 183 | 0 % |
| 6.8 | 234 | 486 | 301 | 836 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
