# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 218 |
| Peticiones correctas (2xx) | 6 211 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.4 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 252 |
| Tiempo p99 (ms) | 444 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 47 |
| Tiempo con la CPU al tope (%) | 9 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 10 |
| Memoria pico (MB, de 512) | 427 |
| Sentencias SQL | 167 237 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 218 | 20.4 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 5 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2356; fallidas: 7.
- Acciones hechas: panel 289, agenda 237, historia 231, reportes 22, recepcion 312, atencion 156, cobro 92, cancelar 56; sin trabajo disponible: 253.
- CPU del backend: 47 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 25 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 427 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 312 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.6 %).
- GC: 526 pausas, 36.3 s en total (11 % del tiempo de vida de la JVM); 156 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25420 en total, 180 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8291 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 85 | 86 | 18044 |
| 1.2 | 111 | 114 | 24459 |
| 2.0 | 110 | 114 | 24816 |
| 2.5 | 111 | 114 | 25034 |
| 3.1 | 111 | 114 | 25261 |
| 3.7 | 111 | 114 | 25328 |
| 4.4 | 111 | 114 | 25353 |
| 5.0 | 111 | 114 | 25419 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 176 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 180 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 389 | 3.4 | 8.86 | 16 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 3.2 | 72.87 | 15 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 389 | 2.2 | 5.6 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 231 | 1.7 | 7.32 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 1.6 | 2.48 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 237 | 1.3 | 5.62 | 6 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 0.7 | 16.35 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 626 | 0.7 | 1.07 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.5 | 1.32 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 231 | 0.5 | 2.02 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 312 | 312 | coincide |
| Mascotas | 312 | 312 | coincide |
| Citas agendadas | 309 | 309 | coincide |
| Diagnósticos | 152 | 152 | coincide |
| Recetas | 152 | 152 | coincide |
| Pagos | 92 | 92 | coincide |
| Citas completadas | 152 | 152 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 312 | 0 | 0.0 % | 33 | 249 | 373 |
| Registrar mascota | 312 | 0 | 0.0 % | 21 | 113 | 327 |
| Agendar cita | 309 | 3 | 1.0 % | 26 | 118 | 322 |
| Recepción (llegada) | 156 | 0 | 0.0 % | 17 | 219 | 517 |
| Iniciar atención | 152 | 4 | 2.6 % | 21 | 98 | 272 |
| Leer la consulta | 152 | 0 | 0.0 % | 13 | 74 | 330 |
| Registrar diagnóstico | 152 | 0 | 0.0 % | 12 | 57 | 260 |
| Registrar receta | 152 | 0 | 0.0 % | 12 | 45 | 305 |
| Actualizar mascota | 152 | 0 | 0.0 % | 13 | 42 | 321 |
| Cerrar la consulta | 152 | 0 | 0.0 % | 20 | 244 | 498 |
| Caja: crear punto de cobro | 69 | 0 | 0.0 % | 11 | 258 | 321 |
| Caja: vincular equipo | 69 | 0 | 0.0 % | 10 | 48 | 259 |
| Caja: abrir la caja | 69 | 0 | 0.0 % | 12 | 101 | 316 |
| Cobrar | 92 | 0 | 0.0 % | 19 | 101 | 288 |
| Cancelar cita | 56 | 0 | 0.0 % | 22 | 200 | 268 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 386 | 1 212 | 1 738 | 15 % |
| reporte_clinico | 22 | 0 (0 %) | 1 638 | 2 185 | 2 363 | 11 % |
| panel | 389 | 0 (0 %) | 47 | 266 | 417 | 9 % |
| apoderado | 312 | 0 (0 %) | 33 | 249 | 373 | 6 % |
| propietarios | 237 | 0 (0 %) | 47 | 316 | 437 | 6 % |
| citas_del_mes | 237 | 0 (0 %) | 28 | 326 | 396 | 6 % |
| cita | 312 | 3 (1 %) | 26 | 118 | 322 | 4 % |
| lista_de_historias | 231 | 0 (0 %) | 24 | 235 | 341 | 4 % |
| navegacion | 389 | 0 (0 %) | 8 | 193 | 360 | 4 % |
| mascota | 312 | 0 (0 %) | 21 | 113 | 327 | 4 % |
| historia | 231 | 0 (0 %) | 17 | 67 | 332 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 7 | 39 | 297 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 5 | 592 | 592 | 0 % |
| 0.0 | 14 | 95 | 366 | 1 212 | 0 % |
| 0.3 | 28 | 152 | 406 | 1 738 | 0 % |
| 0.5 | 41 | 233 | 342 | 421 | 0 % |
| 0.8 | 55 | 247 | 333 | 401 | 0 % |
| 1.0 | 68 | 286 | 353 | 1 133 | 0 % |
| 1.3 | 82 | 360 | 337 | 925 | 0 % |
| 1.5 | 96 | 417 | 103 | 462 | 0 % |
| 1.8 | 100 | 435 | 192 | 434 | 0 % |
| 2.0 | 100 | 418 | 49 | 136 | 0 % |
| 2.3 | 100 | 416 | 206 | 410 | 0 % |
| 2.5 | 100 | 370 | 254 | 330 | 0 % |
| 2.8 | 100 | 403 | 252 | 439 | 0 % |
| 3.0 | 100 | 413 | 207 | 343 | 0 % |
| 3.3 | 100 | 367 | 268 | 444 | 0 % |
| 3.5 | 100 | 391 | 235 | 306 | 0 % |
| 3.8 | 100 | 377 | 219 | 423 | 0 % |
| 4.0 | 100 | 393 | 332 | 525 | 0 % |
| 4.3 | 100 | 326 | 270 | 416 | 0 % |
| 4.5 | 82 | 114 | 287 | 1 422 | 1 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
