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
| Usuarios sin degradación | 13 |
| Peticiones hechas | 6 260 |
| Peticiones correctas (2xx) | 6 254 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.6 |
| Tiempo mediano (ms) | 24 |
| Tiempo p95 (ms) | 155 |
| Tiempo p99 (ms) | 950 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 20 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 22 |
| Memoria pico (MB, de 2048) | 863 |
| Sentencias SQL | 169 092 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 260 | 20.6 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 8 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2350; fallidas: 6.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 310, atencion 154, cobro 93, cancelar 55; sin trabajo disponible: 254.
- CPU del backend: 58 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 20 % del tiempo.
- CPU de PostgreSQL: 22 % de media y 55 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 863 MB (42 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **13 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.2 s.
- Correo: el servidor de pruebas recibió 310 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+2.0 %).
- GC: 293 pausas, 20.4 s en total (6 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 426 MB (tope -Xmx1g).
- Clases cargadas: 25425 en total, 354 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 22 | 23 | 5564 |
| 0.2 | 43 | 43 | 9656 |
| 0.3 | 66 | 67 | 13757 |
| 0.3 | 80 | 81 | 16079 |
| 0.4 | 81 | 81 | 16121 |
| 0.5 | 87 | 88 | 17206 |
| 0.6 | 93 | 95 | 18063 |
| 1.4 | 126 | 128 | 24323 |
| 2.3 | 131 | 134 | 24863 |
| 2.9 | 132 | 135 | 25071 |
| 3.6 | 133 | 136 | 25299 |
| 4.2 | 134 | 137 | 25350 |
| 5.3 | 134 | 138 | 25423 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 342 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 353 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 12.6 | 285.25 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 10.2 | 26.26 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 7.2 | 18.59 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 5.9 | 25.43 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 4.0 | 91.1 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.3 | 3.72 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 1.0 | 1.59 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 0.9 | 1.5 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 231 | 0.7 | 3.22 | 1 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.7 | 1.81 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 310 | 310 | coincide |
| Mascotas | 310 | 310 | coincide |
| Citas agendadas | 306 | 306 | coincide |
| Diagnósticos | 152 | 152 | coincide |
| Recetas | 152 | 152 | coincide |
| Pagos | 93 | 93 | coincide |
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
| Registrar propietario | 310 | 0 | 0.0 % | 41 | 187 | 531 |
| Registrar mascota | 310 | 0 | 0.0 % | 30 | 101 | 245 |
| Agendar cita | 306 | 4 | 1.3 % | 42 | 127 | 250 |
| Recepción (llegada) | 154 | 0 | 0.0 % | 20 | 149 | 727 |
| Iniciar atención | 152 | 2 | 1.3 % | 26 | 100 | 299 |
| Leer la consulta | 152 | 0 | 0.0 % | 17 | 62 | 117 |
| Registrar diagnóstico | 152 | 0 | 0.0 % | 18 | 64 | 116 |
| Registrar receta | 152 | 0 | 0.0 % | 18 | 69 | 160 |
| Actualizar mascota | 152 | 0 | 0.0 % | 18 | 55 | 92 |
| Cerrar la consulta | 152 | 0 | 0.0 % | 30 | 102 | 181 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 16 | 93 | 319 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 17 | 41 | 186 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 19 | 59 | 232 |
| Cobrar | 93 | 0 | 0.0 % | 21 | 71 | 448 |
| Cancelar cita | 55 | 0 | 0.0 % | 24 | 82 | 279 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 807 | 2 545 | 3 124 | 24 % |
| reporte_clinico | 22 | 0 (0 %) | 2 400 | 5 029 | 10 076 | 16 % |
| panel | 389 | 0 (0 %) | 103 | 253 | 350 | 11 % |
| apoderado | 310 | 0 (0 %) | 41 | 187 | 531 | 5 % |
| cita | 310 | 4 (1 %) | 42 | 127 | 250 | 4 % |
| propietarios | 237 | 0 (0 %) | 51 | 161 | 278 | 4 % |
| lista_de_historias | 231 | 0 (0 %) | 44 | 148 | 658 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 33 | 197 | 540 | 3 % |
| mascota | 310 | 0 (0 %) | 30 | 101 | 245 | 3 % |
| navegacion | 389 | 0 (0 %) | 15 | 67 | 192 | 2 % |
| historia | 231 | 0 (0 %) | 25 | 96 | 134 | 2 % |
| privacidad | 389 | 0 (0 %) | 14 | 48 | 114 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 473 | 768 | 0 % |
| 0.2 | 27 | 143 | 2 235 | 2 802 | 1 % |
| 0.5 | 40 | 191 | 622 | 1 048 | 1 % |
| 0.8 | 54 | 234 | 494 | 1 026 | 0 % |
| 1.0 | 68 | 295 | 504 | 1 444 | 0 % |
| 1.2 | 81 | 346 | 207 | 1 049 | 0 % |
| 1.5 | 95 | 418 | 317 | 2 065 | 0 % |
| 1.8 | 100 | 427 | 135 | 678 | 0 % |
| 2.0 | 100 | 429 | 96 | 191 | 0 % |
| 2.2 | 100 | 393 | 105 | 626 | 0 % |
| 2.5 | 100 | 415 | 119 | 279 | 0 % |
| 2.8 | 100 | 413 | 133 | 448 | 0 % |
| 3.0 | 100 | 403 | 140 | 717 | 0 % |
| 3.2 | 100 | 353 | 74 | 204 | 0 % |
| 3.5 | 100 | 403 | 62 | 88 | 0 % |
| 3.8 | 100 | 394 | 82 | 414 | 0 % |
| 4.0 | 100 | 410 | 78 | 117 | 0 % |
| 4.2 | 100 | 358 | 130 | 677 | 0 % |
| 4.5 | 85 | 138 | 126 | 1 701 | 0 % |
| 4.8 | 38 | 6 | 71 | 71 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
