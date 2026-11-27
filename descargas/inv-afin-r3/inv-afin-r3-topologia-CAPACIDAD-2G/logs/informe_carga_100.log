# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 6 282 |
| Peticiones correctas (2xx) | 6 273 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.7 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 147 |
| Tiempo p99 (ms) | 747 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 20 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 20 |
| Memoria pico (MB, de 2048) | 846 |
| Sentencias SQL | 168 791 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 282 | 20.7 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 8 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2366; fallidas: 9.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 313, atencion 154, cobro 94, cancelar 53; sin trabajo disponible: 256.
- CPU del backend: 58 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 20 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 52 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 846 MB (41 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.9 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+2.0 %).
- GC: 301 pausas, 21.0 s en total (6 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 423 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 373 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3819 |
| 0.1 | 24 | 25 | 5761 |
| 0.2 | 49 | 50 | 10933 |
| 0.3 | 73 | 74 | 14890 |
| 0.4 | 80 | 81 | 16107 |
| 0.5 | 82 | 83 | 16413 |
| 0.6 | 87 | 88 | 17213 |
| 0.8 | 102 | 103 | 19773 |
| 1.9 | 129 | 132 | 24551 |
| 2.7 | 132 | 134 | 24956 |
| 3.4 | 133 | 136 | 25265 |
| 4.0 | 134 | 137 | 25302 |
| 4.9 | 134 | 137 | 25358 |
| 5.6 | 135 | 138 | 25426 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 360 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 371 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 12.5 | 284.52 | 23 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 8.9 | 22.79 | 16 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 7.1 | 18.31 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 5.3 | 22.88 | 10 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 237 | 3.2 | 13.42 | 6 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.1 | 71.3 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.4 | 3.89 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 1.1 | 1.68 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.8 | 1.96 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 232 | 0.7 | 3.01 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 305 | 305 | coincide |
| Diagnósticos | 153 | 153 | coincide |
| Recetas | 153 | 153 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 153 | 153 | coincide |
| Citas canceladas | 53 | 53 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 42 | 149 | 387 |
| Registrar mascota | 313 | 0 | 0.0 % | 29 | 83 | 256 |
| Agendar cita | 305 | 8 | 2.6 % | 37 | 109 | 233 |
| Recepción (llegada) | 154 | 0 | 0.0 % | 18 | 129 | 322 |
| Iniciar atención | 153 | 1 | 0.6 % | 28 | 81 | 173 |
| Leer la consulta | 153 | 0 | 0.0 % | 16 | 42 | 56 |
| Registrar diagnóstico | 153 | 0 | 0.0 % | 17 | 43 | 88 |
| Registrar receta | 153 | 0 | 0.0 % | 18 | 57 | 198 |
| Actualizar mascota | 153 | 0 | 0.0 % | 16 | 48 | 79 |
| Cerrar la consulta | 153 | 0 | 0.0 % | 32 | 104 | 190 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 13 | 124 | 242 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 13 | 46 | 92 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 16 | 57 | 80 |
| Cobrar | 94 | 0 | 0.0 % | 19 | 67 | 452 |
| Cancelar cita | 53 | 0 | 0.0 % | 29 | 88 | 108 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 618 | 1 980 | 2 491 | 20 % |
| reporte_clinico | 22 | 0 (0 %) | 2 616 | 4 543 | 11 266 | 17 % |
| panel | 389 | 0 (0 %) | 98 | 251 | 484 | 12 % |
| apoderado | 313 | 0 (0 %) | 42 | 149 | 387 | 5 % |
| propietarios | 237 | 0 (0 %) | 60 | 161 | 235 | 5 % |
| cita | 313 | 8 (3 %) | 37 | 109 | 233 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 40 | 164 | 576 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 42 | 136 | 416 | 3 % |
| mascota | 313 | 0 (0 %) | 29 | 83 | 256 | 3 % |
| navegacion | 389 | 0 (0 %) | 16 | 85 | 514 | 3 % |
| historia | 232 | 0 (0 %) | 25 | 82 | 103 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 16 | 43 | 86 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 471 | 471 | 0 % |
| 0.1 | 16 | 90 | 434 | 580 | 0 % |
| 0.3 | 29 | 152 | 1 894 | 2 491 | 1 % |
| 0.6 | 43 | 237 | 487 | 854 | 1 % |
| 0.8 | 57 | 250 | 342 | 882 | 0 % |
| 1.1 | 70 | 288 | 436 | 1 063 | 0 % |
| 1.3 | 84 | 361 | 328 | 1 811 | 0 % |
| 1.6 | 98 | 447 | 130 | 643 | 0 % |
| 1.8 | 100 | 401 | 132 | 488 | 0 % |
| 2.0 | 100 | 444 | 102 | 206 | 0 % |
| 2.3 | 100 | 406 | 109 | 298 | 0 % |
| 2.5 | 100 | 367 | 163 | 581 | 0 % |
| 2.8 | 100 | 437 | 151 | 576 | 0 % |
| 3.0 | 100 | 406 | 88 | 179 | 0 % |
| 3.3 | 100 | 368 | 68 | 150 | 0 % |
| 3.5 | 100 | 399 | 59 | 85 | 0 % |
| 3.8 | 100 | 391 | 89 | 322 | 0 % |
| 4.0 | 100 | 396 | 86 | 173 | 0 % |
| 4.3 | 100 | 323 | 153 | 504 | 0 % |
| 4.5 | 80 | 105 | 124 | 1 686 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
