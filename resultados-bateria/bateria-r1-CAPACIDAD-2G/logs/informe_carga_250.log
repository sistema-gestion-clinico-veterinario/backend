# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 144 |
| Peticiones hechas | 6 728 |
| Peticiones correctas (2xx) | 5 133 |
| Errores (%) | 23.7 % |
| Ritmo medio (peticiones/s) | 13.7 |
| Tiempo mediano (ms) | 77 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 9 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 19 |
| Memoria pico (MB, de 2048) | 972 |
| Sentencias SQL | 141 650 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 6 728 | 13.7 | no aplica (ramping-vus) | 23.7 % | 1 543 | 1 / 4 / 8 | 0 % | 217 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1690; fallidas: 238.
- Acciones hechas: panel 277, agenda 238, historia 253, reportes 26, recepcion 361, atencion 140, cobro 72, cancelar 54; sin trabajo disponible: 290.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2834 avisos en el registro.
- CPU del backend: 30 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 19 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 972 MB (47 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % 401 (sesión perdida), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.4 con unos 220 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **144 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 269 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.3 %).
- GC: 279 pausas, 15.7 s en total (3 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 480 MB (tope -Xmx1g).
- Clases cargadas: 25478 en total, 90 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 13 | 3807 |
| 0.1 | 21 | 22 | 5302 |
| 0.1 | 37 | 38 | 8351 |
| 0.2 | 65 | 66 | 13628 |
| 0.2 | 80 | 81 | 16049 |
| 0.3 | 80 | 81 | 16104 |
| 0.4 | 86 | 87 | 17076 |
| 0.4 | 87 | 88 | 17211 |
| 1.0 | 123 | 126 | 23779 |
| 1.9 | 130 | 133 | 24796 |
| 2.5 | 132 | 135 | 25004 |
| 3.0 | 133 | 136 | 25312 |
| 3.4 | 134 | 137 | 25383 |
| 3.8 | 134 | 137 | 25402 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 46 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 50 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 369 | 91.4 | 247.7 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 369 | 65.2 | 176.57 | 25 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 369 | 29.4 | 79.73 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 40 | 16.3 | 407.92 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 369 | 13.5 | 36.48 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 369 | 12.0 | 32.45 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 369 | 9.8 | 26.44 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 173 | 6.9 | 39.94 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 40 | 5.6 | 139.69 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 537 | 1.4 | 2.54 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 263 | 269 | la base tiene más (ver nota) |
| Mascotas | 263 | 263 | coincide |
| Citas agendadas | 258 | 258 | coincide |
| Diagnósticos | 97 | 98 | la base tiene más (ver nota) |
| Recetas | 97 | 97 | coincide |
| Pagos | 47 | 47 | coincide |
| Citas completadas | 96 | 97 | la base tiene más (ver nota) |
| Citas canceladas | 37 | 38 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 263 | 98 | 27.1 % | 121 | 30 001 | 30 001 |
| Registrar mascota | 263 | 0 | 0.0 % | 45 | 570 | 1 336 |
| Agendar cita | 258 | 5 | 1.9 % | 64 | 569 | 1 406 |
| Recepción (llegada) | 105 | 35 | 25.0 % | 121 | 30 001 | 30 001 |
| Iniciar atención | 101 | 39 | 27.9 % | 78 | 30 001 | 30 001 |
| Leer la consulta | 99 | 2 | 2.0 % | 36 | 599 | 30 000 |
| Registrar diagnóstico | 97 | 4 | 4.0 % | 34 | 973 | 30 001 |
| Registrar receta | 97 | 4 | 4.0 % | 36 | 439 | 30 000 |
| Actualizar mascota | 97 | 4 | 4.0 % | 33 | 1 081 | 30 001 |
| Cerrar la consulta | 96 | 5 | 5.0 % | 66 | 535 | 30 000 |
| Caja: crear punto de cobro | 44 | 22 | 33.3 % | 151 | 30 001 | 30 001 |
| Caja: vincular equipo | 43 | 1 | 2.3 % | 36 | 821 | 30 001 |
| Caja: abrir la caja | 43 | 0 | 0.0 % | 43 | 694 | 761 |
| Cobrar | 47 | 2 | 4.1 % | 52 | 2 035 | 30 001 |
| Cancelar cita | 37 | 17 | 31.5 % | 71 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 674 | 524 (78 %) | 30 000 | 30 001 | 30 001 | 30 % |
| apoderado | 361 | 98 (27 %) | 121 | 30 001 | 30 001 | 6 % |
| privacidad | 471 | 88 (19 %) | 24 | 30 001 | 30 001 | 6 % |
| navegacion | 471 | 94 (20 %) | 27 | 30 001 | 30 001 | 6 % |
| cumplimiento | 460 | 90 (20 %) | 30 | 30 001 | 30 001 | 6 % |
| lista_de_historias | 253 | 85 (34 %) | 190 | 30 001 | 30 001 | 5 % |
| citas_del_mes | 254 | 83 (33 %) | 226 | 30 001 | 30 001 | 5 % |
| empleados | 254 | 79 (31 %) | 111 | 30 001 | 30 001 | 5 % |
| notificaciones | 449 | 80 (18 %) | 22 | 30 001 | 30 001 | 5 % |
| propietarios | 238 | 72 (30 %) | 227 | 30 001 | 30 001 | 5 % |
| panel | 427 | 63 (15 %) | 554 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 438 | 71 (16 %) | 22 | 30 001 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 334 | 576 | 0 % |
| 0.4 | 40 | 191 | 338 | 572 | 0 % |
| 0.9 | 70 | 316 | 376 | 893 | 1 % |
| 1.4 | 100 | 436 | 369 | 614 | 0 % |
| 1.9 | 130 | 559 | 326 | 776 | 0 % |
| 2.4 | 160 | 567 | 1 916 | 2 930 | 2 % |
| 2.9 | 190 | 507 | 2 159 | 3 400 | 4 % |
| 3.7 | 235 | 132 | 30 001 | 30 001 | 99 % |
| 4.2 | 250 | 73 | 30 001 | 30 002 | 95 % |
| 4.7 | 250 | 70 | 30 001 | 30 001 | 100 % |
| 5.2 | 250 | 87 | 30 001 | 30 001 | 100 % |
| 5.7 | 250 | 96 | 30 001 | 30 001 | 100 % |
| 6.2 | 250 | 99 | 30 001 | 30 001 | 100 % |
| 6.7 | 250 | 90 | 30 001 | 30 001 | 100 % |
| 7.2 | 176 | 65 | 30 001 | 30 001 | 100 % |
| 7.7 | 81 | 32 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
