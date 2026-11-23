# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 14 |
| Peticiones hechas | 731 |
| Peticiones correctas (2xx) | 171 |
| Errores (%) | 76.6 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 39 |
| Tiempo con la CPU al tope (%) | 37 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 426 |
| Sentencias SQL | 5 459 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 731 | 2.1 | no aplica (ramping-vus) | 76.6 % | 560 | 0 / 1 / 2 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 27; fallidas: 40.
- Acciones hechas: panel 12, agenda 6, historia 7, reportes 1, recepcion 37, atencion 3, cobro 0, cancelar 3; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 19 errores `OutOfMemoryError` en el registro.
- CPU del backend: 39 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 37 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 26 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 426 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.5 con unos 41 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **14 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 19 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 1.
- Correo: el servidor de pruebas recibió 11 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.4 %).
- GC: 595 pausas, 116.2 s en total (65 % del tiempo de vida de la JVM); 369 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24275 en total, 187 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 10 | 11 | 3575 |
| 0.1 | 23 | 24 | 6227 |
| 0.2 | 72 | 73 | 15926 |
| 0.2 | 75 | 76 | 16373 |
| 0.4 | 80 | 81 | 17313 |
| 1.0 | 108 | 112 | 23997 |
| 1.3 | 108 | 112 | 24084 |
| 1.6 | 108 | 112 | 24089 |
| 1.8 | 108 | 112 | 24089 |
| 2.0 | 108 | 112 | 24089 |
| 2.2 | 108 | 112 | 24095 |
| 2.4 | 108 | 112 | 24096 |
| 2.6 | 108 | 112 | 24097 |
| 2.9 | 109 | 112 | 24252 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 115 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 5 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 5 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 123 |
| `org.hibernate.query.sqm` | 14 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 4 |
| `org.hibernate.internal.SessionImpl` | 3 |
| `org.springframework.web.servlet` | 3 |
| `java.lang.Throwable` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.7 | 370.17 | 44 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 20 | 0.3 | 16.71 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 20 | 0.2 | 9.01 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 21 | 0.0 | 2.31 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 20 | 0.0 | 1.61 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 43 | 0.0 | 0.49 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 21 | 0.0 | 0.91 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 17.63 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 17.27 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 20 | 0.0 | 0.77 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 8 | 11 | la base tiene más (ver nota) |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 7 | 7 | coincide |
| Diagnósticos | 1 | 1 | coincide |
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
| Registrar propietario | 8 | 29 | 78.4 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 0 | 0.0 % | 543 | 11 897 | 11 897 |
| Agendar cita | 7 | 1 | 12.5 % | 1 307 | 30 000 | 30 000 |
| Recepción (llegada) | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Iniciar atención | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 2 794 | 2 794 | 2 794 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 13 567 | 13 567 | 13 567 |
| Registrar receta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 0 | 3 | 100.0 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 441 | 423 (96 %) | 30 000 | 30 001 | 30 001 | 75 % |
| apoderado | 37 | 29 (78 %) | 30 000 | 30 001 | 30 001 | 5 % |
| cumplimiento | 33 | 13 (39 %) | 71 | 30 001 | 30 001 | 2 % |
| privacidad | 33 | 13 (39 %) | 62 | 30 001 | 30 001 | 2 % |
| navegacion | 33 | 12 (36 %) | 393 | 30 001 | 30 001 | 2 % |
| notificaciones | 32 | 12 (38 %) | 65 | 30 001 | 30 001 | 2 % |
| panel | 30 | 11 (37 %) | 671 | 30 001 | 30 002 | 2 % |
| avisos_de_cumplimiento | 30 | 10 (33 %) | 105 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 7 | 6 (86 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 7 | 6 (86 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 14 | 92 | 446 | 495 | 0 % |
| 0.3 | 28 | 70 | 4 464 | 8 242 | 0 % |
| 0.5 | 41 | 10 | 30 001 | 30 001 | 10 % |
| 0.8 | 55 | 10 | 30 001 | 30 001 | 100 % |
| 1.0 | 68 | 30 | 30 001 | 30 001 | 100 % |
| 1.3 | 82 | 16 | 30 001 | 30 001 | 100 % |
| 1.5 | 96 | 33 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 27 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 42 | 30 001 | 30 002 | 100 % |
| 4.5 | 96 | 33 | 30 001 | 30 001 | 100 % |
| 4.8 | 73 | 27 | 30 001 | 30 001 | 100 % |
| 5.0 | 44 | 15 | 30 001 | 30 001 | 100 % |
| 5.3 | 22 | 5 | 30 001 | 30 001 | 100 % |
| 5.5 | 8 | 3 | 30 001 | 30 001 | 100 % |
| 5.8 | 5 | 1 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
