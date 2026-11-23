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
| Usuarios sin degradación | 153 |
| Peticiones hechas | 6 726 |
| Peticiones correctas (2xx) | 5 135 |
| Errores (%) | 23.7 % |
| Ritmo medio (peticiones/s) | 13.7 |
| Tiempo mediano (ms) | 53 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 28 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 20 |
| Memoria pico (MB, de 2048) | 962 |
| Sentencias SQL | 142 496 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 6 726 | 13.7 | no aplica (ramping-vus) | 23.7 % | 1 541 | 1 / 3 / 8 | 0 % | 217 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1687; fallidas: 249.
- Acciones hechas: panel 280, agenda 241, historia 253, reportes 26, recepcion 359, atencion 142, cobro 72, cancelar 53; sin trabajo disponible: 290.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2770 avisos en el registro.
- CPU del backend: 28 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 962 MB (47 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 2 % 401 (sesión perdida), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.6 con unos 228 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **153 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 275 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 135 MB (+0.5 %).
- GC: 281 pausas, 14.4 s en total (3 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 460 MB (tope -Xmx1g).
- Clases cargadas: 25472 en total, 76 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 13 | 3807 |
| 0.1 | 22 | 22 | 5474 |
| 0.1 | 39 | 40 | 8808 |
| 0.2 | 66 | 66 | 13661 |
| 0.2 | 80 | 81 | 16070 |
| 0.3 | 80 | 81 | 16103 |
| 0.3 | 86 | 87 | 17085 |
| 0.4 | 88 | 88 | 17301 |
| 0.9 | 123 | 125 | 23773 |
| 1.8 | 130 | 133 | 24796 |
| 2.4 | 132 | 134 | 24997 |
| 3.0 | 133 | 136 | 25306 |
| 3.3 | 134 | 137 | 25377 |
| 3.7 | 134 | 137 | 25383 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 31 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 35 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.apache.tomcat.util` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 372 | 92.4 | 248.26 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 372 | 56.3 | 151.26 | 23 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 372 | 27.7 | 74.47 | 11 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 38 | 19.0 | 499.34 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 372 | 12.6 | 33.8 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 372 | 11.1 | 29.96 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 372 | 7.3 | 19.66 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 172 | 6.5 | 37.58 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 38 | 4.1 | 108.38 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 544 | 1.3 | 2.33 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 264 | 275 | la base tiene más (ver nota) |
| Mascotas | 263 | 263 | coincide |
| Citas agendadas | 256 | 257 | la base tiene más (ver nota) |
| Diagnósticos | 98 | 98 | coincide |
| Recetas | 98 | 100 | la base tiene más (ver nota) |
| Pagos | 46 | 46 | coincide |
| Citas completadas | 97 | 97 | coincide |
| Citas canceladas | 37 | 38 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 264 | 95 | 26.5 % | 83 | 30 001 | 30 001 |
| Registrar mascota | 263 | 1 | 0.4 % | 38 | 262 | 2 613 |
| Agendar cita | 256 | 7 | 2.7 % | 52 | 221 | 30 001 |
| Recepción (llegada) | 104 | 38 | 26.8 % | 45 | 30 001 | 30 001 |
| Iniciar atención | 103 | 39 | 27.5 % | 60 | 30 001 | 30 001 |
| Leer la consulta | 98 | 5 | 4.9 % | 27 | 708 | 30 001 |
| Registrar diagnóstico | 98 | 5 | 4.9 % | 25 | 1 418 | 30 001 |
| Registrar receta | 98 | 5 | 4.9 % | 28 | 1 409 | 30 000 |
| Actualizar mascota | 97 | 6 | 5.8 % | 26 | 30 000 | 30 001 |
| Cerrar la consulta | 97 | 6 | 5.8 % | 48 | 30 000 | 30 001 |
| Caja: crear punto de cobro | 42 | 24 | 36.4 % | 90 | 30 001 | 30 001 |
| Caja: vincular equipo | 42 | 0 | 0.0 % | 27 | 288 | 674 |
| Caja: abrir la caja | 42 | 0 | 0.0 % | 31 | 165 | 573 |
| Cobrar | 46 | 2 | 4.2 % | 44 | 1 184 | 30 000 |
| Cancelar cita | 37 | 16 | 30.2 % | 64 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 672 | 522 (78 %) | 30 000 | 30 001 | 30 001 | 30 % |
| apoderado | 359 | 95 (26 %) | 83 | 30 001 | 30 001 | 6 % |
| navegacion | 469 | 94 (20 %) | 22 | 30 001 | 30 001 | 6 % |
| privacidad | 469 | 85 (18 %) | 18 | 30 001 | 30 001 | 6 % |
| lista_de_historias | 253 | 87 (34 %) | 125 | 30 001 | 30 001 | 6 % |
| cumplimiento | 458 | 87 (19 %) | 24 | 30 001 | 30 001 | 5 % |
| citas_del_mes | 252 | 82 (33 %) | 69 | 30 001 | 30 001 | 5 % |
| empleados | 252 | 74 (29 %) | 62 | 30 001 | 30 001 | 5 % |
| notificaciones | 449 | 81 (18 %) | 17 | 30 001 | 30 001 | 5 % |
| propietarios | 241 | 74 (31 %) | 212 | 30 001 | 30 001 | 5 % |
| panel | 430 | 64 (15 %) | 532 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 436 | 66 (15 %) | 18 | 30 001 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 3 | 27 | 325 | 424 | 0 % |
| 0.3 | 33 | 190 | 395 | 1 070 | 0 % |
| 0.8 | 63 | 279 | 381 | 640 | 1 % |
| 1.3 | 93 | 384 | 344 | 646 | 1 % |
| 1.8 | 123 | 548 | 443 | 1 282 | 0 % |
| 2.3 | 153 | 639 | 1 439 | 2 832 | 1 % |
| 2.8 | 183 | 603 | 909 | 2 082 | 3 % |
| 3.3 | 213 | 3 | 30 001 | 30 001 | 100 % |
| 3.8 | 243 | 68 | 30 001 | 30 001 | 100 % |
| 4.3 | 250 | 108 | 30 001 | 30 001 | 100 % |
| 4.8 | 250 | 78 | 30 001 | 30 001 | 100 % |
| 5.3 | 250 | 99 | 30 001 | 30 001 | 100 % |
| 5.8 | 250 | 96 | 30 001 | 30 001 | 100 % |
| 6.3 | 250 | 84 | 30 001 | 30 001 | 100 % |
| 6.8 | 237 | 99 | 30 001 | 30 001 | 100 % |
| 7.3 | 140 | 49 | 30 001 | 30 001 | 100 % |
| 7.8 | 76 | 27 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
