# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 9 |
| Peticiones hechas | 417 |
| Peticiones correctas (2xx) | 159 |
| Errores (%) | 61.9 % |
| Ritmo medio (peticiones/s) | 1.4 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 26 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 4 972 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 417 | 1.4 | no aplica (ramping-vus) | 61.9 % | 258 | 0 / 1 / 1 | 0 % | 72 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 21; fallidas: 38.
- Acciones hechas: panel 8, agenda 3, historia 6, reportes 1, recepcion 34, atencion 2, cobro 0, cancelar 2; sin trabajo disponible: 21.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 17 errores `OutOfMemoryError` en el registro.
- CPU del backend: 29 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 26 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 5 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 17 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 9 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.4 %).
- GC: 450 pausas, 69.5 s en total (56 % del tiempo de vida de la JVM); 228 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24270 en total, 252 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2322 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8292 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16106 |
| 0.3 | 79 | 80 | 17106 |
| 0.4 | 86 | 88 | 18207 |
| 1.0 | 108 | 112 | 23912 |
| 1.2 | 108 | 112 | 24059 |
| 1.3 | 108 | 112 | 24090 |
| 1.5 | 108 | 112 | 24091 |
| 1.6 | 108 | 112 | 24092 |
| 1.8 | 108 | 112 | 24092 |
| 1.9 | 108 | 112 | 24092 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 175 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |
| `veterinaria.vargasvet.service.impl.ControlPreventivoServiceImpl$$Lambda/` | 5 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 5 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |
| `veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl$$Lambda/` | 3 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 184 |
| `org.hibernate.query.sqm` | 13 |
| `veterinaria.vargasvet.service.impl` | 11 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 3 |
| `java.lang.Throwable` | 2 |
| `org.springframework.web.servlet` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.7 | 340.05 | 45 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 19 | 0.3 | 15.16 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 19 | 0.2 | 10.1 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 20 | 0.0 | 1.61 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 19 | 0.0 | 1.19 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 17.77 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 20 | 0.0 | 0.81 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 37 | 0.0 | 0.42 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 15.3 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 15.26 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 7 | 9 | la base tiene más (ver nota) |
| Mascotas | 7 | 7 | coincide |
| Citas agendadas | 5 | 6 | la base tiene más (ver nota) |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 1 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 7 | 27 | 79.4 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 7 | 0 | 0.0 % | 1 374 | 4 298 | 4 298 |
| Agendar cita | 5 | 2 | 28.6 % | 1 054 | 30 001 | 30 001 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 15 514 | 30 000 | 30 000 |
| Iniciar atención | 1 | 1 | 50.0 % | 16 821 | 30 000 | 30 000 |
| Leer la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Registrar diagnóstico | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Registrar receta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 160 | 142 (89 %) | 30 000 | 30 001 | 30 001 | 55 % |
| apoderado | 34 | 27 (79 %) | 30 000 | 30 001 | 30 001 | 10 % |
| privacidad | 31 | 11 (35 %) | 42 | 30 001 | 30 001 | 4 % |
| navegacion | 31 | 11 (35 %) | 48 | 30 001 | 30 002 | 4 % |
| notificaciones | 30 | 10 (33 %) | 44 | 30 001 | 30 001 | 4 % |
| cumplimiento | 30 | 10 (33 %) | 50 | 30 001 | 30 001 | 4 % |
| avisos_de_cumplimiento | 29 | 10 (34 %) | 58 | 30 000 | 30 001 | 4 % |
| panel | 26 | 8 (31 %) | 162 | 30 001 | 30 001 | 3 % |
| lista_de_historias | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 4 | 3 (75 %) | 30 001 | 30 001 | 30 001 | 1 % |
| empleados | 4 | 3 (75 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 3 | 3 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 70 | 383 | 761 | 0 % |
| 0.2 | 23 | 73 | 3 046 | 6 666 | 0 % |
| 0.4 | 37 | 16 | 4 500 | 4 500 | 0 % |
| 0.7 | 50 | 8 | 30 001 | 30 001 | 100 % |
| 0.9 | 50 | 26 | 30 001 | 30 001 | 100 % |
| 1.2 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 1.4 | 50 | 14 | 30 001 | 30 001 | 100 % |
| 1.7 | 50 | 21 | 30 001 | 30 001 | 100 % |
| 1.9 | 50 | 18 | 30 002 | 30 002 | 100 % |
| 2.2 | 50 | 13 | 30 001 | 30 001 | 100 % |
| 2.4 | 50 | 24 | 30 001 | 30 001 | 100 % |
| 2.7 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 2.9 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 3.2 | 50 | 20 | 30 001 | 30 001 | 100 % |
| 3.4 | 50 | 20 | 30 001 | 30 001 | 100 % |
| 3.7 | 49 | 17 | 30 001 | 30 001 | 100 % |
| 3.9 | 32 | 12 | 30 001 | 30 001 | 100 % |
| 4.2 | 19 | 8 | 30 001 | 30 001 | 100 % |
| 4.4 | 12 | 3 | 30 001 | 30 001 | 100 % |
| 4.7 | 8 | 4 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
