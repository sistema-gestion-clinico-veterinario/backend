# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 19 |
| Peticiones hechas | 6 261 |
| Peticiones correctas (2xx) | 6 250 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.4 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 160 |
| Tiempo p99 (ms) | 801 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 55 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 20 |
| Memoria pico (MB, de 2048) | 940 |
| Sentencias SQL | 168 414 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 261 | 20.4 | no aplica (ramping-vus) | 0.2 % | 0 | 2 / 5 / 7 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2346; fallidas: 11.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 314, atencion 153, cobro 92, cancelar 54; sin trabajo disponible: 256.
- CPU del backend: 55 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 53 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 940 MB (46 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **19 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 314 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+2.1 %).
- GC: 279 pausas, 19.9 s en total (6 % del tiempo de vida de la JVM); 25 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 473 MB (tope -Xmx1g).
- Clases cargadas: 25425 en total, 371 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 21 | 22 | 5295 |
| 0.2 | 37 | 38 | 8355 |
| 0.3 | 65 | 66 | 13634 |
| 0.4 | 80 | 81 | 16055 |
| 0.4 | 81 | 81 | 16110 |
| 0.5 | 86 | 87 | 17091 |
| 0.6 | 87 | 88 | 17219 |
| 1.3 | 123 | 125 | 23668 |
| 2.2 | 131 | 133 | 24794 |
| 2.9 | 132 | 135 | 25054 |
| 3.5 | 133 | 136 | 25265 |
| 4.2 | 134 | 137 | 25349 |
| 5.3 | 135 | 138 | 25424 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 359 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 370 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 12.4 | 281.19 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 6.9 | 17.65 | 13 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 6.7 | 17.32 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 5.2 | 22.43 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.1 | 70.77 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 237 | 2.7 | 11.31 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.3 | 3.74 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 1.0 | 1.57 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.8 | 2.03 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 231 | 0.7 | 2.82 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 314 | 314 | coincide |
| Mascotas | 314 | 314 | coincide |
| Citas agendadas | 306 | 306 | coincide |
| Diagnósticos | 150 | 150 | coincide |
| Recetas | 150 | 150 | coincide |
| Pagos | 92 | 92 | coincide |
| Citas completadas | 150 | 150 | coincide |
| Citas canceladas | 54 | 54 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 314 | 0 | 0.0 % | 48 | 220 | 523 |
| Registrar mascota | 314 | 0 | 0.0 % | 31 | 122 | 200 |
| Agendar cita | 306 | 8 | 2.5 % | 43 | 162 | 263 |
| Recepción (llegada) | 153 | 0 | 0.0 % | 16 | 127 | 658 |
| Iniciar atención | 150 | 3 | 2.0 % | 24 | 82 | 227 |
| Leer la consulta | 150 | 0 | 0.0 % | 15 | 54 | 234 |
| Registrar diagnóstico | 150 | 0 | 0.0 % | 14 | 51 | 231 |
| Registrar receta | 150 | 0 | 0.0 % | 17 | 53 | 81 |
| Actualizar mascota | 150 | 0 | 0.0 % | 18 | 55 | 100 |
| Cerrar la consulta | 150 | 0 | 0.0 % | 32 | 92 | 192 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 14 | 48 | 185 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 14 | 43 | 163 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 15 | 107 | 234 |
| Cobrar | 92 | 0 | 0.0 % | 18 | 46 | 155 |
| Cancelar cita | 54 | 0 | 0.0 % | 25 | 141 | 214 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 670 | 2 303 | 2 674 | 21 % |
| reporte_clinico | 22 | 0 (0 %) | 2 435 | 4 847 | 11 291 | 17 % |
| panel | 389 | 0 (0 %) | 94 | 235 | 338 | 11 % |
| apoderado | 314 | 0 (0 %) | 48 | 220 | 523 | 6 % |
| propietarios | 237 | 0 (0 %) | 62 | 183 | 327 | 4 % |
| cita | 314 | 8 (3 %) | 43 | 162 | 263 | 4 % |
| lista_de_historias | 231 | 0 (0 %) | 41 | 149 | 785 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 43 | 165 | 419 | 3 % |
| mascota | 314 | 0 (0 %) | 31 | 122 | 200 | 3 % |
| navegacion | 389 | 0 (0 %) | 15 | 68 | 307 | 3 % |
| cumplimiento | 389 | 0 (0 %) | 16 | 52 | 105 | 2 % |
| historia | 231 | 0 (0 %) | 24 | 92 | 207 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 429 | 477 | 0 % |
| 0.1 | 19 | 93 | 692 | 2 000 | 0 % |
| 0.4 | 33 | 183 | 890 | 2 674 | 1 % |
| 0.6 | 47 | 230 | 386 | 788 | 0 % |
| 0.9 | 60 | 266 | 341 | 1 101 | 0 % |
| 1.1 | 74 | 315 | 439 | 1 250 | 0 % |
| 1.4 | 88 | 371 | 448 | 1 788 | 0 % |
| 1.6 | 100 | 444 | 109 | 689 | 0 % |
| 1.9 | 100 | 405 | 115 | 422 | 0 % |
| 2.1 | 100 | 476 | 67 | 98 | 0 % |
| 2.4 | 100 | 394 | 234 | 811 | 0 % |
| 2.6 | 100 | 370 | 149 | 785 | 0 % |
| 2.9 | 100 | 417 | 81 | 138 | 0 % |
| 3.1 | 100 | 376 | 100 | 295 | 0 % |
| 3.4 | 100 | 369 | 68 | 93 | 0 % |
| 3.6 | 100 | 399 | 64 | 118 | 0 % |
| 3.9 | 100 | 382 | 87 | 235 | 0 % |
| 4.1 | 100 | 440 | 87 | 234 | 0 % |
| 4.4 | 99 | 229 | 155 | 1 611 | 0 % |
| 4.6 | 64 | 60 | 64 | 69 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
