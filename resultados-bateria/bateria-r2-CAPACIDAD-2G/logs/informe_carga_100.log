# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 271 |
| Peticiones correctas (2xx) | 6 267 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 17 |
| Tiempo p95 (ms) | 257 |
| Tiempo p99 (ms) | 655 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 45 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 31 |
| Memoria pico (MB, de 2048) | 865 |
| Sentencias SQL | 172 396 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 271 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 4 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2363; fallidas: 4.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 313, atencion 154, cobro 92, cancelar 56; sin trabajo disponible: 253.
- CPU del backend: 45 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 31 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 865 MB (42 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 280 pausas, 15.4 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 423 MB (tope -Xmx1g).
- Clases cargadas: 25430 en total, 278 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 13 | 3807 |
| 0.1 | 22 | 22 | 5461 |
| 0.1 | 39 | 39 | 8773 |
| 0.2 | 66 | 66 | 13659 |
| 0.2 | 80 | 81 | 16071 |
| 0.3 | 80 | 81 | 16103 |
| 0.4 | 86 | 87 | 17103 |
| 0.5 | 110 | 112 | 21321 |
| 1.7 | 130 | 132 | 24661 |
| 2.4 | 132 | 134 | 24967 |
| 3.0 | 133 | 136 | 25269 |
| 3.5 | 133 | 136 | 25305 |
| 4.1 | 134 | 137 | 25355 |
| 5.0 | 134 | 137 | 25428 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 268 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 278 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 389 | 40.3 | 103.58 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 389 | 18.1 | 46.54 | 16 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 389 | 14.2 | 36.56 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 9.2 | 209.61 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 389 | 6.3 | 16.13 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 389 | 6.2 | 15.96 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 4.5 | 11.48 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 4.3 | 18.59 | 4 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 2.6 | 58.77 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 1.5 | 2.39 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 310 | 310 | coincide |
| Diagnósticos | 153 | 153 | coincide |
| Recetas | 153 | 153 | coincide |
| Pagos | 92 | 92 | coincide |
| Citas completadas | 153 | 153 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 36 | 115 | 337 |
| Registrar mascota | 313 | 0 | 0.0 % | 22 | 53 | 91 |
| Agendar cita | 310 | 3 | 1.0 % | 30 | 73 | 164 |
| Recepción (llegada) | 154 | 0 | 0.0 % | 17 | 89 | 251 |
| Iniciar atención | 153 | 1 | 0.6 % | 22 | 53 | 78 |
| Leer la consulta | 153 | 0 | 0.0 % | 14 | 38 | 50 |
| Registrar diagnóstico | 153 | 0 | 0.0 % | 13 | 37 | 72 |
| Registrar receta | 153 | 0 | 0.0 % | 14 | 41 | 64 |
| Actualizar mascota | 153 | 0 | 0.0 % | 13 | 35 | 58 |
| Cerrar la consulta | 153 | 0 | 0.0 % | 21 | 67 | 218 |
| Caja: crear punto de cobro | 69 | 0 | 0.0 % | 12 | 33 | 416 |
| Caja: vincular equipo | 69 | 0 | 0.0 % | 9 | 29 | 36 |
| Caja: abrir la caja | 69 | 0 | 0.0 % | 13 | 38 | 60 |
| Cobrar | 92 | 0 | 0.0 % | 14 | 48 | 395 |
| Cancelar cita | 56 | 0 | 0.0 % | 29 | 216 | 324 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| panel | 389 | 0 (0 %) | 212 | 762 | 1 502 | 34 % |
| login | 100 | 0 (0 %) | 339 | 973 | 1 550 | 13 % |
| reporte_clinico | 22 | 0 (0 %) | 1 599 | 2 662 | 4 439 | 12 % |
| apoderado | 313 | 0 (0 %) | 36 | 115 | 337 | 4 % |
| propietarios | 237 | 0 (0 %) | 40 | 135 | 232 | 4 % |
| cita | 313 | 3 (1 %) | 30 | 73 | 164 | 3 % |
| lista_de_historias | 231 | 0 (0 %) | 32 | 103 | 398 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 21 | 89 | 453 | 3 % |
| mascota | 313 | 0 (0 %) | 22 | 53 | 91 | 2 % |
| navegacion | 389 | 0 (0 %) | 11 | 30 | 201 | 2 % |
| historia | 231 | 0 (0 %) | 22 | 55 | 65 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 10 | 30 | 50 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 73 | 335 | 379 | 0 % |
| 0.2 | 24 | 144 | 648 | 1 550 | 0 % |
| 0.5 | 38 | 184 | 316 | 523 | 1 % |
| 0.7 | 51 | 241 | 264 | 447 | 0 % |
| 0.9 | 65 | 281 | 348 | 826 | 0 % |
| 1.2 | 79 | 336 | 339 | 487 | 0 % |
| 1.4 | 92 | 403 | 339 | 733 | 0 % |
| 1.7 | 100 | 424 | 318 | 543 | 0 % |
| 1.9 | 100 | 481 | 118 | 691 | 0 % |
| 2.2 | 100 | 347 | 87 | 762 | 0 % |
| 2.5 | 100 | 425 | 308 | 1 512 | 0 % |
| 2.7 | 100 | 411 | 184 | 532 | 0 % |
| 3.0 | 100 | 418 | 324 | 960 | 0 % |
| 3.2 | 100 | 346 | 100 | 569 | 0 % |
| 3.5 | 100 | 376 | 97 | 472 | 0 % |
| 3.7 | 100 | 383 | 89 | 127 | 0 % |
| 4.0 | 100 | 414 | 95 | 138 | 0 % |
| 4.2 | 100 | 398 | 117 | 399 | 0 % |
| 4.5 | 88 | 161 | 184 | 1 009 | 0 % |
| 4.7 | 48 | 25 | 405 | 406 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
