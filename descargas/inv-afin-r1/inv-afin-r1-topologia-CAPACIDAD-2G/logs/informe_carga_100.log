# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 324 |
| Peticiones correctas (2xx) | 6 315 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.7 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 81 |
| Tiempo p99 (ms) | 423 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 42 |
| Tiempo con la CPU al tope (%) | 4 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 12 |
| Memoria pico (MB, de 2048) | 874 |
| Sentencias SQL | 169 841 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 324 | 20.7 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 5 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2387; fallidas: 9.
- Acciones hechas: panel 291, agenda 238, historia 233, reportes 23, recepcion 315, atencion 156, cobro 94, cancelar 54; sin trabajo disponible: 257.
- CPU del backend: 42 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 4 % del tiempo.
- CPU de PostgreSQL: 12 % de media y 36 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 874 MB (43 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 315 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.7 %).
- GC: 289 pausas, 16.0 s en total (5 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 424 MB (tope -Xmx1g).
- Clases cargadas: 25428 en total, 180 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3807 |
| 0.1 | 22 | 23 | 5564 |
| 0.1 | 43 | 43 | 9676 |
| 0.2 | 66 | 67 | 13761 |
| 0.2 | 80 | 81 | 16082 |
| 0.3 | 80 | 81 | 16121 |
| 0.4 | 87 | 88 | 17212 |
| 0.6 | 110 | 112 | 21419 |
| 1.7 | 130 | 132 | 24641 |
| 2.4 | 132 | 134 | 24981 |
| 3.1 | 133 | 136 | 25266 |
| 3.6 | 134 | 136 | 25304 |
| 4.5 | 134 | 137 | 25360 |
| 5.2 | 135 | 138 | 25426 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 176 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 180 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 46 | 7.9 | 171.91 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 391 | 6.3 | 16.1 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 391 | 3.7 | 9.56 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 233 | 3.5 | 14.83 | 10 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 238 | 2.9 | 12.22 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 46 | 2.3 | 49.2 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 629 | 1.5 | 2.44 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 629 | 0.6 | 1.01 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 391 | 0.5 | 1.29 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 233 | 0.4 | 1.71 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 315 | 315 | coincide |
| Mascotas | 315 | 315 | coincide |
| Citas agendadas | 307 | 307 | coincide |
| Diagnósticos | 155 | 155 | coincide |
| Recetas | 155 | 155 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 155 | 155 | coincide |
| Citas canceladas | 54 | 54 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 315 | 0 | 0.0 % | 22 | 80 | 310 |
| Registrar mascota | 315 | 0 | 0.0 % | 16 | 46 | 150 |
| Agendar cita | 307 | 8 | 2.5 % | 21 | 62 | 95 |
| Recepción (llegada) | 156 | 0 | 0.0 % | 11 | 73 | 451 |
| Iniciar atención | 155 | 1 | 0.6 % | 12 | 47 | 135 |
| Leer la consulta | 155 | 0 | 0.0 % | 8 | 32 | 81 |
| Registrar diagnóstico | 155 | 0 | 0.0 % | 7 | 30 | 65 |
| Registrar receta | 155 | 0 | 0.0 % | 8 | 33 | 50 |
| Actualizar mascota | 155 | 0 | 0.0 % | 9 | 32 | 129 |
| Cerrar la consulta | 155 | 0 | 0.0 % | 16 | 48 | 90 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 8 | 34 | 351 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 7 | 27 | 36 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 6 | 27 | 58 |
| Cobrar | 94 | 0 | 0.0 % | 10 | 36 | 111 |
| Cancelar cita | 54 | 0 | 0.0 % | 16 | 36 | 48 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 341 | 983 | 1 409 | 19 % |
| reporte_clinico | 23 | 0 (0 %) | 1 501 | 2 558 | 4 395 | 17 % |
| panel | 391 | 0 (0 %) | 55 | 117 | 188 | 12 % |
| apoderado | 315 | 0 (0 %) | 22 | 80 | 310 | 5 % |
| citas_del_mes | 238 | 0 (0 %) | 29 | 105 | 290 | 4 % |
| propietarios | 238 | 0 (0 %) | 27 | 94 | 175 | 4 % |
| cita | 315 | 8 (3 %) | 21 | 62 | 95 | 4 % |
| lista_de_historias | 233 | 0 (0 %) | 27 | 80 | 286 | 4 % |
| mascota | 315 | 0 (0 %) | 16 | 46 | 150 | 3 % |
| navegacion | 391 | 0 (0 %) | 8 | 27 | 120 | 3 % |
| historia | 233 | 0 (0 %) | 15 | 42 | 108 | 2 % |
| privacidad | 391 | 0 (0 %) | 7 | 25 | 80 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 344 | 344 | 0 % |
| 0.1 | 16 | 96 | 339 | 1 297 | 0 % |
| 0.3 | 29 | 155 | 564 | 1 409 | 1 % |
| 0.6 | 43 | 233 | 339 | 525 | 1 % |
| 0.8 | 57 | 248 | 261 | 506 | 0 % |
| 1.1 | 70 | 301 | 258 | 597 | 0 % |
| 1.3 | 84 | 380 | 140 | 556 | 0 % |
| 1.6 | 98 | 437 | 81 | 361 | 0 % |
| 1.8 | 100 | 395 | 81 | 590 | 0 % |
| 2.0 | 100 | 453 | 44 | 71 | 0 % |
| 2.3 | 100 | 420 | 68 | 347 | 0 % |
| 2.5 | 100 | 371 | 83 | 192 | 0 % |
| 2.8 | 100 | 428 | 70 | 480 | 0 % |
| 3.0 | 100 | 396 | 43 | 138 | 0 % |
| 3.3 | 100 | 373 | 44 | 115 | 0 % |
| 3.5 | 100 | 399 | 44 | 67 | 0 % |
| 3.8 | 100 | 388 | 56 | 144 | 0 % |
| 4.0 | 100 | 410 | 47 | 110 | 0 % |
| 4.3 | 100 | 325 | 121 | 1 036 | 0 % |
| 4.5 | 78 | 102 | 52 | 449 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
