# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 2 791 |
| Peticiones correctas (2xx) | 2 790 |
| Errores (%) | 0.0 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 55 |
| Tiempo p99 (ms) | 306 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 21 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 4 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 74 493 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 791 | 11.2 | no aplica (ramping-vus) | 0.0 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1039; fallidas: 1.
- Acciones hechas: panel 133, agenda 100, historia 101, reportes 8, recepcion 145, atencion 64, cobro 35, cancelar 29; sin trabajo disponible: 108.
- CPU del backend: 21 % de media y 74 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 4 % de media y 11 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.3 s.
- Correo: el servidor de pruebas recibió 145 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 349 pausas, 13.3 s en total (5 % del tiempo de vida de la JVM); 56 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25269 en total, 467 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1748 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4592 |
| 0.1 | 32 | 32 | 7992 |
| 0.1 | 60 | 61 | 13639 |
| 0.1 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17215 |
| 0.5 | 105 | 107 | 22473 |
| 1.3 | 111 | 114 | 24482 |
| 2.0 | 110 | 114 | 24783 |
| 3.0 | 111 | 114 | 25098 |
| 4.0 | 111 | 114 | 25251 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 446 |
| `java.lang.invoke.LambdaForm$DMH/` | 12 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 458 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.springframework.web.servlet` | 2 |
| `org.hibernate.event.spi` | 1 |
| `org.springframework.web.method` | 1 |
| `org.springframework.core.ExceptionDepthComparator` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.2 | 6.43 | 18 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 0.9 | 55.16 | 13 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 0.7 | 4.07 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.5 | 1.66 | 7 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 101 | 0.5 | 4.6 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 100 | 0.4 | 3.92 | 6 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.2 | 0.77 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 0.93 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 0.1 | 9.36 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 101 | 0.1 | 1.25 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 145 | 145 | coincide |
| Mascotas | 145 | 145 | coincide |
| Citas agendadas | 144 | 144 | coincide |
| Diagnósticos | 64 | 64 | coincide |
| Recetas | 64 | 64 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 64 | 64 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 145 | 0 | 0.0 % | 20 | 76 | 270 |
| Registrar mascota | 145 | 0 | 0.0 % | 14 | 35 | 175 |
| Agendar cita | 144 | 1 | 0.7 % | 16 | 46 | 219 |
| Recepción (llegada) | 64 | 0 | 0.0 % | 11 | 28 | 188 |
| Iniciar atención | 64 | 0 | 0.0 % | 12 | 34 | 41 |
| Leer la consulta | 64 | 0 | 0.0 % | 7 | 21 | 30 |
| Registrar diagnóstico | 64 | 0 | 0.0 % | 7 | 18 | 30 |
| Registrar receta | 64 | 0 | 0.0 % | 8 | 24 | 32 |
| Actualizar mascota | 64 | 0 | 0.0 % | 9 | 27 | 216 |
| Cerrar la consulta | 64 | 0 | 0.0 % | 14 | 45 | 373 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 8 | 20 | 192 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 7 | 19 | 20 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 7 | 28 | 30 |
| Cobrar | 35 | 0 | 0.0 % | 9 | 25 | 30 |
| Cancelar cita | 29 | 0 | 0.0 % | 13 | 42 | 98 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 279 | 895 | 1 480 | 22 % |
| panel | 183 | 0 (0 %) | 35 | 68 | 286 | 11 % |
| reporte_clinico | 8 | 0 (0 %) | 844 | 2 245 | 2 245 | 11 % |
| apoderado | 145 | 0 (0 %) | 20 | 76 | 270 | 6 % |
| cita | 145 | 1 (1 %) | 16 | 46 | 219 | 4 % |
| propietarios | 100 | 0 (0 %) | 21 | 73 | 306 | 4 % |
| citas_del_mes | 100 | 0 (0 %) | 19 | 117 | 309 | 4 % |
| mascota | 145 | 0 (0 %) | 14 | 35 | 175 | 4 % |
| lista_de_historias | 101 | 0 (0 %) | 16 | 68 | 229 | 4 % |
| navegacion | 183 | 0 (0 %) | 7 | 41 | 192 | 4 % |
| avisos_de_cumplimiento | 183 | 0 (0 %) | 5 | 18 | 215 | 2 % |
| notificaciones | 183 | 0 (0 %) | 5 | 17 | 198 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 283 | 283 | 0 % |
| 0.1 | 16 | 92 | 303 | 1 480 | 0 % |
| 0.3 | 29 | 168 | 326 | 953 | 0 % |
| 0.6 | 43 | 232 | 261 | 306 | 0 % |
| 0.8 | 50 | 206 | 250 | 360 | 0 % |
| 1.1 | 50 | 166 | 52 | 240 | 0 % |
| 1.3 | 50 | 193 | 47 | 281 | 0 % |
| 1.6 | 50 | 222 | 44 | 147 | 0 % |
| 1.8 | 50 | 182 | 30 | 44 | 0 % |
| 2.0 | 50 | 213 | 41 | 206 | 0 % |
| 2.3 | 50 | 225 | 40 | 216 | 0 % |
| 2.5 | 50 | 187 | 41 | 294 | 0 % |
| 2.8 | 50 | 192 | 32 | 211 | 0 % |
| 3.0 | 50 | 195 | 32 | 198 | 1 % |
| 3.3 | 50 | 203 | 30 | 36 | 0 % |
| 3.5 | 48 | 93 | 22 | 29 | 0 % |
| 3.8 | 24 | 8 | 18 | 18 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
