# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Peticiones hechas | 3 670 |
| Peticiones correctas (2xx) | 3 305 |
| Errores (%) | 9.9 % |
| Ritmo medio (peticiones/s) | 10.5 |
| Tiempo mediano (ms) | 40 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 51 |
| Tiempo con la CPU al tope (%) | 38 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 508 |
| Sentencias SQL | 88 841 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 670 | 10.5 | no aplica (ramping-vus) | 9.9 % | 346 | 1 / 2 / 4 | 0 % | 123 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1077; fallidas: 107.
- Acciones hechas: panel 152, agenda 137, historia 135, reportes 11, recepcion 206, atencion 89, cobro 35, cancelar 30; sin trabajo disponible: 171.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 17 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 43 avisos en el registro.
- CPU del backend: 51 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 38 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 34 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 508 MB (99 % de 512 MB).
- Los fallos fueron: 95 % sin respuesta (tiempo agotado), 5 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 2.9 con unos 100 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 17 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 179 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.4 %).
- GC: 539 pausas, 98.3 s en total (31 % del tiempo de vida de la JVM); 267 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25433 en total, 453 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 8 | 8 | 2964 |
| 0.0 | 19 | 19 | 5379 |
| 0.1 | 60 | 61 | 13567 |
| 0.2 | 74 | 75 | 16106 |
| 0.3 | 79 | 80 | 17107 |
| 0.8 | 109 | 111 | 23650 |
| 1.8 | 110 | 115 | 24782 |
| 2.5 | 111 | 115 | 25037 |
| 3.0 | 111 | 115 | 25247 |
| 3.3 | 111 | 115 | 25253 |
| 3.5 | 111 | 115 | 25253 |
| 3.7 | 111 | 115 | 25375 |
| 4.0 | 111 | 115 | 25399 |
| 4.2 | 111 | 115 | 25422 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 379 |
| `java.lang.invoke.LambdaForm$DMH/` | 22 |
| `org.springframework.dao.support.PersistenceExceptionTranslationInterceptor$$Lambda/` | 4 |
| `java.lang.invoke.LambdaForm$BMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 405 |
| `org.apache.catalina.core` | 8 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.dao.support` | 5 |
| `java.lang.Throwable` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 21 | 40.5 | 1927.83 | 70 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 238 | 4.7 | 19.59 | 8 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 238 | 2.8 | 11.81 | 5 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 115 | 2.2 | 18.92 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 147 | 1.2 | 8.47 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 14 | 0.9 | 62.83 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 347 | 0.9 | 2.51 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 347 | 0.4 | 1.03 | 1 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 238 | 0.3 | 1.39 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 168 | 0.2 | 1.46 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 170 | 179 | la base tiene más (ver nota) |
| Mascotas | 169 | 169 | coincide |
| Citas agendadas | 160 | 161 | la base tiene más (ver nota) |
| Diagnósticos | 61 | 61 | coincide |
| Recetas | 61 | 61 | coincide |
| Pagos | 28 | 28 | coincide |
| Citas completadas | 60 | 60 | coincide |
| Citas canceladas | 26 | 28 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 170 | 36 | 17.5 % | 92 | 30 001 | 30 001 |
| Registrar mascota | 169 | 1 | 0.6 % | 36 | 658 | 12 831 |
| Agendar cita | 160 | 9 | 5.3 % | 51 | 1 087 | 30 000 |
| Recepción (llegada) | 75 | 14 | 15.7 % | 152 | 30 001 | 30 002 |
| Iniciar atención | 63 | 26 | 29.2 % | 75 | 30 001 | 30 001 |
| Leer la consulta | 62 | 1 | 1.6 % | 30 | 980 | 30 001 |
| Registrar diagnóstico | 61 | 2 | 3.2 % | 28 | 9 758 | 30 001 |
| Registrar receta | 61 | 2 | 3.2 % | 30 | 1 014 | 30 001 |
| Actualizar mascota | 61 | 2 | 3.2 % | 28 | 588 | 30 000 |
| Cerrar la consulta | 60 | 3 | 4.8 % | 48 | 1 081 | 30 001 |
| Caja: crear punto de cobro | 27 | 5 | 15.6 % | 29 | 30 001 | 30 001 |
| Caja: vincular equipo | 27 | 0 | 0.0 % | 22 | 634 | 2 663 |
| Caja: abrir la caja | 27 | 0 | 0.0 % | 27 | 1 151 | 12 179 |
| Cobrar | 28 | 2 | 6.7 % | 30 | 30 000 | 30 000 |
| Cancelar cita | 26 | 4 | 13.3 % | 69 | 30 001 | 30 002 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| apoderado | 206 | 36 (17 %) | 92 | 30 001 | 30 001 | 10 % |
| empleados | 139 | 30 (22 %) | 44 | 30 001 | 30 001 | 8 % |
| propietarios | 137 | 30 (22 %) | 149 | 30 001 | 30 001 | 8 % |
| citas_del_mes | 139 | 27 (19 %) | 168 | 30 001 | 30 001 | 8 % |
| privacidad | 271 | 29 (11 %) | 17 | 30 001 | 30 001 | 8 % |
| navegacion | 271 | 25 (9 %) | 19 | 30 001 | 30 001 | 7 % |
| lista_de_historias | 135 | 23 (17 %) | 94 | 30 001 | 30 001 | 7 % |
| cumplimiento | 265 | 25 (9 %) | 20 | 30 001 | 30 001 | 7 % |
| notificaciones | 261 | 22 (8 %) | 15 | 30 000 | 30 001 | 6 % |
| avisos_de_cumplimiento | 256 | 18 (7 %) | 14 | 30 000 | 30 001 | 5 % |
| checkin | 89 | 14 (16 %) | 152 | 30 001 | 30 002 | 5 % |
| panel | 252 | 14 (6 %) | 88 | 30 000 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 35 | 387 | 387 | 0 % |
| 0.1 | 18 | 93 | 547 | 1 993 | 0 % |
| 0.3 | 32 | 182 | 373 | 2 087 | 1 % |
| 0.6 | 46 | 239 | 341 | 404 | 0 % |
| 0.8 | 59 | 235 | 407 | 1 344 | 0 % |
| 1.1 | 73 | 305 | 451 | 1 847 | 0 % |
| 1.4 | 87 | 399 | 551 | 2 900 | 0 % |
| 1.6 | 100 | 451 | 91 | 463 | 0 % |
| 1.9 | 100 | 343 | 504 | 627 | 0 % |
| 2.1 | 100 | 425 | 1 210 | 1 766 | 1 % |
| 2.4 | 100 | 265 | 1 462 | 2 563 | 2 % |
| 2.6 | 100 | 3 | 3 165 | 3 165 | 0 % |
| 2.9 | 100 | 38 | 30 001 | 30 002 | 100 % |
| 3.1 | 100 | 76 | 30 001 | 30 001 | 55 % |
| 3.4 | 100 | 70 | 30 001 | 30 001 | 39 % |
| 3.6 | 100 | 112 | 30 001 | 30 001 | 33 % |
| 3.9 | 100 | 120 | 30 001 | 30 001 | 31 % |
| 4.1 | 100 | 88 | 30 001 | 30 001 | 38 % |
| 4.3 | 99 | 69 | 30 001 | 30 002 | 39 % |
| 4.6 | 89 | 54 | 30 001 | 30 001 | 76 % |
| 4.8 | 72 | 29 | 30 001 | 30 001 | 100 % |
| 5.1 | 59 | 25 | 30 001 | 30 001 | 100 % |
| 5.3 | 36 | 12 | 30 001 | 30 001 | 100 % |
| 5.6 | 14 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
