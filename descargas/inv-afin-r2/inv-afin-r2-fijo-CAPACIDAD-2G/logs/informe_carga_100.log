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
| Usuarios sin degradación | 4 |
| Peticiones hechas | 6 281 |
| Peticiones correctas (2xx) | 6 276 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 20 |
| Tiempo p95 (ms) | 136 |
| Tiempo p99 (ms) | 794 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 55 |
| Tiempo con la CPU al tope (%) | 24 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 18 |
| Memoria pico (MB, de 2048) | 865 |
| Sentencias SQL | 169 695 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 281 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 8 | 0 % | 136 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2372; fallidas: 5.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 313, atencion 155, cobro 93, cancelar 55; sin trabajo disponible: 257.
- CPU del backend: 55 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 24 % del tiempo.
- CPU de PostgreSQL: 18 % de media y 55 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 865 MB (42 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **4 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.2 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 312 pausas, 20.7 s en total (6 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 426 MB (tope -Xmx1g).
- Clases cargadas: 25423 en total, 297 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3819 |
| 0.1 | 24 | 25 | 5765 |
| 0.2 | 49 | 50 | 10923 |
| 0.3 | 73 | 74 | 14915 |
| 0.4 | 80 | 81 | 16107 |
| 0.5 | 82 | 83 | 16413 |
| 0.5 | 87 | 88 | 17213 |
| 0.7 | 100 | 102 | 19474 |
| 1.7 | 129 | 131 | 24528 |
| 2.4 | 131 | 134 | 24905 |
| 2.9 | 132 | 135 | 25052 |
| 3.7 | 133 | 136 | 25297 |
| 4.3 | 134 | 137 | 25348 |
| 5.3 | 134 | 137 | 25422 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 285 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 296 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 11.2 | 253.97 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 6.8 | 17.5 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 6.7 | 17.16 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 4.5 | 19.39 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.2 | 73.28 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.2 | 3.48 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 0.9 | 1.44 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 0.8 | 1.27 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.7 | 1.77 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 231 | 0.6 | 2.69 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 309 | 309 | coincide |
| Diagnósticos | 154 | 154 | coincide |
| Recetas | 154 | 154 | coincide |
| Pagos | 93 | 93 | coincide |
| Citas completadas | 154 | 154 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 35 | 173 | 419 |
| Registrar mascota | 313 | 0 | 0.0 % | 24 | 74 | 237 |
| Agendar cita | 309 | 4 | 1.3 % | 32 | 97 | 206 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 13 | 172 | 691 |
| Iniciar atención | 154 | 1 | 0.6 % | 21 | 71 | 165 |
| Leer la consulta | 154 | 0 | 0.0 % | 12 | 37 | 86 |
| Registrar diagnóstico | 154 | 0 | 0.0 % | 12 | 41 | 377 |
| Registrar receta | 154 | 0 | 0.0 % | 12 | 41 | 59 |
| Actualizar mascota | 154 | 0 | 0.0 % | 12 | 53 | 139 |
| Cerrar la consulta | 154 | 0 | 0.0 % | 23 | 86 | 242 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 12 | 54 | 449 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 11 | 42 | 154 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 15 | 38 | 51 |
| Cobrar | 93 | 0 | 0.0 % | 17 | 52 | 182 |
| Cancelar cita | 55 | 0 | 0.0 % | 19 | 81 | 571 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 707 | 2 003 | 2 507 | 24 % |
| reporte_clinico | 22 | 0 (0 %) | 2 233 | 3 886 | 8 737 | 16 % |
| panel | 389 | 0 (0 %) | 84 | 227 | 546 | 11 % |
| apoderado | 313 | 0 (0 %) | 35 | 173 | 419 | 5 % |
| propietarios | 237 | 0 (0 %) | 45 | 132 | 299 | 4 % |
| cita | 313 | 4 (1 %) | 32 | 97 | 206 | 4 % |
| lista_de_historias | 231 | 0 (0 %) | 35 | 132 | 332 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 26 | 228 | 412 | 3 % |
| mascota | 313 | 0 (0 %) | 24 | 74 | 237 | 3 % |
| navegacion | 389 | 0 (0 %) | 12 | 65 | 449 | 3 % |
| historia | 231 | 0 (0 %) | 20 | 68 | 94 | 2 % |
| checkin | 155 | 0 (0 %) | 13 | 172 | 691 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 28 | 467 | 798 | 0 % |
| 0.1 | 18 | 90 | 519 | 2 362 | 1 % |
| 0.3 | 31 | 170 | 1 173 | 2 507 | 0 % |
| 0.6 | 45 | 223 | 431 | 815 | 1 % |
| 0.8 | 58 | 257 | 480 | 1 117 | 0 % |
| 1.1 | 72 | 297 | 375 | 1 490 | 0 % |
| 1.3 | 86 | 379 | 389 | 1 160 | 0 % |
| 1.6 | 99 | 455 | 103 | 779 | 0 % |
| 1.8 | 100 | 391 | 136 | 487 | 0 % |
| 2.1 | 100 | 479 | 68 | 129 | 0 % |
| 2.3 | 100 | 390 | 156 | 704 | 0 % |
| 2.6 | 100 | 365 | 153 | 483 | 0 % |
| 2.8 | 100 | 409 | 166 | 665 | 0 % |
| 3.1 | 100 | 406 | 71 | 280 | 0 % |
| 3.3 | 100 | 389 | 73 | 167 | 0 % |
| 3.6 | 100 | 391 | 49 | 77 | 0 % |
| 3.8 | 100 | 385 | 62 | 147 | 0 % |
| 4.1 | 100 | 414 | 69 | 419 | 0 % |
| 4.3 | 100 | 294 | 180 | 1 509 | 0 % |
| 4.6 | 73 | 69 | 83 | 1 775 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
