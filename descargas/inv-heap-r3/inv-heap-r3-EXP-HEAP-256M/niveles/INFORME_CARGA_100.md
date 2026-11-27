# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 256m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 3 452 |
| Peticiones correctas (2xx) | 3 018 |
| Errores (%) | 12.6 % |
| Ritmo medio (peticiones/s) | 9.9 |
| Tiempo mediano (ms) | 28 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 71 |
| Tiempo con la CPU al tope (%) | 54 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 500 |
| Sentencias SQL | 79 369 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 452 | 9.9 | no aplica (ramping-vus) | 12.6 % | 429 | 1 / 2 / 4 | 0 % | 122 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 997; fallidas: 113.
- Acciones hechas: panel 134, agenda 124, historia 130, reportes 11, recepcion 198, atencion 81, cobro 37, cancelar 28; sin trabajo disponible: 167.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 36 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 34 avisos en el registro.
- CPU del backend: 71 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 54 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 30 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 500 MB (98 % de 512 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.0 con unos 100 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 36 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 156 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.3 %).
- GC: 754 pausas, 174.9 s en total (53 % del tiempo de vida de la JVM); 486 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25515 en total, 280 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 11 | 11 | 3772 |
| 0.1 | 52 | 53 | 12171 |
| 0.2 | 74 | 75 | 16116 |
| 0.7 | 106 | 108 | 22866 |
| 2.0 | 111 | 115 | 24902 |
| 2.9 | 111 | 115 | 25250 |
| 3.3 | 111 | 115 | 25267 |
| 3.6 | 111 | 115 | 25267 |
| 3.9 | 111 | 115 | 25269 |
| 4.3 | 111 | 115 | 25271 |
| 4.6 | 111 | 115 | 25271 |
| 4.9 | 111 | 115 | 25271 |
| 5.2 | 111 | 115 | 25272 |
| 5.5 | 111 | 115 | 25515 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 212 |
| `org.springframework.security.web.util.ThrowableAnalyzer$$Lambda/` | 5 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 4 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 215 |
| `org.apache.catalina.core` | 8 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.security.web` | 5 |
| `org.springframework.transaction.support` | 4 |
| `org.springframework.web.servlet` | 3 |
| `java.lang.Throwable` | 2 |
| `org.springframework.dao.support` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 17 | 4.3 | 252.75 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 221 | 3.3 | 15.06 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 221 | 1.8 | 8.03 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 98 | 1.5 | 15.46 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.1 | 68.01 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 98 | 0.7 | 6.69 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 312 | 0.6 | 2.08 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 312 | 0.3 | 0.9 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 221 | 0.3 | 1.24 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 153 | 0.2 | 1.23 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 155 | 156 | la base tiene más (ver nota) |
| Mascotas | 155 | 155 | coincide |
| Citas agendadas | 148 | 148 | coincide |
| Diagnósticos | 58 | 58 | coincide |
| Recetas | 58 | 58 | coincide |
| Pagos | 26 | 26 | coincide |
| Citas completadas | 58 | 58 | coincide |
| Citas canceladas | 24 | 24 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 155 | 43 | 21.7 % | 56 | 30 001 | 30 001 |
| Registrar mascota | 155 | 0 | 0.0 % | 28 | 135 | 961 |
| Agendar cita | 148 | 7 | 4.5 % | 40 | 167 | 30 000 |
| Recepción (llegada) | 63 | 18 | 22.2 % | 30 | 30 001 | 30 001 |
| Iniciar atención | 60 | 21 | 25.9 % | 44 | 30 001 | 30 001 |
| Leer la consulta | 59 | 1 | 1.7 % | 19 | 387 | 30 001 |
| Registrar diagnóstico | 58 | 2 | 3.3 % | 20 | 533 | 30 000 |
| Registrar receta | 58 | 2 | 3.3 % | 21 | 477 | 30 001 |
| Actualizar mascota | 58 | 2 | 3.3 % | 20 | 516 | 30 001 |
| Cerrar la consulta | 58 | 2 | 3.3 % | 33 | 548 | 30 001 |
| Caja: crear punto de cobro | 25 | 10 | 28.6 % | 30 | 30 001 | 30 001 |
| Caja: vincular equipo | 25 | 0 | 0.0 % | 11 | 52 | 52 |
| Caja: abrir la caja | 25 | 0 | 0.0 % | 15 | 432 | 474 |
| Cobrar | 26 | 1 | 3.7 % | 25 | 497 | 30 000 |
| Cancelar cita | 24 | 4 | 14.3 % | 38 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| apoderado | 198 | 43 (22 %) | 56 | 30 001 | 30 001 | 10 % |
| privacidad | 262 | 38 (15 %) | 11 | 30 001 | 30 001 | 9 % |
| empleados | 129 | 38 (29 %) | 39 | 30 001 | 30 001 | 9 % |
| navegacion | 262 | 36 (14 %) | 13 | 30 001 | 30 001 | 8 % |
| propietarios | 124 | 35 (28 %) | 114 | 30 001 | 30 001 | 8 % |
| citas_del_mes | 129 | 34 (26 %) | 72 | 30 001 | 30 001 | 8 % |
| lista_de_historias | 130 | 33 (25 %) | 51 | 30 001 | 30 001 | 8 % |
| cumplimiento | 256 | 33 (13 %) | 13 | 30 001 | 30 001 | 8 % |
| notificaciones | 248 | 26 (10 %) | 8 | 30 000 | 30 001 | 6 % |
| iniciar | 81 | 21 (26 %) | 44 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 240 | 19 (8 %) | 11 | 30 001 | 30 001 | 4 % |
| checkin | 81 | 18 (22 %) | 30 | 30 001 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 83 | 342 | 365 | 0 % |
| 0.2 | 25 | 141 | 433 | 1 512 | 1 % |
| 0.5 | 39 | 204 | 327 | 376 | 1 % |
| 0.7 | 52 | 234 | 321 | 383 | 0 % |
| 1.0 | 66 | 283 | 392 | 1 958 | 0 % |
| 1.2 | 79 | 333 | 403 | 2 044 | 0 % |
| 1.5 | 93 | 422 | 359 | 2 288 | 0 % |
| 1.7 | 100 | 405 | 329 | 455 | 0 % |
| 2.0 | 100 | 454 | 413 | 621 | 0 % |
| 2.2 | 100 | 392 | 374 | 548 | 0 % |
| 2.5 | 100 | 72 | 4 307 | 5 515 | 0 % |
| 2.7 | 100 | 2 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 83 | 30 001 | 30 005 | 100 % |
| 3.2 | 100 | 17 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 43 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 47 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 44 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 4.5 | 97 | 42 | 30 001 | 30 001 | 100 % |
| 4.7 | 91 | 41 | 30 001 | 30 001 | 100 % |
| 5.0 | 80 | 40 | 30 001 | 30 001 | 100 % |
| 5.2 | 58 | 19 | 30 001 | 30 001 | 100 % |
| 5.5 | 37 | 15 | 30 001 | 30 001 | 100 % |
| 5.7 | 5 | 1 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
