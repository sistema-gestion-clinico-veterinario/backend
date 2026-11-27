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
| Usuarios sin degradación | 23 |
| Peticiones hechas | 753 |
| Peticiones correctas (2xx) | 185 |
| Errores (%) | 75.4 % |
| Ritmo medio (peticiones/s) | 2.2 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 26 |
| Tiempo con la CPU al tope (%) | 23 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 5 606 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 753 | 2.2 | no aplica (ramping-vus) | 75.4 % | 568 | 0 / 1 / 5 | 0 % | 99 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 30; fallidas: 43.
- Acciones hechas: panel 14, agenda 6, historia 8, reportes 1, recepcion 41, atencion 4, cobro 0, cancelar 3; sin trabajo disponible: 26.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 13 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 6 avisos en el registro.
- CPU del backend: 26 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 23 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 12 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **23 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 13 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 12 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.5 %).
- GC: 442 pausas, 71.4 s en total (53 % del tiempo de vida de la JVM); 220 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24304 en total, 292 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2322 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8291 |
| 0.2 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16106 |
| 0.3 | 79 | 80 | 17106 |
| 0.4 | 86 | 88 | 18207 |
| 1.1 | 108 | 112 | 24003 |
| 1.3 | 109 | 112 | 24094 |
| 1.5 | 109 | 112 | 24107 |
| 1.7 | 109 | 112 | 24108 |
| 1.8 | 109 | 112 | 24110 |
| 2.0 | 109 | 112 | 24110 |
| 2.2 | 109 | 112 | 24110 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 175 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 5 |
| `java.lang.invoke.LambdaForm$BMH/` | 4 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$PeriodoVentas$$Lambda/` | 4 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 185 |
| `veterinaria.vargasvet.service.reportes` | 21 |
| `org.hibernate.query.sqm` | 13 |
| `veterinaria.vargasvet.service.impl` | 6 |
| `org.apache.catalina.core` | 6 |
| `ch.qos.logback.classic` | 5 |
| `org.springframework.web.servlet` | 3 |
| `org.springframework.transaction.support` | 3 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 308.81 | 34 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 21 | 0.4 | 18.64 | 22 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 21 | 0.2 | 10.97 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 22 | 0.0 | 2.24 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 21 | 0.0 | 1.93 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 2 | 0.0 | 18.29 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 22 | 0.0 | 1.15 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 38 | 0.0 | 0.65 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 21 | 0.0 | 1.1 | 1 % |
| `select a1_0.id from apoderado a1_0 left join usuario u1_0 on u1_0.id=a1_0.user_id left join company c1_0 on c1_0.id=a1_0.company_id where u1_0.id=$1 and c1_0.id=$2 fetch first $3 rows only` | 130 | 0.0 | 0.17 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 8 | 12 | la base tiene más (ver nota) |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 8 | 8 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 8 | 33 | 80.5 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 0 | 0.0 % | 492 | 1 572 | 1 572 |
| Agendar cita | 8 | 0 | 0.0 % | 697 | 2 889 | 2 889 |
| Recepción (llegada) | 1 | 3 | 75.0 % | 30 000 | 30 001 | 30 001 |
| Iniciar atención | 1 | 3 | 75.0 % | 30 000 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 566 | 566 | 566 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 678 | 2 678 | 2 678 |
| Registrar receta | 1 | 0 | 0.0 % | 4 507 | 4 507 | 4 507 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 431 | 411 (95 %) | 30 001 | 30 001 | 30 001 | 72 % |
| apoderado | 41 | 33 (80 %) | 30 000 | 30 001 | 30 001 | 6 % |
| cumplimiento | 36 | 14 (39 %) | 433 | 30 001 | 30 001 | 2 % |
| privacidad | 36 | 14 (39 %) | 267 | 30 001 | 30 001 | 2 % |
| navegacion | 36 | 14 (39 %) | 272 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 35 | 14 (40 %) | 94 | 30 001 | 30 001 | 2 % |
| notificaciones | 35 | 14 (40 %) | 88 | 30 001 | 30 001 | 2 % |
| panel | 34 | 13 (38 %) | 838 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 9 | 8 (89 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 9 | 8 (89 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 8 | 7 (88 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 70 | 408 | 440 | 0 % |
| 0.2 | 23 | 83 | 1 757 | 4 147 | 0 % |
| 0.4 | 37 | 32 | 5 962 | 15 514 | 0 % |
| 0.7 | 50 | 5 | 30 001 | 30 001 | 100 % |
| 0.9 | 64 | 22 | 30 001 | 30 001 | 100 % |
| 1.2 | 78 | 25 | 30 001 | 30 001 | 100 % |
| 1.4 | 91 | 22 | 30 001 | 30 001 | 100 % |
| 1.7 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.4 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 3.4 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 4.4 | 99 | 35 | 30 001 | 30 001 | 100 % |
| 4.7 | 84 | 28 | 30 001 | 30 001 | 100 % |
| 4.9 | 55 | 27 | 30 001 | 30 001 | 100 % |
| 5.2 | 34 | 7 | 30 001 | 30 001 | 100 % |
| 5.4 | 10 | 4 | 30 000 | 30 000 | 100 % |
| 5.7 | 7 | 2 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
