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
| Usuarios sin degradación | 100 |
| Peticiones hechas | 3 476 |
| Peticiones correctas (2xx) | 3 021 |
| Errores (%) | 13.1 % |
| Ritmo medio (peticiones/s) | 18.4 |
| Tiempo mediano (ms) | 19 |
| Tiempo p95 (ms) | 546 |
| Tiempo p99 (ms) | 13 947 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 29 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 11 |
| Memoria pico (MB, de 512) | 507 |
| Sentencias SQL | 79 601 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 476 | 18.4 | no aplica (ramping-vus) | 13.1 % | 447 | 1 / 4 / 6 | 0 % | 123 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_100`:** k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 989; fallidas: 101.
- Acciones hechas: panel 158, agenda 127, historia 125, reportes 12, recepcion 193, atencion 77, cobro 34, cancelar 28; sin trabajo disponible: 157.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- Java agotó su memoria de trabajo (heap): 3 errores `OutOfMemoryError` en el registro.
- CPU del backend: 58 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 29 % del tiempo.
- CPU de PostgreSQL: 11 % de media y 42 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 507 MB (99 % de 512 MB).
- Los fallos fueron: 98 % sin respuesta (tiempo agotado), 0 % errores 5xx del servidor, 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 2.6 con unos 100 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 16.1 s.
- Java agotó el heap: 3 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 156 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 383 pausas, 40.9 s en total (21 % del tiempo de vida de la JVM); 119 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25327 en total, 556 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 5 | 1922 |
| 0.0 | 11 | 11 | 3767 |
| 0.0 | 21 | 21 | 5676 |
| 0.1 | 46 | 47 | 11144 |
| 0.1 | 73 | 74 | 16048 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17208 |
| 0.5 | 105 | 107 | 22472 |
| 1.4 | 112 | 114 | 24570 |
| 1.9 | 110 | 115 | 24851 |
| 2.4 | 111 | 115 | 25040 |
| 2.8 | 111 | 115 | 25248 |
| 3.1 | 111 | 115 | 25251 |
| 3.3 | 111 | 115 | 25260 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 511 |
| `java.lang.invoke.LambdaForm$DMH/` | 24 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 535 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `java.util.stream.ReduceOps` | 2 |
| `java.util.concurrent.locks` | 1 |
| `org.springframework.security.web` | 1 |
| `org.apache.catalina.AsyncDispatcher` | 1 |
| `java.util.function.IntBinaryOperator` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.8 | 234.93 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 221 | 3.4 | 15.38 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 221 | 1.8 | 8.07 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 1.7 | 17.3 | 10 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 153 | 1.0 | 6.23 | 6 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 8 | 0.7 | 89.38 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 311 | 0.7 | 2.18 | 4 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 6 | 0.6 | 100.9 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 311 | 0.3 | 0.85 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 221 | 0.3 | 1.17 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 156 | 156 | coincide |
| Mascotas | 155 | 156 | la base tiene más (ver nota) |
| Citas agendadas | 146 | 146 | coincide |
| Diagnósticos | 57 | 58 | la base tiene más (ver nota) |
| Recetas | 57 | 57 | coincide |
| Pagos | 26 | 26 | coincide |
| Citas completadas | 57 | 57 | coincide |
| Citas canceladas | 24 | 24 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 156 | 37 | 19.2 % | 42 | 6 402 | 14 763 |
| Registrar mascota | 155 | 1 | 0.6 % | 29 | 198 | 15 676 |
| Agendar cita | 146 | 9 | 5.8 % | 41 | 164 | 1 063 |
| Recepción (llegada) | 62 | 15 | 19.5 % | 22 | 12 529 | 17 460 |
| Iniciar atención | 59 | 18 | 23.4 % | 25 | 496 | 13 014 |
| Leer la consulta | 58 | 1 | 1.7 % | 18 | 483 | 15 676 |
| Registrar diagnóstico | 57 | 2 | 3.4 % | 18 | 419 | 788 |
| Registrar receta | 57 | 2 | 3.4 % | 16 | 52 | 517 |
| Actualizar mascota | 57 | 2 | 3.4 % | 14 | 58 | 920 |
| Cerrar la consulta | 57 | 2 | 3.4 % | 28 | 384 | 987 |
| Caja: crear punto de cobro | 25 | 6 | 19.4 % | 21 | 10 997 | 15 352 |
| Caja: vincular equipo | 25 | 0 | 0.0 % | 17 | 48 | 456 |
| Caja: abrir la caja | 25 | 0 | 0.0 % | 15 | 55 | 585 |
| Cobrar | 26 | 2 | 7.1 % | 21 | 68 | 970 |
| Cancelar cita | 24 | 4 | 14.3 % | 45 | 13 885 | 15 758 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| citas_del_mes | 127 | 27 (21 %) | 40 | 13 150 | 16 007 | 13 % |
| navegacion | 258 | 27 (10 %) | 11 | 4 030 | 16 110 | 12 % |
| apoderado | 193 | 37 (19 %) | 42 | 6 402 | 14 763 | 12 % |
| lista_de_historias | 125 | 26 (21 %) | 38 | 10 004 | 15 635 | 10 % |
| checkin | 77 | 15 (19 %) | 22 | 12 529 | 17 460 | 7 % |
| reporte_clinico | 12 | 5 (42 %) | 5 277 | 22 910 | 22 910 | 6 % |
| login | 100 | 0 (0 %) | 376 | 2 172 | 2 553 | 5 % |
| privacidad | 258 | 35 (14 %) | 8 | 39 | 12 603 | 4 % |
| mascota | 156 | 1 (1 %) | 29 | 198 | 15 676 | 4 % |
| empleados | 127 | 34 (27 %) | 12 | 500 | 16 077 | 4 % |
| panel | 258 | 38 (15 %) | 55 | 490 | 1 062 | 3 % |
| punto_cobro | 31 | 6 (19 %) | 21 | 10 997 | 15 352 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 28 | 352 | 353 | 0 % |
| 0.1 | 18 | 90 | 391 | 1 451 | 0 % |
| 0.3 | 31 | 178 | 407 | 1 795 | 1 % |
| 0.6 | 45 | 239 | 335 | 403 | 0 % |
| 0.8 | 59 | 231 | 334 | 469 | 0 % |
| 1.1 | 72 | 311 | 391 | 2 172 | 0 % |
| 1.3 | 86 | 389 | 432 | 2 478 | 0 % |
| 1.6 | 99 | 443 | 73 | 402 | 0 % |
| 1.8 | 100 | 378 | 439 | 653 | 0 % |
| 2.1 | 100 | 476 | 91 | 413 | 0 % |
| 2.3 | 100 | 233 | 1 426 | 2 764 | 1 % |
| 2.6 | 100 | 255 | 16 077 | 17 460 | 87 % |
| 2.8 | 100 | 153 | 0 | 0 | 100 % |
| 3.1 | 100 | 72 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
