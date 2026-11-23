# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 1 |
| Peticiones hechas | 6 289 |
| Peticiones correctas (2xx) | 6 277 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 19 |
| Tiempo p95 (ms) | 124 |
| Tiempo p99 (ms) | 632 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 53 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 17 |
| Memoria pico (MB, de 2048) | 848 |
| Sentencias SQL | 169 803 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 289 | 20.5 | no aplica (ramping-vus) | 0.2 % | 0 | 2 / 5 / 6 | 0 % | 136 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2370; fallidas: 12.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 314, atencion 155, cobro 93, cancelar 54; sin trabajo disponible: 256.
- CPU del backend: 53 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 17 % de media y 52 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 848 MB (41 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **1 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.5 s.
- Correo: el servidor de pruebas recibió 314 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 292 pausas, 17.8 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 394 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 334 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 23 | 23 | 5565 |
| 0.2 | 43 | 43 | 9656 |
| 0.3 | 66 | 67 | 13760 |
| 0.3 | 80 | 81 | 16081 |
| 0.4 | 80 | 81 | 16120 |
| 0.5 | 86 | 87 | 17109 |
| 0.6 | 93 | 94 | 18040 |
| 1.5 | 128 | 130 | 24473 |
| 2.3 | 131 | 134 | 24910 |
| 3.0 | 133 | 135 | 25148 |
| 3.7 | 133 | 136 | 25301 |
| 4.3 | 134 | 137 | 25352 |
| 5.3 | 135 | 138 | 25425 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 324 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 334 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 11.2 | 255.33 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 8.8 | 22.72 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 6.5 | 16.63 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 4.3 | 18.39 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 2.8 | 64.73 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.1 | 3.34 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 0.8 | 1.29 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 626 | 0.8 | 1.24 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.6 | 1.64 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 22 | 0.5 | 21.62 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 314 | 314 | coincide |
| Mascotas | 314 | 314 | coincide |
| Citas agendadas | 303 | 303 | coincide |
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
| Registrar propietario | 314 | 0 | 0.0 % | 36 | 149 | 309 |
| Registrar mascota | 314 | 0 | 0.0 % | 25 | 76 | 222 |
| Agendar cita | 303 | 11 | 3.5 % | 32 | 104 | 236 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 14 | 55 | 189 |
| Iniciar atención | 154 | 1 | 0.6 % | 19 | 58 | 112 |
| Leer la consulta | 154 | 0 | 0.0 % | 11 | 42 | 54 |
| Registrar diagnóstico | 154 | 0 | 0.0 % | 12 | 42 | 82 |
| Registrar receta | 154 | 0 | 0.0 % | 13 | 55 | 408 |
| Actualizar mascota | 154 | 0 | 0.0 % | 14 | 50 | 153 |
| Cerrar la consulta | 154 | 0 | 0.0 % | 26 | 85 | 131 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 12 | 110 | 388 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 12 | 41 | 121 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 13 | 44 | 69 |
| Cobrar | 93 | 0 | 0.0 % | 18 | 58 | 194 |
| Cancelar cita | 54 | 0 | 0.0 % | 25 | 84 | 421 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 569 | 1 718 | 2 033 | 22 % |
| reporte_clinico | 22 | 0 (0 %) | 1 995 | 3 371 | 6 668 | 16 % |
| panel | 389 | 0 (0 %) | 86 | 198 | 285 | 12 % |
| apoderado | 314 | 0 (0 %) | 36 | 149 | 309 | 5 % |
| cita | 314 | 11 (4 %) | 32 | 104 | 236 | 4 % |
| propietarios | 237 | 0 (0 %) | 39 | 128 | 435 | 4 % |
| mascota | 314 | 0 (0 %) | 25 | 76 | 222 | 3 % |
| lista_de_historias | 232 | 0 (0 %) | 35 | 91 | 180 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 23 | 87 | 503 | 3 % |
| navegacion | 389 | 0 (0 %) | 13 | 59 | 253 | 3 % |
| historia | 232 | 0 (0 %) | 22 | 68 | 109 | 2 % |
| avisos_de_cumplimiento | 389 | 0 (0 %) | 12 | 38 | 82 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 7 | 459 | 459 | 0 % |
| 0.0 | 15 | 97 | 435 | 682 | 1 % |
| 0.3 | 28 | 145 | 1 529 | 2 033 | 1 % |
| 0.5 | 42 | 236 | 443 | 840 | 0 % |
| 0.8 | 56 | 250 | 347 | 701 | 0 % |
| 1.0 | 69 | 289 | 436 | 1 237 | 0 % |
| 1.3 | 83 | 359 | 236 | 986 | 0 % |
| 1.5 | 97 | 437 | 106 | 591 | 0 % |
| 1.8 | 100 | 416 | 104 | 619 | 0 % |
| 2.0 | 100 | 417 | 71 | 324 | 0 % |
| 2.3 | 100 | 409 | 92 | 212 | 0 % |
| 2.5 | 100 | 395 | 130 | 253 | 0 % |
| 2.8 | 100 | 414 | 82 | 232 | 0 % |
| 3.0 | 100 | 419 | 122 | 388 | 0 % |
| 3.3 | 100 | 375 | 64 | 104 | 0 % |
| 3.5 | 100 | 399 | 53 | 86 | 0 % |
| 3.8 | 100 | 394 | 64 | 128 | 0 % |
| 4.0 | 100 | 420 | 72 | 182 | 0 % |
| 4.3 | 100 | 309 | 111 | 286 | 0 % |
| 4.5 | 80 | 99 | 107 | 1 984 | 0 % |
| 4.8 | 34 | 3 | 17 | 17 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
