# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Peticiones hechas | 6 276 |
| Peticiones correctas (2xx) | 6 264 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 127 |
| Tiempo p99 (ms) | 771 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 54 |
| Tiempo con la CPU al tope (%) | 24 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 21 |
| Memoria pico (MB, de 2048) | 1 000 |
| Sentencias SQL | 169 551 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 276 | 20.5 | no aplica (ramping-vus) | 0.2 % | 0 | 2 / 4 / 8 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2357; fallidas: 12.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 315, atencion 153, cobro 94, cancelar 53; sin trabajo disponible: 255.
- CPU del backend: 54 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 24 % del tiempo.
- CPU de PostgreSQL: 21 % de media y 68 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 000 MB (49 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 315 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.6 %).
- GC: 286 pausas, 19.4 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 515 MB (tope -Xmx1g).
- Clases cargadas: 25423 en total, 353 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 23 | 23 | 5564 |
| 0.2 | 43 | 43 | 9656 |
| 0.3 | 66 | 67 | 13760 |
| 0.3 | 80 | 81 | 16082 |
| 0.4 | 80 | 81 | 16120 |
| 0.5 | 86 | 87 | 17110 |
| 0.5 | 88 | 89 | 17312 |
| 1.1 | 123 | 125 | 23666 |
| 2.2 | 131 | 133 | 24795 |
| 2.9 | 132 | 135 | 25069 |
| 3.6 | 133 | 136 | 25298 |
| 4.5 | 134 | 137 | 25349 |
| 5.4 | 135 | 138 | 25422 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 341 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 352 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 11.6 | 262.74 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 9.5 | 24.43 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 5.6 | 14.49 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 5.0 | 21.71 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.2 | 72.83 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.2 | 3.53 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 0.9 | 1.49 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 0.9 | 1.46 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.7 | 1.78 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 232 | 0.7 | 2.82 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 315 | 315 | coincide |
| Mascotas | 315 | 315 | coincide |
| Citas agendadas | 305 | 305 | coincide |
| Diagnósticos | 151 | 151 | coincide |
| Recetas | 151 | 151 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 151 | 151 | coincide |
| Citas canceladas | 53 | 53 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 315 | 0 | 0.0 % | 33 | 147 | 408 |
| Registrar mascota | 315 | 0 | 0.0 % | 22 | 64 | 140 |
| Agendar cita | 305 | 10 | 3.2 % | 29 | 84 | 187 |
| Recepción (llegada) | 153 | 0 | 0.0 % | 13 | 85 | 501 |
| Iniciar atención | 151 | 2 | 1.3 % | 20 | 62 | 185 |
| Leer la consulta | 151 | 0 | 0.0 % | 11 | 43 | 175 |
| Registrar diagnóstico | 151 | 0 | 0.0 % | 10 | 38 | 77 |
| Registrar receta | 151 | 0 | 0.0 % | 10 | 41 | 135 |
| Actualizar mascota | 151 | 0 | 0.0 % | 11 | 40 | 99 |
| Cerrar la consulta | 151 | 0 | 0.0 % | 21 | 62 | 197 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 9 | 64 | 459 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 9 | 33 | 125 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 9 | 44 | 105 |
| Cobrar | 94 | 0 | 0.0 % | 14 | 47 | 129 |
| Cancelar cita | 53 | 0 | 0.0 % | 21 | 58 | 294 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 615 | 2 125 | 3 270 | 24 % |
| reporte_clinico | 22 | 0 (0 %) | 2 179 | 3 901 | 9 181 | 17 % |
| panel | 389 | 0 (0 %) | 86 | 232 | 487 | 12 % |
| apoderado | 315 | 0 (0 %) | 33 | 147 | 408 | 5 % |
| propietarios | 237 | 0 (0 %) | 42 | 140 | 419 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 38 | 127 | 521 | 4 % |
| cita | 315 | 10 (3 %) | 29 | 84 | 187 | 4 % |
| mascota | 315 | 0 (0 %) | 22 | 64 | 140 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 25 | 65 | 343 | 3 % |
| navegacion | 389 | 0 (0 %) | 12 | 54 | 371 | 3 % |
| historia | 232 | 0 (0 %) | 22 | 72 | 162 | 2 % |
| avisos_de_cumplimiento | 389 | 0 (0 %) | 10 | 36 | 142 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 35 | 469 | 482 | 0 % |
| 0.1 | 18 | 97 | 708 | 2 125 | 0 % |
| 0.3 | 32 | 169 | 945 | 3 270 | 0 % |
| 0.6 | 46 | 245 | 458 | 815 | 0 % |
| 0.8 | 59 | 261 | 389 | 1 308 | 0 % |
| 1.1 | 73 | 306 | 220 | 1 067 | 1 % |
| 1.4 | 87 | 370 | 214 | 1 363 | 0 % |
| 1.6 | 100 | 453 | 111 | 594 | 0 % |
| 1.9 | 100 | 413 | 102 | 501 | 0 % |
| 2.1 | 100 | 463 | 57 | 91 | 0 % |
| 2.4 | 100 | 412 | 214 | 889 | 0 % |
| 2.6 | 100 | 379 | 142 | 526 | 0 % |
| 2.9 | 100 | 411 | 79 | 317 | 0 % |
| 3.1 | 100 | 375 | 80 | 168 | 0 % |
| 3.4 | 100 | 361 | 57 | 83 | 0 % |
| 3.6 | 100 | 414 | 60 | 108 | 0 % |
| 3.9 | 100 | 374 | 67 | 149 | 0 % |
| 4.1 | 100 | 439 | 94 | 232 | 0 % |
| 4.3 | 99 | 236 | 122 | 1 644 | 1 % |
| 4.6 | 70 | 63 | 67 | 104 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
