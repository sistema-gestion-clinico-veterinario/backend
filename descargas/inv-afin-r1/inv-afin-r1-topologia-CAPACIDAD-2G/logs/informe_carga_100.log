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
| Usuarios sin degradación | 4 |
| Peticiones hechas | 6 290 |
| Peticiones correctas (2xx) | 6 279 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 20 |
| Tiempo p95 (ms) | 131 |
| Tiempo p99 (ms) | 694 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 57 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 20 |
| Memoria pico (MB, de 2048) | 913 |
| Sentencias SQL | 169 858 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 290 | 20.5 | no aplica (ramping-vus) | 0.2 % | 0 | 2 / 5 / 6 | 0 % | 132 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2375; fallidas: 11.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 314, atencion 155, cobro 93, cancelar 53; sin trabajo disponible: 256.
- CPU del backend: 57 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 20 % de media y 59 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 913 MB (45 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **4 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 314 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 281 pausas, 19.5 s en total (6 % del tiempo de vida de la JVM); 25 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 474 MB (tope -Xmx1g).
- Clases cargadas: 25425 en total, 286 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 21 | 22 | 5295 |
| 0.2 | 37 | 38 | 8326 |
| 0.2 | 65 | 66 | 13635 |
| 0.3 | 80 | 81 | 16072 |
| 0.4 | 80 | 81 | 16111 |
| 0.5 | 86 | 87 | 17110 |
| 0.6 | 89 | 90 | 17420 |
| 1.2 | 125 | 127 | 24016 |
| 2.2 | 131 | 133 | 24807 |
| 2.8 | 132 | 135 | 25054 |
| 3.4 | 133 | 136 | 25266 |
| 4.0 | 134 | 137 | 25349 |
| 5.1 | 135 | 138 | 25424 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 275 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 285 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 12.3 | 279.58 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 10.0 | 25.69 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 6.8 | 17.37 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 5.4 | 23.2 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 3.9 | 88.39 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.5 | 3.97 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 1.0 | 1.64 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 1.0 | 1.61 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.8 | 2.01 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 231 | 0.8 | 3.27 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 314 | 314 | coincide |
| Mascotas | 314 | 314 | coincide |
| Citas agendadas | 303 | 303 | coincide |
| Diagnósticos | 155 | 155 | coincide |
| Recetas | 155 | 155 | coincide |
| Pagos | 93 | 93 | coincide |
| Citas completadas | 155 | 155 | coincide |
| Citas canceladas | 53 | 53 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 314 | 0 | 0.0 % | 39 | 181 | 498 |
| Registrar mascota | 314 | 0 | 0.0 % | 27 | 67 | 106 |
| Agendar cita | 303 | 11 | 3.5 % | 35 | 89 | 162 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 16 | 203 | 499 |
| Iniciar atención | 155 | 0 | 0.0 % | 21 | 79 | 160 |
| Leer la consulta | 155 | 0 | 0.0 % | 12 | 45 | 56 |
| Registrar diagnóstico | 155 | 0 | 0.0 % | 14 | 46 | 121 |
| Registrar receta | 155 | 0 | 0.0 % | 14 | 51 | 169 |
| Actualizar mascota | 155 | 0 | 0.0 % | 15 | 47 | 75 |
| Cerrar la consulta | 155 | 0 | 0.0 % | 25 | 69 | 152 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 14 | 100 | 502 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 13 | 38 | 53 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 16 | 50 | 129 |
| Cobrar | 93 | 0 | 0.0 % | 18 | 66 | 667 |
| Cancelar cita | 53 | 0 | 0.0 % | 24 | 57 | 569 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 586 | 1 733 | 2 126 | 20 % |
| reporte_clinico | 22 | 0 (0 %) | 2 440 | 3 898 | 8 567 | 18 % |
| panel | 389 | 0 (0 %) | 92 | 230 | 313 | 12 % |
| apoderado | 314 | 0 (0 %) | 39 | 181 | 498 | 5 % |
| propietarios | 237 | 0 (0 %) | 50 | 138 | 200 | 4 % |
| cita | 314 | 11 (4 %) | 35 | 89 | 162 | 4 % |
| citas_del_mes | 237 | 0 (0 %) | 29 | 177 | 576 | 4 % |
| lista_de_historias | 231 | 0 (0 %) | 40 | 100 | 205 | 4 % |
| mascota | 314 | 0 (0 %) | 27 | 67 | 106 | 3 % |
| navegacion | 389 | 0 (0 %) | 12 | 52 | 354 | 3 % |
| historia | 231 | 0 (0 %) | 23 | 74 | 313 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 13 | 39 | 131 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 28 | 439 | 460 | 0 % |
| 0.1 | 18 | 96 | 669 | 1 628 | 1 % |
| 0.3 | 31 | 164 | 736 | 2 126 | 1 % |
| 0.6 | 45 | 234 | 402 | 786 | 0 % |
| 0.8 | 58 | 255 | 363 | 1 028 | 0 % |
| 1.1 | 72 | 309 | 365 | 872 | 0 % |
| 1.3 | 86 | 387 | 170 | 1 135 | 0 % |
| 1.6 | 99 | 429 | 110 | 653 | 0 % |
| 1.8 | 100 | 415 | 130 | 651 | 0 % |
| 2.1 | 100 | 450 | 70 | 98 | 0 % |
| 2.3 | 100 | 393 | 181 | 822 | 0 % |
| 2.6 | 100 | 403 | 96 | 296 | 0 % |
| 2.8 | 100 | 408 | 84 | 382 | 0 % |
| 3.1 | 100 | 381 | 108 | 428 | 1 % |
| 3.3 | 100 | 384 | 73 | 148 | 0 % |
| 3.6 | 100 | 399 | 56 | 85 | 0 % |
| 3.8 | 100 | 383 | 92 | 410 | 0 % |
| 4.1 | 100 | 425 | 70 | 211 | 0 % |
| 4.3 | 100 | 284 | 264 | 1 715 | 0 % |
| 4.6 | 71 | 63 | 70 | 1 710 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
