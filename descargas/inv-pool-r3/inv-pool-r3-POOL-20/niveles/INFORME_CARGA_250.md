# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 250 |
| Peticiones hechas | 19 977 |
| Peticiones correctas (2xx) | 19 953 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 22 |
| Tiempo p95 (ms) | 228 |
| Tiempo p99 (ms) | 783 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 36 |
| Memoria pico (MB, de 2048) | 1 282 |
| Sentencias SQL | 541 768 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 977 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 13 | 0 % | 283 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7268; fallidas: 24.
- Acciones hechas: panel 1002, agenda 817, historia 730, reportes 94, recepcion 983, atencion 474, cobro 319, cancelar 200; sin trabajo disponible: 734.
- CPU del backend: 58 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 36 % de media y 94 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 282 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.8 s.
- Correo: el servidor de pruebas recibió 983 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 505 pausas, 47.1 s en total (10 % del tiempo de vida de la JVM); 53 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 933 MB (tope -Xmx1g).
- Clases cargadas: 25561 en total, 167 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 20 | 20 | 4870 |
| 0.1 | 56 | 56 | 11916 |
| 0.2 | 80 | 81 | 16107 |
| 0.3 | 86 | 87 | 17107 |
| 1.0 | 124 | 126 | 23821 |
| 2.4 | 132 | 135 | 25002 |
| 3.3 | 134 | 137 | 25347 |
| 3.8 | 134 | 137 | 25398 |
| 4.5 | 135 | 138 | 25473 |
| 5.0 | 135 | 138 | 25473 |
| 5.5 | 136 | 139 | 25514 |
| 6.2 | 136 | 139 | 25558 |
| 6.8 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 155 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 162 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 65.2 | 347.0 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1252 | 42.3 | 33.75 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1252 | 38.4 | 30.66 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 730 | 25.2 | 34.53 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 23.4 | 124.3 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 817 | 18.0 | 22.02 | 6 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1252 | 15.2 | 12.17 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2069 | 12.4 | 6.0 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.6 | 38.58 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2069 | 3.2 | 1.56 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 983 | 983 | coincide |
| Mascotas | 983 | 983 | coincide |
| Citas agendadas | 970 | 970 | coincide |
| Diagnósticos | 463 | 463 | coincide |
| Recetas | 463 | 463 | coincide |
| Pagos | 319 | 319 | coincide |
| Citas completadas | 463 | 463 | coincide |
| Citas canceladas | 200 | 200 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 983 | 0 | 0.0 % | 41 | 317 | 696 |
| Registrar mascota | 983 | 0 | 0.0 % | 24 | 76 | 191 |
| Agendar cita | 970 | 13 | 1.3 % | 33 | 115 | 253 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 20 | 205 | 562 |
| Iniciar atención | 463 | 11 | 2.3 % | 28 | 119 | 233 |
| Leer la consulta | 463 | 0 | 0.0 % | 16 | 51 | 154 |
| Registrar diagnóstico | 463 | 0 | 0.0 % | 16 | 54 | 137 |
| Registrar receta | 463 | 0 | 0.0 % | 18 | 54 | 139 |
| Actualizar mascota | 463 | 0 | 0.0 % | 18 | 48 | 159 |
| Cerrar la consulta | 463 | 0 | 0.0 % | 30 | 94 | 221 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 16 | 148 | 531 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 14 | 51 | 177 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 17 | 57 | 128 |
| Cobrar | 319 | 0 | 0.0 % | 24 | 92 | 412 |
| Cancelar cita | 200 | 0 | 0.0 % | 24 | 189 | 736 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 680 | 6 000 | 7 521 | 21 % |
| panel | 1252 | 0 (0 %) | 87 | 491 | 978 | 14 % |
| login | 250 | 0 (0 %) | 361 | 1 677 | 2 673 | 10 % |
| propietarios | 817 | 0 (0 %) | 79 | 325 | 1 068 | 7 % |
| apoderado | 983 | 0 (0 %) | 41 | 317 | 696 | 6 % |
| citas_del_mes | 817 | 0 (0 %) | 42 | 369 | 762 | 5 % |
| lista_de_historias | 730 | 0 (0 %) | 43 | 396 | 783 | 5 % |
| cita | 983 | 13 (1 %) | 33 | 115 | 253 | 3 % |
| navegacion | 1252 | 0 (0 %) | 12 | 138 | 505 | 3 % |
| privacidad | 1252 | 0 (0 %) | 15 | 131 | 231 | 3 % |
| mascota | 983 | 0 (0 %) | 24 | 76 | 191 | 2 % |
| historia | 730 | 0 (0 %) | 26 | 93 | 179 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 84 | 328 | 358 | 0 % |
| 0.5 | 41 | 201 | 331 | 480 | 0 % |
| 0.9 | 71 | 316 | 262 | 401 | 0 % |
| 1.4 | 101 | 442 | 73 | 345 | 0 % |
| 1.9 | 131 | 601 | 82 | 364 | 0 % |
| 2.5 | 161 | 612 | 186 | 1 459 | 0 % |
| 3.0 | 191 | 787 | 183 | 1 084 | 0 % |
| 3.5 | 221 | 926 | 141 | 542 | 0 % |
| 4.0 | 250 | 1027 | 257 | 912 | 0 % |
| 4.5 | 250 | 950 | 199 | 773 | 0 % |
| 5.0 | 250 | 990 | 270 | 860 | 0 % |
| 5.5 | 250 | 956 | 317 | 865 | 0 % |
| 6.0 | 250 | 933 | 227 | 837 | 0 % |
| 6.5 | 250 | 912 | 266 | 765 | 0 % |
| 7.0 | 178 | 188 | 92 | 1 009 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
