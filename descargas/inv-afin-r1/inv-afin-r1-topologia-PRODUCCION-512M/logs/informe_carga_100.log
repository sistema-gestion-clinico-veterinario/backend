# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 5 |
| Peticiones hechas | 748 |
| Peticiones correctas (2xx) | 184 |
| Errores (%) | 75.4 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 72 |
| Tiempo con la CPU al tope (%) | 70 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 433 |
| Sentencias SQL | 5 505 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 748 | 2.1 | no aplica (ramping-vus) | 75.4 % | 563 | 1 / 2 / 8 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 31; fallidas: 41.
- Acciones hechas: panel 13, agenda 6, historia 7, reportes 1, recepcion 41, atencion 3, cobro 0, cancelar 3; sin trabajo disponible: 25.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 34 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 4 avisos en el registro.
- CPU del backend: 72 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 70 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 35 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 433 MB (85 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.6 con unos 46 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **5 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 34 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 11 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.5 %).
- GC: 874 pausas, 230.0 s en total (78 % del tiempo de vida de la JVM); 650 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24543 en total, 387 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 12 | 13 | 3999 |
| 0.2 | 69 | 70 | 15534 |
| 0.3 | 79 | 80 | 17106 |
| 1.1 | 109 | 112 | 24016 |
| 1.5 | 109 | 112 | 24132 |
| 1.9 | 109 | 112 | 24138 |
| 2.3 | 109 | 112 | 24149 |
| 2.6 | 109 | 112 | 24166 |
| 3.0 | 109 | 112 | 24173 |
| 3.4 | 109 | 112 | 24175 |
| 3.7 | 109 | 112 | 24178 |
| 4.1 | 109 | 112 | 24259 |
| 4.5 | 109 | 112 | 24369 |
| 4.8 | 109 | 112 | 24376 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 284 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 8 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |
| `org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard$$Lambda/` | 5 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |
| `java.lang.invoke.LambdaForm$BMH/` | 4 |
| `org.hibernate.validator.internal.engine.valueextraction.ValueExtractorResolver$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 295 |
| `org.hibernate.query.sqm` | 12 |
| `org.hibernate.validator.internal` | 10 |
| `org.apache.catalina.core` | 8 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 8 |
| `com.fasterxml.jackson.databind` | 7 |
| `org.hibernate.persister.entity` | 5 |
| `ch.qos.logback.classic` | 3 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.7 | 345.86 | 36 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 21 | 0.4 | 19.47 | 22 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 21 | 0.2 | 10.73 | 12 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 22 | 0.1 | 2.44 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 21 | 0.0 | 2.18 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 2 | 0.0 | 19.09 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 38 | 0.0 | 0.87 | 2 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 21 | 0.0 | 1.35 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 22 | 0.0 | 1.28 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 22 | 0.0 | 1.21 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 9 | 12 | la base tiene más (ver nota) |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 7 | 7 | coincide |
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
| Registrar propietario | 9 | 32 | 78.0 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 1 | 11.1 % | 486 | 30 001 | 30 001 |
| Agendar cita | 7 | 1 | 12.5 % | 878 | 3 707 | 3 707 |
| Recepción (llegada) | 1 | 2 | 66.7 % | 30 000 | 30 000 | 30 000 |
| Iniciar atención | 1 | 2 | 66.7 % | 30 001 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 1 752 | 1 752 | 1 752 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 409 | 2 409 | 2 409 |
| Registrar receta | 1 | 0 | 0.0 % | 3 263 | 3 263 | 3 263 |
| Actualizar mascota | 1 | 0 | 0.0 % | 5 450 | 5 450 | 5 450 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 436 | 417 (96 %) | 30 000 | 30 001 | 30 001 | 74 % |
| apoderado | 41 | 32 (78 %) | 30 000 | 30 001 | 30 001 | 6 % |
| notificaciones | 35 | 14 (40 %) | 249 | 30 001 | 30 001 | 2 % |
| cumplimiento | 35 | 13 (37 %) | 528 | 30 001 | 30 001 | 2 % |
| navegacion | 35 | 13 (37 %) | 412 | 30 001 | 30 001 | 2 % |
| privacidad | 35 | 13 (37 %) | 155 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 34 | 13 (38 %) | 252 | 30 001 | 30 001 | 2 % |
| panel | 32 | 12 (38 %) | 888 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 8 | 7 (88 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 8 | 7 (88 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 28 | 413 | 447 | 0 % |
| 0.1 | 18 | 89 | 450 | 2 853 | 1 % |
| 0.3 | 32 | 62 | 3 539 | 8 958 | 0 % |
| 0.6 | 46 | 7 | 30 001 | 30 001 | 14 % |
| 0.8 | 59 | 14 | 30 001 | 30 001 | 100 % |
| 1.1 | 73 | 31 | 30 001 | 30 001 | 100 % |
| 1.4 | 87 | 15 | 30 001 | 30 001 | 100 % |
| 1.6 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.1 | 100 | 40 | 30 001 | 30 003 | 100 % |
| 2.4 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 2.6 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.1 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 3.4 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.6 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 4.1 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 4.6 | 90 | 35 | 30 001 | 30 001 | 100 % |
| 4.8 | 63 | 29 | 30 001 | 30 001 | 100 % |
| 5.1 | 38 | 9 | 30 001 | 30 001 | 100 % |
| 5.3 | 17 | 6 | 30 001 | 30 001 | 100 % |
| 5.6 | 8 | 2 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
