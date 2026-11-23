# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 41 |
| Peticiones hechas | 3 938 |
| Peticiones correctas (2xx) | 3 578 |
| Errores (%) | 9.1 % |
| Ritmo medio (peticiones/s) | 12.5 |
| Tiempo mediano (ms) | 23 |
| Tiempo p95 (ms) | 500 |
| Tiempo p99 (ms) | 2 702 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 55 |
| Tiempo con la CPU al tope (%) | 25 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 497 |
| Sentencias SQL | 101 096 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 938 | 12.5 | no aplica (ramping-vus) | 9.1 % | 0 | 1 / 2 / 4 | 0 % | 121 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1336; fallidas: 0.
- Acciones hechas: panel 174, agenda 141, historia 130, reportes 12, recepcion 175, atencion 87, cobro 55, cancelar 33; sin trabajo disponible: 137.
- Java agotó su memoria de trabajo (heap): 2 errores `OutOfMemoryError` en el registro.
- CPU del backend: 55 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 25 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 22 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 497 MB (97 % de 512 MB).
- Los fallos fueron: 0 % errores 5xx del servidor, 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 69 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **41 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 8.0 s.
- Java agotó el heap: 2 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 175 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.7 %).
- GC: 420 pausas, 38.7 s en total (12 % del tiempo de vida de la JVM); 119 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25422 en total, 395 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2132 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 26 | 26 | 6796 |
| 0.1 | 61 | 61 | 13687 |
| 0.1 | 74 | 75 | 16103 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 93 | 94 | 19768 |
| 1.4 | 112 | 114 | 24562 |
| 2.0 | 110 | 115 | 24836 |
| 2.8 | 111 | 115 | 25084 |
| 3.4 | 111 | 115 | 25182 |
| 4.8 | 111 | 115 | 25313 |
| 5.1 | 111 | 115 | 25344 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 357 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 369 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `java.util.concurrent.locks` | 1 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 24 | 5.1 | 212.05 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 224 | 3.1 | 13.81 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.8 | 13.55 | 9 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 224 | 1.6 | 7.13 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 22 | 1.2 | 52.97 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 365 | 0.8 | 2.12 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 365 | 0.4 | 1.02 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 609 | 0.3 | 0.55 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 659 | 0.3 | 0.49 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 365 | 0.3 | 0.86 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 175 | 175 | coincide |
| Mascotas | 175 | 175 | coincide |
| Citas agendadas | 175 | 175 | coincide |
| Diagnósticos | 87 | 87 | coincide |
| Recetas | 87 | 87 | coincide |
| Pagos | 55 | 55 | coincide |
| Citas completadas | 87 | 87 | coincide |
| Citas canceladas | 33 | 33 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 175 | 0 | 0.0 % | 37 | 416 | 5 907 |
| Registrar mascota | 175 | 0 | 0.0 % | 23 | 122 | 499 |
| Agendar cita | 175 | 0 | 0.0 % | 33 | 157 | 389 |
| Recepción (llegada) | 87 | 0 | 0.0 % | 25 | 257 | 7 322 |
| Iniciar atención | 87 | 0 | 0.0 % | 35 | 83 | 543 |
| Leer la consulta | 87 | 0 | 0.0 % | 20 | 60 | 79 |
| Registrar diagnóstico | 87 | 0 | 0.0 % | 16 | 62 | 438 |
| Registrar receta | 87 | 0 | 0.0 % | 15 | 58 | 92 |
| Actualizar mascota | 87 | 0 | 0.0 % | 13 | 69 | 485 |
| Cerrar la consulta | 87 | 0 | 0.0 % | 20 | 106 | 142 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 20 | 106 | 327 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 18 | 67 | 73 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 18 | 354 | 415 |
| Cobrar | 55 | 0 | 0.0 % | 18 | 100 | 5 184 |
| Cancelar cita | 33 | 0 | 0.0 % | 26 | 266 | 1 066 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 409 | 359 (88 %) | 356 | 2 598 | 3 567 | 48 % |
| reporte_clinico | 12 | 1 (8 %) | 4 376 | 17 322 | 17 322 | 14 % |
| apoderado | 175 | 0 (0 %) | 37 | 416 | 5 907 | 5 % |
| panel | 224 | 0 (0 %) | 60 | 250 | 505 | 5 % |
| navegacion | 224 | 0 (0 %) | 10 | 95 | 4 896 | 4 % |
| historia | 130 | 0 (0 %) | 29 | 134 | 2 577 | 3 % |
| lista_de_historias | 130 | 0 (0 %) | 36 | 363 | 1 120 | 3 % |
| propietarios | 141 | 0 (0 %) | 66 | 279 | 518 | 2 % |
| checkin | 87 | 0 (0 %) | 25 | 257 | 7 322 | 2 % |
| citas_del_mes | 141 | 0 (0 %) | 34 | 345 | 471 | 2 % |
| cita | 175 | 0 (0 %) | 33 | 157 | 389 | 2 % |
| mascota | 175 | 0 (0 %) | 23 | 122 | 499 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 14 | 94 | 342 | 367 | 0 % |
| 0.3 | 28 | 154 | 402 | 1 815 | 0 % |
| 0.5 | 41 | 228 | 319 | 356 | 0 % |
| 0.8 | 55 | 221 | 314 | 397 | 2 % |
| 1.0 | 69 | 186 | 347 | 1 389 | 7 % |
| 1.3 | 82 | 190 | 833 | 2 325 | 8 % |
| 1.5 | 96 | 239 | 2 634 | 3 954 | 13 % |
| 1.8 | 100 | 244 | 613 | 1 244 | 12 % |
| 2.0 | 100 | 224 | 518 | 1 624 | 10 % |
| 2.3 | 100 | 241 | 1 786 | 2 744 | 12 % |
| 2.5 | 100 | 219 | 2 539 | 3 285 | 13 % |
| 2.8 | 100 | 225 | 775 | 1 063 | 14 % |
| 3.0 | 100 | 220 | 481 | 989 | 10 % |
| 3.3 | 100 | 254 | 341 | 471 | 13 % |
| 3.5 | 100 | 207 | 53 | 361 | 12 % |
| 3.8 | 100 | 226 | 46 | 69 | 12 % |
| 4.0 | 100 | 244 | 29 | 51 | 8 % |
| 4.3 | 100 | 215 | 367 | 502 | 11 % |
| 4.5 | 83 | 60 | 948 | 1 120 | 2 % |
| 4.8 | 34 | 47 | 8 018 | 17 322 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
