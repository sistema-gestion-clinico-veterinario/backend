# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: PRODUCCION-512M (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: P, en el control: P)
- usadas en esta corrida: P para los niveles y P para el control (perfil PRODUCCION-512M)
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 9 |
| Peticiones hechas | 762 |
| Peticiones correctas (2xx) | 200 |
| Errores (%) | 73.8 % |
| Ritmo medio (peticiones/s) | 2.2 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 34 |
| Tiempo con la CPU al tope (%) | 31 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 6 349 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 762 | 2.2 | no aplica (ramping-vus) | 73.8 % | 561 | 1 / 1 / 6 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 33; fallidas: 45.
- Acciones hechas: panel 13, agenda 7, historia 9, reportes 1, recepcion 45, atencion 3, cobro 0, cancelar 3; sin trabajo disponible: 27.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 13 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 25 avisos en el registro.
- CPU del backend: 34 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 31 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 13 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 428 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 13 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 14 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.4 %).
- GC: 442 pausas, 88.7 s en total (42 % del tiempo de vida de la JVM); 216 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24374 en total, 292 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2322 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8291 |
| 0.2 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17106 |
| 0.4 | 84 | 85 | 17884 |
| 1.1 | 109 | 112 | 24013 |
| 1.4 | 109 | 112 | 24101 |
| 1.7 | 109 | 112 | 24175 |
| 1.9 | 109 | 112 | 24184 |
| 2.1 | 109 | 112 | 24186 |
| 2.3 | 109 | 112 | 24192 |
| 2.5 | 109 | 112 | 24359 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 202 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 5 |
| `org.hibernate.validator.internal.engine.valueextraction.ValueExtractorResolver$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 215 |
| `org.hibernate.query.sqm` | 13 |
| `org.hibernate.validator.internal` | 10 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `java.lang.Throwable` | 2 |
| `org.springframework.security.core` | 2 |
| `org.springframework.dao.support` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.9 | 442.23 | 38 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 22 | 0.4 | 20.13 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 22 | 0.3 | 11.7 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 23 | 0.1 | 2.56 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 22 | 0.1 | 2.29 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 48 | 0.0 | 1.0 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 2 | 0.0 | 23.41 | 2 % |
| `select a1_0.id from apoderado a1_0 left join usuario u1_0 on u1_0.id=a1_0.user_id left join company c1_0 on c1_0.id=a1_0.company_id where u1_0.id=$1 and c1_0.id=$2 fetch first $3 rows only` | 162 | 0.0 | 0.25 | 2 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 22 | 0.0 | 1.56 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 23 | 0.0 | 1.29 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 11 | 14 | la base tiene más (ver nota) |
| Mascotas | 8 | 10 | la base tiene más (ver nota) |
| Citas agendadas | 7 | 7 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 2 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 11 | 34 | 75.6 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 3 | 27.3 % | 844 | 30 001 | 30 001 |
| Agendar cita | 7 | 1 | 12.5 % | 979 | 2 332 | 2 332 |
| Recepción (llegada) | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Iniciar atención | 1 | 2 | 66.7 % | 30 001 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 726 | 726 | 726 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 1 907 | 1 907 | 1 907 |
| Registrar receta | 1 | 0 | 0.0 % | 2 344 | 2 344 | 2 344 |
| Actualizar mascota | 1 | 0 | 0.0 % | 2 155 | 2 155 | 2 155 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 420 | 398 (95 %) | 30 000 | 30 001 | 30 001 | 70 % |
| apoderado | 45 | 34 (76 %) | 30 000 | 30 001 | 30 001 | 6 % |
| notificaciones | 38 | 15 (39 %) | 222 | 30 001 | 30 001 | 3 % |
| cumplimiento | 38 | 15 (39 %) | 658 | 30 001 | 30 001 | 3 % |
| avisos_de_cumplimiento | 37 | 15 (41 %) | 698 | 30 001 | 30 001 | 3 % |
| navegacion | 38 | 14 (37 %) | 539 | 30 001 | 30 001 | 2 % |
| privacidad | 38 | 14 (37 %) | 232 | 30 001 | 30 001 | 2 % |
| panel | 35 | 13 (37 %) | 1 485 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 9 | 8 (89 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 9 | 8 (89 %) | 30 001 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 9 | 7 (78 %) | 30 000 | 30 002 | 30 002 | 1 % |
| propietarios | 7 | 6 (86 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 70 | 425 | 456 | 0 % |
| 0.2 | 23 | 81 | 1 930 | 5 213 | 1 % |
| 0.4 | 37 | 44 | 8 252 | 14 952 | 0 % |
| 0.7 | 50 | 7 | 30 001 | 30 001 | 43 % |
| 0.9 | 64 | 20 | 30 001 | 30 001 | 90 % |
| 1.2 | 78 | 28 | 30 001 | 30 001 | 100 % |
| 1.4 | 91 | 21 | 30 002 | 30 002 | 100 % |
| 1.7 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.4 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 3.4 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 4.4 | 99 | 39 | 30 001 | 30 001 | 100 % |
| 4.7 | 84 | 29 | 30 001 | 30 001 | 100 % |
| 4.9 | 53 | 23 | 30 001 | 30 001 | 100 % |
| 5.2 | 31 | 5 | 30 001 | 30 001 | 100 % |
| 5.4 | 6 | 2 | 30 001 | 30 001 | 100 % |
| 5.7 | 6 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
