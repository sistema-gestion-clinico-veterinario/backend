# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 6 330 |
| Peticiones correctas (2xx) | 6 322 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.6 |
| Tiempo mediano (ms) | 13 |
| Tiempo p95 (ms) | 81 |
| Tiempo p99 (ms) | 417 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 44 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 12 |
| Memoria pico (MB, de 2048) | 898 |
| Sentencias SQL | 170 029 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 330 | 20.6 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 4 | 0 % | 133 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2397; fallidas: 8.
- Acciones hechas: panel 291, agenda 238, historia 232, reportes 23, recepcion 316, atencion 156, cobro 95, cancelar 54; sin trabajo disponible: 259.
- CPU del backend: 44 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 12 % de media y 46 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 898 MB (44 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 316 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.8 %).
- GC: 296 pausas, 16.6 s en total (5 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 433 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 181 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3807 |
| 0.1 | 23 | 23 | 5564 |
| 0.1 | 43 | 43 | 9656 |
| 0.2 | 66 | 67 | 13760 |
| 0.3 | 80 | 81 | 16081 |
| 0.3 | 81 | 81 | 16120 |
| 0.4 | 86 | 87 | 17109 |
| 0.5 | 93 | 94 | 17945 |
| 1.4 | 128 | 130 | 24479 |
| 2.2 | 131 | 134 | 24909 |
| 2.9 | 133 | 136 | 25247 |
| 3.5 | 134 | 136 | 25301 |
| 4.2 | 134 | 137 | 25351 |
| 5.1 | 135 | 138 | 25425 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 176 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 180 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 46 | 7.8 | 169.97 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 391 | 6.1 | 15.57 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 391 | 3.7 | 9.38 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 3.4 | 14.79 | 10 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 238 | 3.0 | 12.74 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 46 | 2.1 | 45.26 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 629 | 1.5 | 2.34 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 629 | 0.6 | 0.96 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 391 | 0.5 | 1.24 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 316 | 0.4 | 1.19 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 316 | 316 | coincide |
| Mascotas | 316 | 316 | coincide |
| Citas agendadas | 308 | 308 | coincide |
| Diagnósticos | 156 | 156 | coincide |
| Recetas | 156 | 156 | coincide |
| Pagos | 95 | 95 | coincide |
| Citas completadas | 156 | 156 | coincide |
| Citas canceladas | 54 | 54 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 316 | 0 | 0.0 % | 22 | 78 | 318 |
| Registrar mascota | 316 | 0 | 0.0 % | 15 | 51 | 81 |
| Agendar cita | 308 | 8 | 2.5 % | 20 | 66 | 174 |
| Recepción (llegada) | 156 | 0 | 0.0 % | 9 | 39 | 389 |
| Iniciar atención | 156 | 0 | 0.0 % | 12 | 51 | 129 |
| Leer la consulta | 156 | 0 | 0.0 % | 6 | 29 | 38 |
| Registrar diagnóstico | 156 | 0 | 0.0 % | 6 | 28 | 70 |
| Registrar receta | 156 | 0 | 0.0 % | 8 | 28 | 88 |
| Actualizar mascota | 156 | 0 | 0.0 % | 8 | 27 | 57 |
| Cerrar la consulta | 156 | 0 | 0.0 % | 16 | 50 | 134 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 7 | 21 | 358 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 6 | 24 | 60 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 7 | 27 | 48 |
| Cobrar | 95 | 0 | 0.0 % | 11 | 45 | 309 |
| Cancelar cita | 54 | 0 | 0.0 % | 14 | 46 | 69 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 339 | 1 024 | 1 542 | 20 % |
| reporte_clinico | 23 | 0 (0 %) | 1 513 | 2 595 | 4 447 | 18 % |
| panel | 391 | 0 (0 %) | 54 | 126 | 218 | 12 % |
| apoderado | 316 | 0 (0 %) | 22 | 78 | 318 | 5 % |
| citas_del_mes | 238 | 0 (0 %) | 28 | 82 | 341 | 4 % |
| cita | 316 | 8 (3 %) | 20 | 66 | 174 | 4 % |
| propietarios | 238 | 0 (0 %) | 22 | 97 | 176 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 25 | 63 | 185 | 4 % |
| mascota | 316 | 0 (0 %) | 15 | 51 | 81 | 3 % |
| navegacion | 391 | 0 (0 %) | 8 | 29 | 200 | 3 % |
| cumplimiento | 391 | 0 (0 %) | 8 | 27 | 120 | 2 % |
| historia | 232 | 0 (0 %) | 13 | 42 | 60 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 70 | 337 | 375 | 0 % |
| 0.2 | 23 | 140 | 671 | 1 542 | 1 % |
| 0.4 | 37 | 172 | 390 | 493 | 1 % |
| 0.7 | 50 | 247 | 271 | 516 | 0 % |
| 0.9 | 64 | 276 | 266 | 1 024 | 0 % |
| 1.2 | 78 | 330 | 193 | 744 | 0 % |
| 1.4 | 91 | 406 | 95 | 433 | 0 % |
| 1.7 | 100 | 431 | 82 | 334 | 0 % |
| 1.9 | 100 | 467 | 49 | 373 | 0 % |
| 2.2 | 100 | 391 | 48 | 163 | 0 % |
| 2.4 | 100 | 422 | 49 | 212 | 0 % |
| 2.7 | 100 | 404 | 69 | 188 | 0 % |
| 2.9 | 100 | 426 | 57 | 453 | 0 % |
| 3.2 | 100 | 356 | 46 | 137 | 0 % |
| 3.4 | 100 | 387 | 39 | 65 | 0 % |
| 3.7 | 100 | 378 | 52 | 97 | 0 % |
| 3.9 | 100 | 419 | 44 | 53 | 0 % |
| 4.2 | 100 | 393 | 89 | 1 177 | 0 % |
| 4.4 | 93 | 177 | 109 | 1 024 | 0 % |
| 4.7 | 53 | 38 | 41 | 43 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
