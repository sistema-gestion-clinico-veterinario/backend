# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 14 |
| Peticiones hechas | 689 |
| Peticiones correctas (2xx) | 125 |
| Errores (%) | 81.9 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 30 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 418 |
| Sentencias SQL | 3 578 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 689 | 2.0 | no aplica (ramping-vus) | 81.9 % | 564 | 0 / 1 / 2 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 10; fallidas: 35.
- Acciones hechas: panel 12, agenda 4, historia 3, reportes 1, recepcion 33, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 24 errores `OutOfMemoryError` en el registro.
- CPU del backend: 32 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 30 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 19 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 418 MB (82 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 55 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **14 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 24 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 6 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.5 %).
- GC: 586 pausas, 99.9 s en total (68 % del tiempo de vida de la JVM); 362 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24198 en total, 241 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 10 | 10 | 3447 |
| 0.0 | 22 | 23 | 5986 |
| 0.1 | 67 | 68 | 15027 |
| 0.2 | 74 | 75 | 16117 |
| 0.3 | 79 | 80 | 17216 |
| 0.9 | 108 | 112 | 23810 |
| 1.1 | 108 | 112 | 23954 |
| 1.3 | 108 | 112 | 23957 |
| 1.4 | 108 | 112 | 23959 |
| 1.6 | 108 | 112 | 23961 |
| 1.8 | 108 | 112 | 23961 |
| 2.0 | 108 | 112 | 23976 |
| 2.2 | 108 | 112 | 23991 |
| 2.4 | 108 | 112 | 23993 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 140 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 148 |
| `org.hibernate.query.sqm` | 12 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `com.fasterxml.jackson.databind` | 4 |
| `org.springframework.web.servlet` | 3 |
| `org.hibernate.dialect.ColumnAliasExtractor` | 3 |
| `org.springframework.transaction.support` | 3 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 284.45 | 52 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.2 | 14.17 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 6.63 | 10 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.66 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 1.17 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 13.92 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.88 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.84 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 11.8 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 26 | 0.0 | 0.38 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 7 | la base tiene más (ver nota) |
| Mascotas | 3 | 4 | la base tiene más (ver nota) |
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
| Registrar propietario | 4 | 29 | 87.9 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 3 | 1 | 25.0 % | 258 | 30 001 | 30 001 |
| Agendar cita | 3 | 0 | 0.0 % | 452 | 2 276 | 2 276 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 452 | 436 (96 %) | 30 001 | 30 001 | 30 001 | 77 % |
| apoderado | 33 | 29 (88 %) | 30 000 | 30 001 | 30 001 | 5 % |
| cumplimiento | 30 | 13 (43 %) | 203 | 30 001 | 30 001 | 2 % |
| privacidad | 30 | 13 (43 %) | 217 | 30 001 | 30 001 | 2 % |
| navegacion | 30 | 13 (43 %) | 46 | 30 001 | 30 001 | 2 % |
| panel | 28 | 12 (43 %) | 497 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 28 | 12 (43 %) | 53 | 30 001 | 30 001 | 2 % |
| notificaciones | 28 | 12 (43 %) | 32 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 4 | 4 (100 %) | 30 000 | 30 000 | 30 000 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 000 | 30 000 | 30 000 | 1 % |
| lista_de_historias | 3 | 3 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 6 | 349 | 349 | 0 % |
| 0.0 | 14 | 91 | 346 | 832 | 0 % |
| 0.3 | 28 | 28 | 2 966 | 4 018 | 0 % |
| 0.5 | 41 | 1 | 30 001 | 30 001 | 100 % |
| 0.8 | 55 | 22 | 30 001 | 30 001 | 100 % |
| 1.0 | 69 | 20 | 30 001 | 30 001 | 100 % |
| 1.3 | 82 | 19 | 30 001 | 30 001 | 100 % |
| 1.5 | 96 | 36 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 4.5 | 96 | 36 | 30 001 | 30 001 | 100 % |
| 4.8 | 74 | 26 | 30 001 | 30 001 | 100 % |
| 5.0 | 46 | 16 | 30 001 | 30 001 | 100 % |
| 5.3 | 21 | 1 | 30 000 | 30 000 | 100 % |
| 5.5 | 4 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
