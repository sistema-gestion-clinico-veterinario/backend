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
| Peticiones hechas | 6 277 |
| Peticiones correctas (2xx) | 6 270 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 268 |
| Tiempo p99 (ms) | 729 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 45 |
| Tiempo con la CPU al tope (%) | 5 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 35 |
| Memoria pico (MB, de 2048) | 857 |
| Sentencias SQL | 172 442 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 277 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 4 / 6 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2357; fallidas: 7.
- Acciones hechas: panel 290, agenda 237, historia 232, reportes 22, recepcion 313, atencion 153, cobro 94, cancelar 55; sin trabajo disponible: 255.
- CPU del backend: 45 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 35 % de media y 87 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 857 MB (42 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.8 %).
- GC: 288 pausas, 15.9 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 424 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 277 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3807 |
| 0.1 | 23 | 24 | 5673 |
| 0.1 | 45 | 46 | 10181 |
| 0.2 | 66 | 67 | 13753 |
| 0.3 | 80 | 81 | 16075 |
| 0.3 | 80 | 81 | 16112 |
| 0.4 | 86 | 87 | 17103 |
| 0.5 | 95 | 97 | 18265 |
| 1.5 | 128 | 130 | 24485 |
| 2.3 | 131 | 134 | 24940 |
| 3.1 | 133 | 136 | 25267 |
| 3.7 | 133 | 136 | 25304 |
| 4.5 | 134 | 137 | 25358 |
| 5.3 | 134 | 137 | 25426 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 267 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 277 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 390 | 48.4 | 124.14 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 390 | 20.5 | 52.63 | 16 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 390 | 15.1 | 38.64 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 9.6 | 217.66 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 390 | 6.6 | 16.98 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 390 | 6.5 | 16.57 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 390 | 4.5 | 11.57 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 4.3 | 18.61 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 2.7 | 61.02 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 627 | 1.5 | 2.37 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 307 | 307 | coincide |
| Diagnósticos | 152 | 152 | coincide |
| Recetas | 152 | 152 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 152 | 152 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 41 | 113 | 272 |
| Registrar mascota | 313 | 0 | 0.0 % | 25 | 55 | 127 |
| Agendar cita | 307 | 6 | 1.9 % | 31 | 70 | 103 |
| Recepción (llegada) | 153 | 0 | 0.0 % | 16 | 107 | 497 |
| Iniciar atención | 152 | 1 | 0.7 % | 22 | 58 | 93 |
| Leer la consulta | 152 | 0 | 0.0 % | 11 | 35 | 63 |
| Registrar diagnóstico | 152 | 0 | 0.0 % | 11 | 39 | 148 |
| Registrar receta | 152 | 0 | 0.0 % | 12 | 40 | 110 |
| Actualizar mascota | 152 | 0 | 0.0 % | 12 | 37 | 49 |
| Cerrar la consulta | 152 | 0 | 0.0 % | 22 | 59 | 142 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 13 | 36 | 51 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 11 | 25 | 40 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 13 | 38 | 91 |
| Cobrar | 94 | 0 | 0.0 % | 19 | 43 | 93 |
| Cancelar cita | 55 | 0 | 0.0 % | 19 | 52 | 169 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| panel | 390 | 0 (0 %) | 249 | 877 | 1 097 | 36 % |
| login | 100 | 0 (0 %) | 335 | 990 | 1 454 | 12 % |
| reporte_clinico | 22 | 0 (0 %) | 1 616 | 2 961 | 4 379 | 11 % |
| apoderado | 313 | 0 (0 %) | 41 | 113 | 272 | 4 % |
| propietarios | 237 | 0 (0 %) | 38 | 159 | 249 | 4 % |
| cita | 313 | 6 (2 %) | 31 | 70 | 103 | 3 % |
| lista_de_historias | 232 | 0 (0 %) | 32 | 94 | 151 | 3 % |
| mascota | 313 | 0 (0 %) | 25 | 55 | 127 | 2 % |
| navegacion | 390 | 0 (0 %) | 11 | 38 | 270 | 2 % |
| citas_del_mes | 237 | 0 (0 %) | 24 | 80 | 159 | 2 % |
| historia | 232 | 0 (0 %) | 21 | 48 | 123 | 2 % |
| cumplimiento | 390 | 0 (0 %) | 11 | 30 | 44 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 329 | 385 | 0 % |
| 0.1 | 19 | 118 | 659 | 1 454 | 1 % |
| 0.4 | 33 | 166 | 337 | 505 | 1 % |
| 0.6 | 47 | 236 | 272 | 457 | 0 % |
| 0.9 | 60 | 280 | 403 | 693 | 0 % |
| 1.1 | 74 | 312 | 304 | 551 | 0 % |
| 1.4 | 88 | 360 | 327 | 700 | 0 % |
| 1.6 | 100 | 459 | 299 | 594 | 0 % |
| 1.9 | 100 | 416 | 205 | 505 | 0 % |
| 2.1 | 100 | 458 | 114 | 659 | 0 % |
| 2.4 | 100 | 402 | 290 | 1 081 | 0 % |
| 2.6 | 100 | 378 | 297 | 1 063 | 0 % |
| 2.9 | 100 | 407 | 170 | 533 | 0 % |
| 3.1 | 100 | 366 | 266 | 627 | 0 % |
| 3.4 | 100 | 376 | 111 | 793 | 0 % |
| 3.6 | 100 | 415 | 61 | 300 | 0 % |
| 3.9 | 100 | 382 | 133 | 884 | 0 % |
| 4.1 | 100 | 422 | 210 | 559 | 0 % |
| 4.4 | 99 | 233 | 286 | 1 162 | 0 % |
| 4.6 | 66 | 49 | 99 | 168 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
