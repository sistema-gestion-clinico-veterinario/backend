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
| Usuarios sin degradación | 8 |
| Peticiones hechas | 6 299 |
| Peticiones correctas (2xx) | 6 292 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 21 |
| Tiempo p95 (ms) | 152 |
| Tiempo p99 (ms) | 663 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 54 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 21 |
| Memoria pico (MB, de 2048) | 930 |
| Sentencias SQL | 170 028 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 299 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 8 | 0 % | 131 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2385; fallidas: 7.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 312, atencion 156, cobro 95, cancelar 53; sin trabajo disponible: 254.
- CPU del backend: 54 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 21 % de media y 62 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 930 MB (45 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **8 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.4 s.
- Correo: el servidor de pruebas recibió 312 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.9 %).
- GC: 303 pausas, 21.0 s en total (6 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 462 MB (tope -Xmx1g).
- Clases cargadas: 25428 en total, 352 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3819 |
| 0.1 | 24 | 25 | 5763 |
| 0.2 | 49 | 50 | 10926 |
| 0.3 | 73 | 74 | 14880 |
| 0.4 | 80 | 81 | 16107 |
| 0.4 | 80 | 81 | 16120 |
| 0.5 | 86 | 87 | 17110 |
| 0.6 | 89 | 90 | 17483 |
| 1.4 | 126 | 129 | 24343 |
| 2.4 | 131 | 134 | 24894 |
| 2.9 | 132 | 135 | 25075 |
| 3.7 | 133 | 136 | 25303 |
| 4.5 | 134 | 137 | 25354 |
| 5.5 | 134 | 137 | 25427 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 341 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 352 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 11.4 | 258.54 | 21 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 10.4 | 26.72 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 6.5 | 16.76 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 5.8 | 25.01 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.5 | 79.12 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.5 | 3.99 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 1.1 | 1.83 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 1.0 | 1.59 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.9 | 2.23 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 232 | 0.9 | 3.72 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 312 | 312 | coincide |
| Mascotas | 312 | 312 | coincide |
| Citas agendadas | 305 | 305 | coincide |
| Diagnósticos | 156 | 156 | coincide |
| Recetas | 156 | 156 | coincide |
| Pagos | 95 | 95 | coincide |
| Citas completadas | 156 | 156 | coincide |
| Citas canceladas | 53 | 53 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 312 | 0 | 0.0 % | 39 | 194 | 683 |
| Registrar mascota | 312 | 0 | 0.0 % | 25 | 72 | 205 |
| Agendar cita | 305 | 7 | 2.2 % | 35 | 126 | 419 |
| Recepción (llegada) | 156 | 0 | 0.0 % | 16 | 263 | 572 |
| Iniciar atención | 156 | 0 | 0.0 % | 21 | 73 | 153 |
| Leer la consulta | 156 | 0 | 0.0 % | 15 | 49 | 103 |
| Registrar diagnóstico | 156 | 0 | 0.0 % | 13 | 45 | 141 |
| Registrar receta | 156 | 0 | 0.0 % | 14 | 48 | 147 |
| Actualizar mascota | 156 | 0 | 0.0 % | 14 | 42 | 69 |
| Cerrar la consulta | 156 | 0 | 0.0 % | 26 | 76 | 103 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 14 | 221 | 398 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 14 | 48 | 179 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 15 | 51 | 80 |
| Cobrar | 95 | 0 | 0.0 % | 18 | 71 | 144 |
| Cancelar cita | 53 | 0 | 0.0 % | 21 | 76 | 238 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 528 | 1 784 | 2 143 | 19 % |
| reporte_clinico | 22 | 0 (0 %) | 2 497 | 3 552 | 10 253 | 17 % |
| panel | 389 | 0 (0 %) | 97 | 229 | 375 | 12 % |
| apoderado | 312 | 0 (0 %) | 39 | 194 | 683 | 6 % |
| propietarios | 237 | 0 (0 %) | 51 | 177 | 235 | 5 % |
| cita | 312 | 7 (2 %) | 35 | 126 | 419 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 42 | 126 | 323 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 31 | 123 | 432 | 3 % |
| mascota | 312 | 0 (0 %) | 25 | 72 | 205 | 3 % |
| navegacion | 389 | 0 (0 %) | 12 | 87 | 374 | 3 % |
| cumplimiento | 389 | 0 (0 %) | 13 | 42 | 321 | 2 % |
| historia | 232 | 0 (0 %) | 23 | 65 | 202 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 426 | 473 | 0 % |
| 0.1 | 21 | 111 | 1 410 | 1 980 | 2 % |
| 0.4 | 35 | 167 | 627 | 2 084 | 0 % |
| 0.7 | 48 | 254 | 354 | 726 | 0 % |
| 0.9 | 62 | 270 | 444 | 1 008 | 0 % |
| 1.1 | 76 | 345 | 407 | 879 | 0 % |
| 1.4 | 89 | 357 | 222 | 799 | 0 % |
| 1.6 | 100 | 447 | 127 | 553 | 0 % |
| 1.9 | 100 | 416 | 103 | 480 | 0 % |
| 2.1 | 100 | 436 | 60 | 85 | 0 % |
| 2.4 | 100 | 414 | 211 | 789 | 0 % |
| 2.6 | 100 | 401 | 186 | 374 | 0 % |
| 2.9 | 100 | 412 | 90 | 197 | 0 % |
| 3.1 | 100 | 358 | 148 | 698 | 1 % |
| 3.4 | 100 | 380 | 62 | 102 | 0 % |
| 3.6 | 100 | 405 | 66 | 394 | 0 % |
| 3.9 | 100 | 416 | 61 | 100 | 0 % |
| 4.2 | 100 | 396 | 125 | 683 | 0 % |
| 4.4 | 98 | 207 | 322 | 1 534 | 0 % |
| 4.7 | 63 | 51 | 70 | 75 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
