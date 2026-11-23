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
| Peticiones hechas | 3 904 |
| Peticiones correctas (2xx) | 3 530 |
| Errores (%) | 9.6 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 26 |
| Tiempo p95 (ms) | 608 |
| Tiempo p99 (ms) | 3 202 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 65 |
| Tiempo con la CPU al tope (%) | 43 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 489 |
| Sentencias SQL | 100 235 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 904 | 11.2 | no aplica (ramping-vus) | 9.6 % | 16 | 1 / 2 / 4 | 0 % | 122 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1316; fallidas: 6.
- Acciones hechas: panel 171, agenda 141, historia 130, reportes 10, recepcion 175, atencion 88, cobro 53, cancelar 33; sin trabajo disponible: 137.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 3 errores `OutOfMemoryError` en el registro.
- CPU del backend: 65 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 43 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 28 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 489 MB (96 % de 512 MB).
- Los fallos fueron: 4 % sin respuesta (tiempo agotado), 95 % 401 (sesión perdida), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 69 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **41 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 3 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 175 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.4 %).
- GC: 546 pausas, 91.0 s en total (24 % del tiempo de vida de la JVM); 242 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25498 en total, 334 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 8 | 8 | 2967 |
| 0.0 | 19 | 19 | 5397 |
| 0.1 | 60 | 61 | 13558 |
| 0.2 | 74 | 75 | 16106 |
| 0.3 | 79 | 80 | 17210 |
| 0.9 | 111 | 113 | 24298 |
| 2.0 | 110 | 114 | 24838 |
| 3.0 | 111 | 114 | 25131 |
| 3.8 | 111 | 114 | 25271 |
| 5.1 | 111 | 114 | 25320 |
| 5.4 | 111 | 114 | 25379 |
| 5.6 | 111 | 114 | 25398 |
| 5.9 | 111 | 114 | 25436 |
| 6.1 | 111 | 114 | 25448 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 303 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 311 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `java.lang.Throwable` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `java.util.concurrent.locks` | 1 |
| `org.springframework.security.web` | 1 |
| `org.apache.catalina.AsyncDispatcher` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 23 | 4.3 | 188.82 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 221 | 3.0 | 13.8 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.8 | 13.8 | 10 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 221 | 1.6 | 7.22 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 22 | 0.9 | 40.49 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 362 | 0.8 | 2.18 | 4 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 607 | 0.3 | 0.56 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 362 | 0.3 | 0.91 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 657 | 0.3 | 0.5 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 362 | 0.3 | 0.86 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 172 | 175 | la base tiene más (ver nota) |
| Mascotas | 172 | 172 | coincide |
| Citas agendadas | 172 | 172 | coincide |
| Diagnósticos | 86 | 86 | coincide |
| Recetas | 86 | 86 | coincide |
| Pagos | 53 | 53 | coincide |
| Citas completadas | 86 | 86 | coincide |
| Citas canceladas | 33 | 33 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 172 | 3 | 1.7 % | 40 | 424 | 30 001 |
| Registrar mascota | 172 | 0 | 0.0 % | 26 | 111 | 603 |
| Agendar cita | 172 | 0 | 0.0 % | 39 | 207 | 453 |
| Recepción (llegada) | 87 | 1 | 1.1 % | 24 | 288 | 30 001 |
| Iniciar atención | 86 | 2 | 2.3 % | 29 | 111 | 30 001 |
| Leer la consulta | 86 | 0 | 0.0 % | 18 | 73 | 101 |
| Registrar diagnóstico | 86 | 0 | 0.0 % | 17 | 53 | 340 |
| Registrar receta | 86 | 0 | 0.0 % | 17 | 57 | 85 |
| Actualizar mascota | 86 | 0 | 0.0 % | 17 | 58 | 67 |
| Cerrar la consulta | 86 | 0 | 0.0 % | 34 | 126 | 528 |
| Caja: crear punto de cobro | 37 | 0 | 0.0 % | 24 | 243 | 272 |
| Caja: vincular equipo | 37 | 0 | 0.0 % | 24 | 64 | 524 |
| Caja: abrir la caja | 37 | 0 | 0.0 % | 27 | 68 | 155 |
| Cobrar | 53 | 0 | 0.0 % | 25 | 170 | 374 |
| Cancelar cita | 33 | 0 | 0.0 % | 35 | 371 | 7 455 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 407 | 357 (88 %) | 387 | 2 820 | 4 190 | 30 % |
| reporte_clinico | 12 | 2 (17 %) | 5 337 | 30 001 | 30 001 | 11 % |
| apoderado | 175 | 3 (2 %) | 40 | 424 | 30 001 | 10 % |
| navegacion | 223 | 2 (1 %) | 13 | 240 | 547 | 7 % |
| historia | 130 | 2 (2 %) | 34 | 129 | 30 000 | 7 % |
| privacidad | 223 | 2 (1 %) | 10 | 48 | 434 | 6 % |
| reporte_de_ventas | 12 | 2 (17 %) | 395 | 30 000 | 30 000 | 6 % |
| panel | 221 | 1 (0 %) | 65 | 247 | 518 | 5 % |
| checkin | 88 | 1 (1 %) | 24 | 288 | 30 001 | 3 % |
| iniciar | 88 | 2 (2 %) | 29 | 111 | 30 001 | 3 % |
| propietarios | 141 | 0 (0 %) | 63 | 304 | 582 | 1 % |
| lista_de_historias | 130 | 0 (0 %) | 41 | 321 | 2 147 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 1 | 355 | 355 | 0 % |
| 0.0 | 14 | 93 | 343 | 392 | 0 % |
| 0.3 | 28 | 154 | 1 027 | 1 971 | 0 % |
| 0.5 | 41 | 228 | 325 | 387 | 0 % |
| 0.8 | 55 | 224 | 322 | 396 | 2 % |
| 1.0 | 69 | 183 | 376 | 1 738 | 7 % |
| 1.3 | 82 | 184 | 709 | 2 008 | 8 % |
| 1.5 | 96 | 239 | 3 386 | 4 643 | 14 % |
| 1.8 | 100 | 244 | 796 | 1 115 | 12 % |
| 2.0 | 100 | 224 | 1 110 | 1 520 | 10 % |
| 2.3 | 100 | 228 | 1 516 | 2 805 | 12 % |
| 2.5 | 100 | 227 | 2 677 | 3 175 | 12 % |
| 2.8 | 100 | 225 | 763 | 1 390 | 13 % |
| 3.0 | 100 | 219 | 844 | 1 739 | 10 % |
| 3.3 | 100 | 251 | 321 | 729 | 13 % |
| 3.5 | 100 | 207 | 50 | 395 | 12 % |
| 3.8 | 100 | 225 | 47 | 85 | 13 % |
| 4.0 | 100 | 253 | 43 | 55 | 8 % |
| 4.3 | 100 | 207 | 341 | 475 | 11 % |
| 4.5 | 86 | 71 | 1 304 | 2 647 | 1 % |
| 4.8 | 32 | 1 | 7 455 | 7 455 | 0 % |
| 5.0 | 12 | 11 | 30 001 | 30 001 | 100 % |
| 5.5 | 7 | 5 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
