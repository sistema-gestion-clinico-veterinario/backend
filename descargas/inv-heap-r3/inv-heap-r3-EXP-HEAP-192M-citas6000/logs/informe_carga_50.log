# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 50 |
| Peticiones hechas | 2 776 |
| Peticiones correctas (2xx) | 2 773 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 65 |
| Tiempo p99 (ms) | 358 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 21 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 420 |
| Sentencias SQL | 74 184 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 776 | 11.2 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 2 | 0 % | 87 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1023; fallidas: 3.
- Acciones hechas: panel 133, agenda 100, historia 100, reportes 8, recepcion 145, atencion 62, cobro 35, cancelar 29; sin trabajo disponible: 106.
- CPU del backend: 21 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 13 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 420 MB (82 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 145 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 343 pausas, 12.3 s en total (5 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 184 MB (tope -Xmx192m).
- Clases cargadas: 25254 en total, 485 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1748 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4588 |
| 0.1 | 32 | 32 | 8045 |
| 0.1 | 60 | 61 | 13631 |
| 0.1 | 74 | 74 | 16075 |
| 0.2 | 74 | 75 | 16106 |
| 0.2 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17215 |
| 0.4 | 102 | 104 | 21640 |
| 1.2 | 111 | 114 | 24460 |
| 2.0 | 110 | 114 | 24768 |
| 2.9 | 111 | 114 | 25026 |
| 3.9 | 111 | 114 | 25253 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 461 |
| `java.lang.invoke.LambdaForm$DMH/` | 15 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 476 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.springframework.web.servlet` | 2 |
| `org.hibernate.event.spi` | 1 |
| `org.springframework.web.method` | 1 |
| `org.springframework.core.ExceptionDepthComparator` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.2 | 6.6 | 15 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 1.1 | 5.96 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 0.9 | 57.67 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 0.7 | 6.72 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.06 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 100 | 0.6 | 5.62 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.2 | 0.86 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.2 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 0.2 | 10.88 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 100 | 0.2 | 1.59 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 145 | 145 | coincide |
| Mascotas | 145 | 145 | coincide |
| Citas agendadas | 142 | 142 | coincide |
| Diagnósticos | 62 | 62 | coincide |
| Recetas | 62 | 62 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 62 | 62 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 145 | 0 | 0.0 % | 22 | 65 | 223 |
| Registrar mascota | 145 | 0 | 0.0 % | 14 | 39 | 247 |
| Agendar cita | 142 | 3 | 2.1 % | 17 | 49 | 269 |
| Recepción (llegada) | 62 | 0 | 0.0 % | 11 | 38 | 266 |
| Iniciar atención | 62 | 0 | 0.0 % | 13 | 42 | 80 |
| Leer la consulta | 62 | 0 | 0.0 % | 7 | 31 | 44 |
| Registrar diagnóstico | 62 | 0 | 0.0 % | 7 | 33 | 36 |
| Registrar receta | 62 | 0 | 0.0 % | 8 | 33 | 340 |
| Actualizar mascota | 62 | 0 | 0.0 % | 9 | 27 | 332 |
| Cerrar la consulta | 62 | 0 | 0.0 % | 14 | 51 | 351 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 8 | 23 | 34 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 7 | 21 | 33 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 8 | 22 | 25 |
| Cobrar | 35 | 0 | 0.0 % | 11 | 25 | 33 |
| Cancelar cita | 29 | 0 | 0.0 % | 14 | 199 | 256 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 347 | 1 271 | 1 670 | 24 % |
| panel | 183 | 0 (0 %) | 39 | 73 | 349 | 11 % |
| reporte_clinico | 8 | 0 (0 %) | 1 130 | 2 369 | 2 369 | 11 % |
| apoderado | 145 | 0 (0 %) | 22 | 65 | 223 | 5 % |
| propietarios | 100 | 0 (0 %) | 22 | 136 | 349 | 5 % |
| cita | 145 | 3 (2 %) | 17 | 49 | 269 | 5 % |
| lista_de_historias | 100 | 0 (0 %) | 19 | 214 | 365 | 4 % |
| mascota | 145 | 0 (0 %) | 14 | 39 | 247 | 4 % |
| citas_del_mes | 100 | 0 (0 %) | 23 | 68 | 336 | 4 % |
| navegacion | 183 | 0 (0 %) | 8 | 29 | 227 | 3 % |
| historia | 100 | 0 (0 %) | 13 | 41 | 99 | 2 % |
| cerrar_consulta | 62 | 0 (0 %) | 14 | 51 | 351 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 84 | 342 | 364 | 0 % |
| 0.2 | 26 | 150 | 447 | 1 670 | 0 % |
| 0.5 | 39 | 198 | 329 | 419 | 0 % |
| 0.7 | 50 | 217 | 320 | 361 | 0 % |
| 1.0 | 50 | 184 | 44 | 351 | 0 % |
| 1.2 | 50 | 182 | 247 | 1 106 | 0 % |
| 1.5 | 50 | 201 | 36 | 49 | 0 % |
| 1.7 | 50 | 217 | 41 | 51 | 0 % |
| 2.0 | 50 | 200 | 48 | 340 | 0 % |
| 2.2 | 50 | 183 | 36 | 257 | 0 % |
| 2.5 | 50 | 216 | 216 | 275 | 0 % |
| 2.7 | 50 | 200 | 37 | 49 | 0 % |
| 3.0 | 50 | 199 | 124 | 327 | 0 % |
| 3.2 | 50 | 198 | 37 | 43 | 0 % |
| 3.5 | 47 | 111 | 38 | 39 | 0 % |
| 3.7 | 31 | 36 | 30 | 37 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
