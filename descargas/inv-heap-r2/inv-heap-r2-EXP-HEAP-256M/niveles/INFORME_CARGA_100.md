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
| Peticiones hechas | 3 544 |
| Peticiones correctas (2xx) | 3 115 |
| Errores (%) | 12.1 % |
| Ritmo medio (peticiones/s) | 17.6 |
| Tiempo mediano (ms) | 17 |
| Tiempo p95 (ms) | 1 674 |
| Tiempo p99 (ms) | 16 279 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 50 |
| Tiempo con la CPU al tope (%) | 26 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 10 |
| Memoria pico (MB, de 512) | 501 |
| Sentencias SQL | 82 384 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 544 | 17.6 | no aplica (ramping-vus) | 12.1 % | 423 | 1 / 3 / 4 | 0 % | 122 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_100`:** k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1022; fallidas: 101.
- Acciones hechas: panel 160, agenda 129, historia 127, reportes 12, recepcion 196, atencion 79, cobro 37, cancelar 29; sin trabajo disponible: 167.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- Java agotó su memoria de trabajo (heap): 3 errores `OutOfMemoryError` en el registro.
- CPU del backend: 50 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 26 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 33 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 501 MB (98 % de 512 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.0 con unos 100 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 20.9 s.
- Java agotó el heap: 3 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 165 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 408 pausas, 46.5 s en total (22 % del tiempo de vida de la JVM); 134 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25341 en total, 553 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2099 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 23 | 23 | 6114 |
| 0.1 | 59 | 60 | 13443 |
| 0.1 | 74 | 74 | 16078 |
| 0.2 | 75 | 76 | 16409 |
| 0.3 | 79 | 80 | 17217 |
| 0.8 | 110 | 112 | 23955 |
| 1.7 | 110 | 115 | 24783 |
| 2.2 | 111 | 115 | 24967 |
| 2.5 | 111 | 115 | 25118 |
| 3.0 | 111 | 115 | 25256 |
| 3.2 | 111 | 115 | 25261 |
| 3.4 | 111 | 115 | 25339 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 509 |
| `java.lang.invoke.LambdaForm$DMH/` | 24 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 533 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `java.util.stream.ReduceOps` | 2 |
| `org.springframework.security.web` | 1 |
| `org.apache.catalina.AsyncDispatcher` | 1 |
| `java.util.function.IntBinaryOperator` | 1 |
| `java.util.stream.IntPipeline` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 17 | 4.4 | 257.92 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 222 | 3.1 | 14.14 | 19 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 108 | 1.8 | 16.7 | 11 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 222 | 1.7 | 7.8 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 14 | 1.2 | 82.85 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 315 | 0.7 | 2.09 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 329 | 0.3 | 0.84 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 315 | 0.3 | 0.85 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 222 | 0.3 | 1.18 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 107 | 0.2 | 1.62 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 166 | 166 | coincide |
| Mascotas | 159 | 160 | la base tiene más (ver nota) |
| Citas agendadas | 147 | 147 | coincide |
| Diagnósticos | 58 | 58 | coincide |
| Recetas | 58 | 58 | coincide |
| Pagos | 25 | 25 | coincide |
| Citas completadas | 58 | 58 | coincide |
| Citas canceladas | 27 | 27 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 166 | 30 | 15.3 % | 37 | 14 493 | 19 755 |
| Registrar mascota | 159 | 7 | 4.2 % | 27 | 7 170 | 13 294 |
| Agendar cita | 147 | 12 | 7.5 % | 38 | 705 | 8 921 |
| Recepción (llegada) | 69 | 10 | 12.7 % | 16 | 15 815 | 23 264 |
| Iniciar atención | 60 | 19 | 24.1 % | 21 | 12 796 | 27 934 |
| Leer la consulta | 59 | 1 | 1.7 % | 12 | 78 | 2 220 |
| Registrar diagnóstico | 58 | 2 | 3.3 % | 12 | 57 | 11 076 |
| Registrar receta | 58 | 2 | 3.3 % | 14 | 52 | 234 |
| Actualizar mascota | 58 | 2 | 3.3 % | 13 | 47 | 430 |
| Cerrar la consulta | 58 | 2 | 3.3 % | 24 | 395 | 617 |
| Caja: crear punto de cobro | 29 | 6 | 17.1 % | 13 | 14 355 | 20 856 |
| Caja: vincular equipo | 25 | 4 | 13.8 % | 13 | 8 913 | 9 389 |
| Caja: abrir la caja | 24 | 1 | 4.0 % | 18 | 109 | 927 |
| Cobrar | 25 | 1 | 3.8 % | 20 | 412 | 16 558 |
| Cancelar cita | 27 | 2 | 6.9 % | 28 | 13 367 | 14 862 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| citas_del_mes | 129 | 22 (17 %) | 30 | 18 406 | 22 473 | 15 % |
| navegacion | 260 | 23 (9 %) | 8 | 9 408 | 22 864 | 11 % |
| apoderado | 196 | 30 (15 %) | 37 | 14 493 | 19 755 | 11 % |
| lista_de_historias | 127 | 20 (16 %) | 33 | 13 114 | 16 341 | 7 % |
| checkin | 79 | 10 (13 %) | 16 | 15 815 | 23 264 | 6 % |
| iniciar | 79 | 19 (24 %) | 21 | 12 796 | 27 934 | 6 % |
| mascota | 166 | 7 (4 %) | 27 | 7 170 | 13 294 | 5 % |
| privacidad | 260 | 26 (10 %) | 7 | 462 | 14 103 | 4 % |
| historia | 107 | 6 (6 %) | 26 | 8 394 | 15 756 | 4 % |
| empleados | 129 | 29 (22 %) | 14 | 8 347 | 11 534 | 4 % |
| reporte_clinico | 12 | 5 (42 %) | 4 030 | 30 001 | 30 001 | 4 % |
| punto_cobro | 35 | 6 (17 %) | 13 | 14 355 | 20 856 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 349 | 372 | 0 % |
| 0.2 | 24 | 147 | 482 | 1 832 | 0 % |
| 0.5 | 38 | 185 | 322 | 392 | 0 % |
| 0.7 | 51 | 242 | 314 | 392 | 0 % |
| 0.9 | 65 | 279 | 351 | 2 035 | 0 % |
| 1.2 | 79 | 326 | 315 | 876 | 1 % |
| 1.4 | 92 | 408 | 348 | 1 392 | 0 % |
| 1.7 | 100 | 423 | 165 | 397 | 0 % |
| 1.9 | 100 | 462 | 313 | 481 | 0 % |
| 2.2 | 100 | 373 | 50 | 335 | 0 % |
| 2.5 | 100 | 76 | 3 659 | 5 966 | 1 % |
| 2.7 | 100 | 87 | 20 856 | 30 001 | 2 % |
| 3.0 | 100 | 298 | 18 135 | 26 090 | 85 % |
| 3.2 | 100 | 168 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
