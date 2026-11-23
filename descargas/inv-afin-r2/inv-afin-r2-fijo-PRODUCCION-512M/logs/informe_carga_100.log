# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 10 |
| Peticiones hechas | 753 |
| Peticiones correctas (2xx) | 187 |
| Errores (%) | 75.2 % |
| Ritmo medio (peticiones/s) | 2.2 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 80 |
| Tiempo con la CPU al tope (%) | 78 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 430 |
| Sentencias SQL | 5 505 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 753 | 2.2 | no aplica (ramping-vus) | 75.2 % | 564 | 1 / 2 / 8 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 31; fallidas: 42.
- Acciones hechas: panel 13, agenda 6, historia 8, reportes 1, recepcion 41, atencion 3, cobro 0, cancelar 3; sin trabajo disponible: 25.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 37 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 1 avisos en el registro.
- CPU del backend: 80 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 78 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 12 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 430 MB (84 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 51 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 37 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 1.
- Correo: el servidor de pruebas recibió 10 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.5 %).
- GC: 960 pausas, 264.1 s en total (80 % del tiempo de vida de la JVM); 737 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24447 en total, 312 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 16 | 16 | 4660 |
| 0.2 | 74 | 74 | 16078 |
| 0.4 | 79 | 80 | 17220 |
| 1.4 | 109 | 112 | 24083 |
| 1.8 | 109 | 112 | 24129 |
| 2.2 | 109 | 112 | 24131 |
| 2.6 | 109 | 112 | 24135 |
| 3.0 | 109 | 112 | 24139 |
| 3.4 | 109 | 112 | 24179 |
| 3.8 | 109 | 112 | 24189 |
| 4.2 | 109 | 112 | 24191 |
| 4.6 | 109 | 112 | 24208 |
| 5.0 | 109 | 112 | 24215 |
| 5.4 | 109 | 112 | 24216 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 209 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 8 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |
| `org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard$$Lambda/` | 5 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |
| `java.lang.invoke.LambdaForm$BMH/` | 4 |
| `org.hibernate.validator.internal.engine.valueextraction.ValueExtractorResolver$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 219 |
| `org.hibernate.query.sqm` | 12 |
| `org.hibernate.validator.internal` | 10 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 8 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `com.fasterxml.jackson.databind` | 6 |
| `org.hibernate.persister.entity` | 5 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.4 | 220.93 | 29 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 21 | 0.3 | 15.65 | 22 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 21 | 0.3 | 12.51 | 18 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 22 | 0.0 | 2.17 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 21 | 0.0 | 1.56 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 2 | 0.0 | 15.73 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 22 | 0.0 | 1.15 | 2 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 21 | 0.0 | 0.98 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 38 | 0.0 | 0.5 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 22 | 0.0 | 0.85 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 10 | 10 | coincide |
| Mascotas | 8 | 10 | la base tiene más (ver nota) |
| Citas agendadas | 6 | 6 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 1 | la base tiene más (ver nota) |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 10 | 31 | 75.6 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 2 | 20.0 % | 686 | 30 001 | 30 001 |
| Agendar cita | 6 | 2 | 25.0 % | 885 | 3 995 | 3 995 |
| Recepción (llegada) | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Iniciar atención | 1 | 2 | 66.7 % | 30 000 | 30 000 | 30 000 |
| Leer la consulta | 1 | 0 | 0.0 % | 1 445 | 1 445 | 1 445 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 488 | 2 488 | 2 488 |
| Registrar receta | 1 | 0 | 0.0 % | 4 601 | 4 601 | 4 601 |
| Actualizar mascota | 1 | 0 | 0.0 % | 6 615 | 6 615 | 6 615 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 431 | 411 (95 %) | 30 000 | 30 001 | 30 001 | 72 % |
| apoderado | 41 | 31 (76 %) | 30 000 | 30 001 | 30 001 | 6 % |
| notificaciones | 36 | 14 (39 %) | 579 | 30 001 | 30 001 | 3 % |
| cumplimiento | 36 | 14 (39 %) | 538 | 30 001 | 30 001 | 2 % |
| privacidad | 36 | 14 (39 %) | 395 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 35 | 14 (40 %) | 574 | 30 001 | 30 001 | 2 % |
| panel | 33 | 13 (39 %) | 888 | 30 001 | 30 001 | 2 % |
| navegacion | 36 | 13 (36 %) | 546 | 30 001 | 30 022 | 2 % |
| citas_del_mes | 9 | 8 (89 %) | 30 001 | 30 001 | 30 001 | 1 % |
| empleados | 9 | 8 (89 %) | 30 001 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 8 | 7 (88 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 464 | 504 | 0 % |
| 0.2 | 24 | 77 | 2 105 | 4 196 | 3 % |
| 0.5 | 38 | 40 | 11 405 | 16 988 | 0 % |
| 0.7 | 51 | 7 | 30 001 | 30 001 | 71 % |
| 0.9 | 65 | 22 | 30 001 | 30 022 | 100 % |
| 1.2 | 78 | 25 | 30 001 | 30 001 | 100 % |
| 1.4 | 92 | 20 | 30 001 | 30 001 | 100 % |
| 1.7 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 36 | 30 001 | 30 003 | 100 % |
| 3.0 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 4.5 | 99 | 37 | 30 001 | 30 004 | 100 % |
| 4.7 | 84 | 28 | 30 001 | 30 001 | 100 % |
| 5.0 | 55 | 24 | 30 001 | 30 001 | 100 % |
| 5.2 | 32 | 7 | 30 001 | 30 001 | 100 % |
| 5.5 | 9 | 4 | 30 001 | 30 001 | 100 % |
| 5.7 | 8 | 2 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
