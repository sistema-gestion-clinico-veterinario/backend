# Fase 3: 250 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: PRODUCCION-512M (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: P, en el control: P)
- usadas en esta corrida: P para los niveles y P para el control (perfil PRODUCCION-512M)
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 25 |
| Peticiones hechas | 1 903 |
| Peticiones correctas (2xx) | 137 |
| Errores (%) | 92.8 % |
| Ritmo medio (peticiones/s) | 3.9 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 19 |
| Tiempo con la CPU al tope (%) | 17 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 3 647 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 1 903 | 3.9 | no aplica (ramping-vus) | 92.8 % | 1 766 | 1 / 1 / 4 | 0 % | 181 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 11; fallidas: 55.
- Acciones hechas: panel 15, agenda 11, historia 10, reportes 1, recepcion 53, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 34.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 25 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 45 avisos en el registro.
- CPU del backend: 19 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 17 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 17 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 55 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **25 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 25 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 7 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.4 %).
- GC: 538 pausas, 81.8 s en total (63 % del tiempo de vida de la JVM); 317 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24167 en total, 194 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 8 | 9 | 3083 |
| 0.0 | 20 | 20 | 5600 |
| 0.1 | 60 | 61 | 13632 |
| 0.2 | 74 | 75 | 16100 |
| 0.3 | 79 | 80 | 17201 |
| 0.6 | 106 | 108 | 22809 |
| 1.0 | 108 | 112 | 23956 |
| 1.2 | 108 | 112 | 23974 |
| 1.3 | 108 | 112 | 23984 |
| 1.5 | 108 | 112 | 23990 |
| 1.7 | 108 | 112 | 23990 |
| 1.8 | 108 | 112 | 23992 |
| 2.0 | 108 | 112 | 23994 |
| 2.1 | 108 | 112 | 23994 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 115 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 5 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |
| `org.springframework.security.core.context.ThreadLocalSecurityContextHolderStrategy$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 119 |
| `org.hibernate.query.sqm` | 12 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 5 |
| `org.springframework.web.servlet` | 4 |
| `org.springframework.security.core` | 3 |
| `java.lang.Throwable` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 17 | 1.2 | 69.31 | 38 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.7 | 353.52 | 23 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 17 | 0.5 | 29.16 | 16 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 17 | 0.2 | 11.54 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 17 | 0.2 | 10.95 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 17 | 0.1 | 6.36 | 3 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 17 | 0.0 | 1.63 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 17 | 0.0 | 0.87 | 0 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 13.67 | 0 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.54 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 7 | la base tiene más (ver nota) |
| Mascotas | 3 | 3 | coincide |
| Citas agendadas | 3 | 3 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 5 | 48 | 90.6 % | 30 000 | 30 001 | 30 002 |
| Registrar mascota | 3 | 2 | 40.0 % | 423 | 30 000 | 30 000 |
| Agendar cita | 3 | 0 | 0.0 % | 212 | 1 222 | 1 222 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 1571 | 1553 (99 %) | 30 000 | 30 001 | 30 001 | 88 % |
| apoderado | 53 | 48 (91 %) | 30 000 | 30 001 | 30 002 | 3 % |
| privacidad | 39 | 20 (51 %) | 30 000 | 30 001 | 30 001 | 1 % |
| navegacion | 39 | 20 (51 %) | 30 000 | 30 001 | 30 001 | 1 % |
| cumplimiento | 37 | 18 (49 %) | 729 | 30 001 | 30 001 | 1 % |
| notificaciones | 35 | 18 (51 %) | 30 000 | 30 001 | 30 002 | 1 % |
| avisos_de_cumplimiento | 34 | 17 (50 %) | 15 018 | 30 001 | 30 001 | 1 % |
| panel | 33 | 16 (48 %) | 895 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 12 | 12 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| empleados | 12 | 12 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 11 | 11 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 10 | 10 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 341 | 359 | 0 % |
| 0.4 | 40 | 1 | 4 249 | 4 249 | 0 % |
| 0.9 | 70 | 23 | 30 001 | 30 001 | 100 % |
| 1.4 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 1.9 | 130 | 39 | 30 001 | 30 001 | 100 % |
| 2.4 | 160 | 57 | 30 001 | 30 001 | 100 % |
| 2.9 | 190 | 53 | 30 001 | 30 001 | 100 % |
| 3.4 | 220 | 74 | 30 001 | 30 001 | 100 % |
| 3.9 | 250 | 72 | 30 001 | 30 001 | 100 % |
| 4.4 | 250 | 84 | 30 001 | 30 001 | 100 % |
| 4.9 | 250 | 90 | 30 001 | 30 001 | 100 % |
| 5.4 | 250 | 68 | 30 001 | 30 002 | 100 % |
| 5.9 | 250 | 76 | 30 001 | 30 001 | 100 % |
| 6.4 | 250 | 84 | 30 001 | 30 001 | 100 % |
| 6.9 | 218 | 74 | 30 001 | 30 001 | 100 % |
| 7.4 | 76 | 18 | 30 001 | 30 001 | 100 % |
| 7.9 | 7 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
