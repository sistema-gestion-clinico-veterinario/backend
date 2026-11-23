# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Peticiones hechas | 412 |
| Peticiones correctas (2xx) | 152 |
| Errores (%) | 63.1 % |
| Ritmo medio (peticiones/s) | 1.4 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 25 |
| Tiempo con la CPU al tope (%) | 22 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 422 |
| Sentencias SQL | 4 848 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 412 | 1.4 | no aplica (ramping-vus) | 63.1 % | 259 | 0 / 1 / 2 | 0 % | 73 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 20; fallidas: 36.
- Acciones hechas: panel 8, agenda 3, historia 4, reportes 1, recepcion 33, atencion 1, cobro 0, cancelar 2; sin trabajo disponible: 19.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 11 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 5 avisos en el registro.
- CPU del backend: 25 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 22 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 6 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 422 MB (82 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **14 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 11 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 11 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+1.0 %).
- GC: 402 pausas, 56.4 s en total (50 % del tiempo de vida de la JVM); 180 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24328 en total, 509 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2099 |
| 0.0 | 11 | 12 | 3804 |
| 0.1 | 23 | 23 | 6152 |
| 0.1 | 60 | 60 | 13548 |
| 0.2 | 74 | 74 | 16079 |
| 0.3 | 75 | 76 | 16402 |
| 0.3 | 79 | 80 | 17210 |
| 0.4 | 100 | 101 | 21140 |
| 1.0 | 108 | 112 | 23956 |
| 1.2 | 108 | 112 | 24084 |
| 1.4 | 108 | 112 | 24089 |
| 1.5 | 108 | 112 | 24093 |
| 1.7 | 108 | 112 | 24099 |
| 1.8 | 108 | 112 | 24100 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 327 |
| `veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl$$Lambda/` | 11 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |
| `java.lang.invoke.LambdaForm$BMH/` | 8 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.impl.ControlPreventivoServiceImpl$$Lambda/` | 5 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 346 |
| `veterinaria.vargasvet.service.impl` | 23 |
| `org.hibernate.query.sql` | 17 |
| `org.hibernate.query.sqm` | 12 |
| `org.springframework.data.repository` | 10 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 7 |
| `com.fasterxml.jackson.databind` | 6 |
| `ch.qos.logback.classic` | 6 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 320.45 | 43 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 18 | 0.3 | 18.51 | 22 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 18 | 0.2 | 9.08 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 19 | 0.0 | 2.18 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 18 | 0.0 | 1.47 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 25.8 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 19 | 0.0 | 1.05 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 18.77 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 19 | 0.0 | 0.84 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 32 | 0.0 | 0.48 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 7 | 11 | la base tiene más (ver nota) |
| Mascotas | 7 | 7 | coincide |
| Citas agendadas | 4 | 6 | la base tiene más (ver nota) |
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
| Registrar propietario | 7 | 26 | 78.8 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 7 | 0 | 0.0 % | 945 | 1 902 | 1 902 |
| Agendar cita | 4 | 3 | 42.9 % | 1 496 | 30 000 | 30 000 |
| Recepción (llegada) | 1 | 0 | 0.0 % | 1 581 | 1 581 | 1 581 |
| Iniciar atención | 1 | 0 | 0.0 % | 3 722 | 3 722 | 3 722 |
| Leer la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Registrar diagnóstico | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Registrar receta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 2 | 100.0 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 164 | 147 (90 %) | 30 000 | 30 001 | 30 001 | 56 % |
| apoderado | 33 | 26 (79 %) | 30 000 | 30 001 | 30 001 | 10 % |
| navegacion | 31 | 12 (39 %) | 115 | 30 001 | 30 001 | 5 % |
| privacidad | 31 | 12 (39 %) | 71 | 30 001 | 30 001 | 5 % |
| notificaciones | 29 | 10 (34 %) | 44 | 30 001 | 30 001 | 4 % |
| cumplimiento | 29 | 10 (34 %) | 68 | 30 001 | 30 001 | 4 % |
| avisos_de_cumplimiento | 28 | 9 (32 %) | 37 | 30 001 | 30 001 | 4 % |
| panel | 25 | 8 (32 %) | 83 | 30 001 | 30 001 | 3 % |
| lista_de_historias | 4 | 3 (75 %) | 30 000 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 3 (75 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 4 | 3 (75 %) | 30 000 | 30 000 | 30 000 | 1 % |
| propietarios | 3 | 3 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 14 | 99 | 446 | 516 | 0 % |
| 0.3 | 28 | 53 | 3 722 | 6 503 | 2 % |
| 0.5 | 41 | 2 | 30 001 | 30 001 | 50 % |
| 0.8 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 1.0 | 50 | 24 | 30 001 | 30 001 | 100 % |
| 1.3 | 50 | 10 | 30 001 | 30 001 | 100 % |
| 1.5 | 50 | 22 | 30 001 | 30 001 | 100 % |
| 1.8 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 2.0 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 2.3 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 2.5 | 50 | 21 | 30 001 | 30 001 | 100 % |
| 2.8 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 3.0 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 3.3 | 50 | 20 | 30 001 | 30 001 | 100 % |
| 3.5 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 3.8 | 41 | 22 | 30 001 | 30 002 | 100 % |
| 4.0 | 26 | 7 | 30 001 | 30 001 | 100 % |
| 4.3 | 16 | 8 | 30 001 | 30 001 | 100 % |
| 4.5 | 11 | 2 | 30 001 | 30 001 | 100 % |
| 4.8 | 8 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
