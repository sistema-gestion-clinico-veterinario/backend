# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Usuarios sin degradación | 50 |
| Peticiones hechas | 2 776 |
| Peticiones correctas (2xx) | 2 774 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 92 |
| Tiempo p99 (ms) | 400 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 23 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 74 051 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 776 | 11.2 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 3 | 0 % | 85 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1025; fallidas: 2.
- Acciones hechas: panel 133, agenda 100, historia 100, reportes 8, recepcion 143, atencion 63, cobro 35, cancelar 29; sin trabajo disponible: 107.
- CPU del backend: 23 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 17 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 143 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 343 pausas, 13.0 s en total (5 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25258 en total, 486 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1748 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4592 |
| 0.1 | 32 | 32 | 7992 |
| 0.1 | 60 | 61 | 13639 |
| 0.2 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16106 |
| 0.3 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17215 |
| 0.5 | 103 | 105 | 21956 |
| 1.3 | 111 | 114 | 24482 |
| 2.0 | 110 | 114 | 24772 |
| 3.0 | 111 | 114 | 25040 |
| 4.0 | 111 | 114 | 25258 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 461 |
| `java.lang.invoke.LambdaForm$DMH/` | 16 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 477 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.springframework.web.servlet` | 2 |
| `org.hibernate.event.spi` | 1 |
| `org.springframework.web.method` | 1 |
| `org.springframework.core.ExceptionDepthComparator` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.5 | 8.45 | 18 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 1.0 | 62.24 | 11 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 1.0 | 5.32 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 0.6 | 6.27 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.17 | 7 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 0.5 | 29.01 | 5 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 100 | 0.4 | 4.45 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.3 | 1.0 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.29 | 3 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 100 | 0.2 | 1.88 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 143 | 143 | coincide |
| Mascotas | 143 | 143 | coincide |
| Citas agendadas | 141 | 141 | coincide |
| Diagnósticos | 63 | 63 | coincide |
| Recetas | 63 | 63 | coincide |
| Pagos | 35 | 35 | coincide |
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
| Registrar propietario | 143 | 0 | 0.0 % | 23 | 116 | 239 |
| Registrar mascota | 143 | 0 | 0.0 % | 16 | 42 | 166 |
| Agendar cita | 141 | 2 | 1.4 % | 19 | 59 | 232 |
| Recepción (llegada) | 63 | 0 | 0.0 % | 11 | 51 | 320 |
| Iniciar atención | 63 | 0 | 0.0 % | 14 | 68 | 387 |
| Leer la consulta | 63 | 0 | 0.0 % | 9 | 30 | 52 |
| Registrar diagnóstico | 63 | 0 | 0.0 % | 8 | 45 | 134 |
| Registrar receta | 63 | 0 | 0.0 % | 9 | 41 | 215 |
| Actualizar mascota | 63 | 0 | 0.0 % | 10 | 29 | 51 |
| Cerrar la consulta | 63 | 0 | 0.0 % | 17 | 72 | 328 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 9 | 30 | 39 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 9 | 21 | 128 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 9 | 26 | 32 |
| Cobrar | 35 | 0 | 0.0 % | 16 | 78 | 161 |
| Cancelar cita | 29 | 0 | 0.0 % | 14 | 43 | 79 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 370 | 1 199 | 1 792 | 23 % |
| reporte_clinico | 8 | 0 (0 %) | 1 245 | 2 872 | 2 872 | 11 % |
| panel | 183 | 0 (0 %) | 44 | 79 | 367 | 10 % |
| apoderado | 143 | 0 (0 %) | 23 | 116 | 239 | 5 % |
| propietarios | 100 | 0 (0 %) | 31 | 172 | 536 | 5 % |
| cita | 143 | 2 (1 %) | 19 | 59 | 232 | 5 % |
| citas_del_mes | 100 | 0 (0 %) | 24 | 259 | 304 | 4 % |
| navegacion | 183 | 0 (0 %) | 8 | 40 | 267 | 4 % |
| mascota | 143 | 0 (0 %) | 16 | 42 | 166 | 3 % |
| lista_de_historias | 100 | 0 (0 %) | 19 | 121 | 290 | 3 % |
| cumplimiento | 183 | 0 (0 %) | 7 | 25 | 111 | 2 % |
| empleados | 100 | 0 (0 %) | 10 | 72 | 295 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 3 | 25 | 375 | 386 | 0 % |
| 0.1 | 17 | 103 | 573 | 1 792 | 0 % |
| 0.3 | 30 | 172 | 365 | 446 | 0 % |
| 0.6 | 44 | 216 | 356 | 429 | 0 % |
| 0.8 | 50 | 208 | 197 | 462 | 0 % |
| 1.1 | 50 | 179 | 128 | 536 | 0 % |
| 1.3 | 50 | 186 | 236 | 328 | 0 % |
| 1.6 | 50 | 222 | 45 | 62 | 0 % |
| 1.8 | 50 | 176 | 51 | 307 | 0 % |
| 2.1 | 50 | 238 | 57 | 116 | 0 % |
| 2.3 | 50 | 205 | 53 | 311 | 0 % |
| 2.6 | 50 | 184 | 44 | 234 | 0 % |
| 2.8 | 50 | 205 | 116 | 264 | 0 % |
| 3.1 | 50 | 188 | 54 | 273 | 1 % |
| 3.3 | 50 | 204 | 43 | 73 | 0 % |
| 3.6 | 47 | 59 | 31 | 39 | 0 % |
| 3.8 | 20 | 6 | 29 | 29 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
