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
| Usuarios sin degradación | 2 |
| Peticiones hechas | 2 786 |
| Peticiones correctas (2xx) | 2 782 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 59 |
| Tiempo p99 (ms) | 364 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 22 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 422 |
| Sentencias SQL | 74 457 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 786 | 11.2 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 2 | 0 % | 87 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1030; fallidas: 4.
- Acciones hechas: panel 133, agenda 100, historia 101, reportes 8, recepcion 147, atencion 63, cobro 33, cancelar 29; sin trabajo disponible: 110.
- CPU del backend: 22 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 18 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 422 MB (82 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **2 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 147 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 338 pausas, 11.9 s en total (5 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25279 en total, 476 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1744 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4594 |
| 0.1 | 32 | 32 | 7993 |
| 0.1 | 60 | 61 | 13638 |
| 0.1 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16111 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17215 |
| 0.5 | 105 | 107 | 22505 |
| 1.3 | 111 | 114 | 24489 |
| 2.0 | 110 | 114 | 24816 |
| 3.0 | 111 | 114 | 25139 |
| 4.0 | 111 | 114 | 25279 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 459 |
| `java.lang.invoke.LambdaForm$DMH/` | 15 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 474 |
| `org.springframework.jdbc.core` | 1 |
| `com.zaxxer.hikari.pool` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.2 | 6.73 | 16 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 1.1 | 6.13 | 14 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 0.7 | 45.64 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 101 | 0.6 | 6.14 | 8 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 100 | 0.6 | 5.83 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.06 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.2 | 0.88 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.19 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 0.2 | 10.14 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 101 | 0.2 | 1.51 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 147 | 147 | coincide |
| Mascotas | 147 | 147 | coincide |
| Citas agendadas | 143 | 143 | coincide |
| Diagnósticos | 63 | 63 | coincide |
| Recetas | 63 | 63 | coincide |
| Pagos | 33 | 33 | coincide |
| Citas completadas | 63 | 63 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 147 | 0 | 0.0 % | 22 | 81 | 269 |
| Registrar mascota | 147 | 0 | 0.0 % | 15 | 41 | 140 |
| Agendar cita | 143 | 4 | 2.7 % | 18 | 52 | 254 |
| Recepción (llegada) | 63 | 0 | 0.0 % | 11 | 22 | 247 |
| Iniciar atención | 63 | 0 | 0.0 % | 12 | 23 | 223 |
| Leer la consulta | 63 | 0 | 0.0 % | 7 | 19 | 31 |
| Registrar diagnóstico | 63 | 0 | 0.0 % | 7 | 26 | 29 |
| Registrar receta | 63 | 0 | 0.0 % | 8 | 24 | 36 |
| Actualizar mascota | 63 | 0 | 0.0 % | 8 | 21 | 359 |
| Cerrar la consulta | 63 | 0 | 0.0 % | 14 | 39 | 87 |
| Caja: crear punto de cobro | 30 | 0 | 0.0 % | 9 | 31 | 202 |
| Caja: vincular equipo | 30 | 0 | 0.0 % | 7 | 25 | 27 |
| Caja: abrir la caja | 30 | 0 | 0.0 % | 8 | 42 | 231 |
| Cobrar | 33 | 0 | 0.0 % | 11 | 33 | 58 |
| Cancelar cita | 29 | 0 | 0.0 % | 14 | 198 | 264 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 347 | 1 300 | 1 609 | 25 % |
| reporte_clinico | 8 | 0 (0 %) | 1 105 | 2 173 | 2 173 | 11 % |
| panel | 183 | 0 (0 %) | 40 | 80 | 309 | 11 % |
| apoderado | 147 | 0 (0 %) | 22 | 81 | 269 | 6 % |
| cita | 147 | 4 (3 %) | 18 | 52 | 254 | 5 % |
| citas_del_mes | 100 | 0 (0 %) | 25 | 61 | 245 | 4 % |
| propietarios | 100 | 0 (0 %) | 23 | 70 | 326 | 4 % |
| mascota | 147 | 0 (0 %) | 15 | 41 | 140 | 4 % |
| lista_de_historias | 101 | 0 (0 %) | 18 | 41 | 288 | 3 % |
| navegacion | 183 | 0 (0 %) | 8 | 22 | 223 | 3 % |
| historia | 101 | 0 (0 %) | 12 | 41 | 271 | 2 % |
| avisos_de_cumplimiento | 183 | 0 (0 %) | 6 | 18 | 36 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 21 | 352 | 373 | 0 % |
| 0.1 | 16 | 93 | 401 | 2 173 | 1 % |
| 0.3 | 29 | 170 | 362 | 576 | 0 % |
| 0.6 | 43 | 225 | 322 | 378 | 0 % |
| 0.8 | 50 | 214 | 241 | 373 | 0 % |
| 1.1 | 50 | 168 | 51 | 288 | 1 % |
| 1.3 | 50 | 186 | 72 | 304 | 0 % |
| 1.6 | 50 | 218 | 37 | 49 | 0 % |
| 1.8 | 50 | 179 | 41 | 271 | 0 % |
| 2.0 | 50 | 228 | 38 | 55 | 0 % |
| 2.3 | 50 | 216 | 48 | 246 | 0 % |
| 2.5 | 50 | 184 | 37 | 70 | 1 % |
| 2.8 | 50 | 196 | 71 | 297 | 0 % |
| 3.0 | 50 | 196 | 36 | 42 | 0 % |
| 3.3 | 50 | 202 | 37 | 46 | 0 % |
| 3.5 | 48 | 82 | 25 | 49 | 1 % |
| 3.8 | 24 | 8 | 23 | 23 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
