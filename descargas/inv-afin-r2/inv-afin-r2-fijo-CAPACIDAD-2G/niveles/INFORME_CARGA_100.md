# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 289 |
| Peticiones correctas (2xx) | 6 273 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 19 |
| Tiempo p95 (ms) | 129 |
| Tiempo p99 (ms) | 719 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 55 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 20 |
| Memoria pico (MB, de 2048) | 913 |
| Sentencias SQL | 168 991 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 289 | 20.5 | no aplica (ramping-vus) | 0.3 % | 0 | 2 / 5 / 8 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2366; fallidas: 16.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 314, atencion 155, cobro 93, cancelar 54; sin trabajo disponible: 252.
- CPU del backend: 55 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 70 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 913 MB (45 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.8 s.
- Correo: el servidor de pruebas recibió 314 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 297 pausas, 20.1 s en total (6 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 432 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 288 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 22 | 23 | 5564 |
| 0.2 | 43 | 43 | 9673 |
| 0.3 | 66 | 67 | 13759 |
| 0.3 | 80 | 81 | 16082 |
| 0.4 | 80 | 81 | 16121 |
| 0.5 | 86 | 87 | 17110 |
| 0.6 | 89 | 90 | 17420 |
| 1.3 | 125 | 127 | 24145 |
| 2.2 | 131 | 133 | 24808 |
| 2.9 | 132 | 135 | 25071 |
| 3.6 | 133 | 136 | 25300 |
| 4.2 | 134 | 137 | 25351 |
| 5.2 | 134 | 137 | 25424 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 276 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 286 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |
| `veterinaria.vargasvet.service.reportes` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 12.2 | 278.41 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then $2 else $3 end),$4) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apo` | 389 | 9.2 | 23.74 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 389 | 5.6 | 14.36 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 4.9 | 21.1 | 9 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 248 | 4.5 | 18.21 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 40 | 3.0 | 76.23 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.2 | 3.53 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 626 | 0.9 | 1.47 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.7 | 1.78 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 232 | 0.6 | 2.78 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 314 | 314 | coincide |
| Mascotas | 314 | 314 | coincide |
| Citas agendadas | 299 | 299 | coincide |
| Diagnósticos | 154 | 154 | coincide |
| Recetas | 154 | 154 | coincide |
| Pagos | 93 | 93 | coincide |
| Citas completadas | 154 | 154 | coincide |
| Citas canceladas | 54 | 54 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 314 | 0 | 0.0 % | 34 | 175 | 337 |
| Registrar mascota | 314 | 0 | 0.0 % | 24 | 64 | 162 |
| Agendar cita | 299 | 15 | 4.8 % | 33 | 95 | 234 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 14 | 55 | 255 |
| Iniciar atención | 154 | 1 | 0.6 % | 21 | 71 | 176 |
| Leer la consulta | 154 | 0 | 0.0 % | 11 | 40 | 90 |
| Registrar diagnóstico | 154 | 0 | 0.0 % | 11 | 35 | 147 |
| Registrar receta | 154 | 0 | 0.0 % | 14 | 45 | 193 |
| Actualizar mascota | 154 | 0 | 0.0 % | 13 | 40 | 105 |
| Cerrar la consulta | 154 | 0 | 0.0 % | 23 | 78 | 487 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 10 | 201 | 534 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 9 | 42 | 73 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 10 | 40 | 85 |
| Cobrar | 93 | 0 | 0.0 % | 14 | 50 | 99 |
| Cancelar cita | 54 | 0 | 0.0 % | 17 | 69 | 84 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 633 | 2 286 | 2 990 | 24 % |
| reporte_clinico | 22 | 0 (0 %) | 2 281 | 4 337 | 8 460 | 17 % |
| panel | 389 | 0 (0 %) | 86 | 211 | 472 | 11 % |
| apoderado | 314 | 0 (0 %) | 34 | 175 | 337 | 5 % |
| propietarios | 237 | 0 (0 %) | 47 | 131 | 276 | 4 % |
| cita | 314 | 15 (5 %) | 33 | 95 | 234 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 38 | 124 | 604 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 39 | 156 | 381 | 4 % |
| mascota | 314 | 0 (0 %) | 24 | 64 | 162 | 3 % |
| navegacion | 389 | 0 (0 %) | 13 | 53 | 419 | 3 % |
| historia | 232 | 0 (0 %) | 19 | 67 | 339 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 11 | 38 | 74 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 63 | 442 | 569 | 0 % |
| 0.2 | 22 | 121 | 1 763 | 2 990 | 0 % |
| 0.4 | 36 | 174 | 675 | 1 739 | 1 % |
| 0.7 | 49 | 249 | 410 | 814 | 0 % |
| 0.9 | 63 | 279 | 446 | 1 022 | 0 % |
| 1.2 | 77 | 337 | 198 | 855 | 0 % |
| 1.4 | 90 | 389 | 182 | 1 721 | 1 % |
| 1.7 | 100 | 437 | 97 | 683 | 0 % |
| 1.9 | 100 | 417 | 95 | 201 | 0 % |
| 2.2 | 100 | 423 | 61 | 95 | 0 % |
| 2.4 | 100 | 418 | 143 | 245 | 0 % |
| 2.7 | 100 | 400 | 118 | 419 | 0 % |
| 2.9 | 100 | 428 | 124 | 678 | 0 % |
| 3.2 | 100 | 336 | 81 | 491 | 0 % |
| 3.4 | 100 | 407 | 61 | 94 | 0 % |
| 3.7 | 100 | 371 | 59 | 154 | 0 % |
| 3.9 | 100 | 408 | 59 | 97 | 0 % |
| 4.2 | 100 | 404 | 156 | 490 | 0 % |
| 4.4 | 95 | 187 | 162 | 2 344 | 1 % |
| 4.7 | 58 | 41 | 42 | 94 | 5 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
