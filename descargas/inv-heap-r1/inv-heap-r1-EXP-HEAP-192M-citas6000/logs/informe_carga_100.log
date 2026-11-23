# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 47 |
| Peticiones hechas | 3 934 |
| Peticiones correctas (2xx) | 3 576 |
| Errores (%) | 9.1 % |
| Ritmo medio (peticiones/s) | 12.8 |
| Tiempo mediano (ms) | 17 |
| Tiempo p95 (ms) | 299 |
| Tiempo p99 (ms) | 874 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 40 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 100 437 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 934 | 12.8 | no aplica (ramping-vus) | 9.1 % | 0 | 1 / 2 / 4 | 0 % | 121 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1344; fallidas: 0.
- Acciones hechas: panel 176, agenda 142, historia 131, reportes 12, recepcion 177, atencion 87, cobro 56, cancelar 34; sin trabajo disponible: 137.
- CPU del backend: 40 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 12 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 1.1 con unos 74 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **47 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.4 s.
- Correo: el servidor de pruebas recibió 177 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.6 %).
- GC: 408 pausas, 15.8 s en total (5 % del tiempo de vida de la JVM); 63 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25337 en total, 334 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2045 |
| 0.0 | 11 | 11 | 3772 |
| 0.0 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12305 |
| 0.1 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16116 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 83 | 84 | 17785 |
| 0.9 | 111 | 114 | 24322 |
| 1.7 | 110 | 115 | 24739 |
| 2.4 | 111 | 115 | 24904 |
| 3.0 | 111 | 115 | 25131 |
| 3.7 | 111 | 115 | 25272 |
| 4.8 | 111 | 115 | 25306 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 320 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 328 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `java.util.concurrent.locks` | 1 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 226 | 1.5 | 6.52 | 16 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 226 | 0.9 | 4.19 | 10 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 24 | 0.9 | 36.18 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 368 | 0.7 | 1.77 | 7 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 131 | 0.6 | 4.78 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 142 | 0.6 | 4.16 | 6 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 24 | 0.3 | 14.34 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 368 | 0.3 | 0.81 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 658 | 0.3 | 0.43 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 608 | 0.3 | 0.43 | 3 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 177 | 177 | coincide |
| Mascotas | 177 | 177 | coincide |
| Citas agendadas | 177 | 177 | coincide |
| Diagnósticos | 87 | 87 | coincide |
| Recetas | 87 | 87 | coincide |
| Pagos | 56 | 56 | coincide |
| Citas completadas | 87 | 87 | coincide |
| Citas canceladas | 34 | 34 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 177 | 0 | 0.0 % | 23 | 107 | 269 |
| Registrar mascota | 177 | 0 | 0.0 % | 15 | 57 | 115 |
| Agendar cita | 177 | 0 | 0.0 % | 18 | 104 | 337 |
| Recepción (llegada) | 87 | 0 | 0.0 % | 12 | 40 | 271 |
| Iniciar atención | 87 | 0 | 0.0 % | 13 | 53 | 238 |
| Leer la consulta | 87 | 0 | 0.0 % | 8 | 36 | 82 |
| Registrar diagnóstico | 87 | 0 | 0.0 % | 8 | 31 | 67 |
| Registrar receta | 87 | 0 | 0.0 % | 9 | 31 | 46 |
| Actualizar mascota | 87 | 0 | 0.0 % | 9 | 30 | 59 |
| Cerrar la consulta | 87 | 0 | 0.0 % | 15 | 71 | 423 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 15 | 33 | 65 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 11 | 37 | 50 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 14 | 38 | 41 |
| Cobrar | 56 | 0 | 0.0 % | 14 | 60 | 151 |
| Cancelar cita | 34 | 0 | 0.0 % | 19 | 264 | 313 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 408 | 358 (88 %) | 276 | 1 110 | 1 489 | 58 % |
| reporte_clinico | 12 | 0 (0 %) | 1 267 | 2 148 | 2 148 | 7 % |
| panel | 226 | 0 (0 %) | 38 | 100 | 360 | 5 % |
| propietarios | 142 | 0 (0 %) | 29 | 168 | 380 | 3 % |
| apoderado | 177 | 0 (0 %) | 23 | 107 | 269 | 3 % |
| cita | 177 | 0 (0 %) | 18 | 104 | 337 | 3 % |
| navegacion | 226 | 0 (0 %) | 9 | 39 | 225 | 2 % |
| citas_del_mes | 142 | 0 (0 %) | 23 | 70 | 118 | 2 % |
| lista_de_historias | 131 | 0 (0 %) | 21 | 92 | 234 | 2 % |
| mascota | 177 | 0 (0 %) | 15 | 57 | 115 | 2 % |
| cumplimiento | 226 | 0 (0 %) | 8 | 31 | 48 | 1 % |
| avisos_de_cumplimiento | 226 | 0 (0 %) | 6 | 26 | 230 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 287 | 317 | 0 % |
| 0.1 | 19 | 103 | 374 | 1 455 | 0 % |
| 0.4 | 33 | 179 | 260 | 306 | 0 % |
| 0.6 | 47 | 234 | 247 | 282 | 0 % |
| 0.9 | 60 | 207 | 255 | 696 | 5 % |
| 1.1 | 74 | 177 | 263 | 856 | 8 % |
| 1.4 | 88 | 208 | 716 | 995 | 12 % |
| 1.6 | 100 | 263 | 349 | 634 | 11 % |
| 1.9 | 100 | 215 | 433 | 709 | 11 % |
| 2.1 | 100 | 249 | 462 | 631 | 13 % |
| 2.4 | 100 | 228 | 1 358 | 1 746 | 11 % |
| 2.6 | 100 | 214 | 509 | 1 139 | 12 % |
| 2.9 | 100 | 246 | 437 | 726 | 11 % |
| 3.1 | 100 | 210 | 298 | 639 | 13 % |
| 3.4 | 100 | 213 | 279 | 475 | 11 % |
| 3.6 | 100 | 225 | 38 | 266 | 13 % |
| 3.9 | 100 | 237 | 30 | 40 | 11 % |
| 4.1 | 100 | 240 | 51 | 290 | 12 % |
| 4.4 | 100 | 174 | 264 | 1 108 | 7 % |
| 4.6 | 62 | 70 | 38 | 216 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
