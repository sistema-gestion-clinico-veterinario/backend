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
| Peticiones hechas | 19 982 |
| Peticiones correctas (2xx) | 19 897 |
| Errores (%) | 0.4 % |
| Ritmo medio (peticiones/s) | 44.6 |
| Tiempo mediano (ms) | 22 |
| Tiempo p95 (ms) | 263 |
| Tiempo p99 (ms) | 735 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 62 |
| Tiempo con la CPU al tope (%) | 21 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 289 |
| Sentencias SQL | 543 194 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 982 | 44.6 | no aplica (ramping-vus) | 0.4 % | 0 | 3 / 8 / 12 | 0 % | 286 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7212; fallidas: 85.
- Acciones hechas: panel 1002, agenda 817, historia 729, reportes 95, recepcion 990, atencion 471, cobro 322, cancelar 189; sin trabajo disponible: 752.
- CPU del backend: 62 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 21 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 289 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 990 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 512 pausas, 46.5 s en total (10 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 943 MB (tope -Xmx1g).
- Clases cargadas: 25559 en total, 165 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 20 | 20 | 4868 |
| 0.2 | 56 | 57 | 11914 |
| 0.3 | 80 | 81 | 16109 |
| 0.4 | 86 | 87 | 17108 |
| 0.5 | 110 | 112 | 21336 |
| 2.1 | 131 | 134 | 24913 |
| 3.0 | 133 | 136 | 25311 |
| 3.6 | 134 | 137 | 25394 |
| 4.2 | 135 | 138 | 25472 |
| 4.9 | 135 | 138 | 25472 |
| 5.4 | 136 | 139 | 25511 |
| 6.0 | 136 | 139 | 25556 |
| 6.7 | 136 | 139 | 25558 |
| 7.5 | 136 | 139 | 25559 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 153 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 160 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 190 | 64.6 | 339.84 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1252 | 40.6 | 32.44 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1252 | 36.7 | 29.33 | 14 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 1802 | 22.6 | 12.55 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 729 | 20.8 | 28.46 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 172 | 19.0 | 110.18 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1252 | 10.6 | 8.43 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2069 | 9.8 | 4.74 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 95 | 3.5 | 36.41 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2069 | 3.2 | 1.54 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 990 | 990 | coincide |
| Mascotas | 990 | 990 | coincide |
| Citas agendadas | 914 | 914 | coincide |
| Diagnósticos | 462 | 462 | coincide |
| Recetas | 462 | 462 | coincide |
| Pagos | 322 | 322 | coincide |
| Citas completadas | 462 | 462 | coincide |
| Citas canceladas | 189 | 189 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 990 | 0 | 0.0 % | 41 | 295 | 648 |
| Registrar mascota | 990 | 0 | 0.0 % | 24 | 106 | 351 |
| Agendar cita | 914 | 76 | 7.7 % | 32 | 129 | 253 |
| Recepción (llegada) | 471 | 0 | 0.0 % | 19 | 200 | 605 |
| Iniciar atención | 462 | 9 | 1.9 % | 27 | 110 | 232 |
| Leer la consulta | 462 | 0 | 0.0 % | 16 | 88 | 242 |
| Registrar diagnóstico | 462 | 0 | 0.0 % | 16 | 74 | 279 |
| Registrar receta | 462 | 0 | 0.0 % | 17 | 95 | 385 |
| Actualizar mascota | 462 | 0 | 0.0 % | 17 | 63 | 201 |
| Cerrar la consulta | 462 | 0 | 0.0 % | 28 | 121 | 302 |
| Caja: crear punto de cobro | 188 | 0 | 0.0 % | 16 | 330 | 757 |
| Caja: vincular equipo | 188 | 0 | 0.0 % | 15 | 79 | 127 |
| Caja: abrir la caja | 188 | 0 | 0.0 % | 18 | 99 | 593 |
| Cobrar | 322 | 0 | 0.0 % | 23 | 175 | 396 |
| Cancelar cita | 189 | 0 | 0.0 % | 27 | 283 | 775 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 95 | 0 (0 %) | 2 462 | 5 261 | 6 360 | 18 % |
| panel | 1252 | 0 (0 %) | 101 | 454 | 754 | 14 % |
| login | 250 | 0 (0 %) | 387 | 1 489 | 1 969 | 10 % |
| propietarios | 817 | 0 (0 %) | 73 | 298 | 514 | 6 % |
| apoderado | 990 | 0 (0 %) | 41 | 295 | 648 | 6 % |
| lista_de_historias | 729 | 0 (0 %) | 43 | 374 | 709 | 5 % |
| citas_del_mes | 817 | 0 (0 %) | 37 | 311 | 687 | 5 % |
| navegacion | 1252 | 0 (0 %) | 12 | 204 | 702 | 4 % |
| cita | 990 | 76 (8 %) | 32 | 129 | 253 | 3 % |
| privacidad | 1252 | 0 (0 %) | 15 | 114 | 341 | 3 % |
| mascota | 990 | 0 (0 %) | 24 | 106 | 351 | 3 % |
| cumplimiento | 1252 | 0 (0 %) | 12 | 68 | 274 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 41 | 334 | 367 | 0 % |
| 0.3 | 35 | 193 | 327 | 544 | 1 % |
| 0.8 | 65 | 297 | 290 | 580 | 0 % |
| 1.4 | 95 | 404 | 128 | 497 | 0 % |
| 1.9 | 125 | 542 | 89 | 376 | 0 % |
| 2.4 | 155 | 655 | 124 | 507 | 0 % |
| 2.9 | 185 | 746 | 163 | 712 | 0 % |
| 3.4 | 215 | 943 | 102 | 428 | 0 % |
| 3.9 | 245 | 1000 | 172 | 511 | 0 % |
| 4.3 | 250 | 977 | 209 | 558 | 1 % |
| 4.8 | 250 | 923 | 134 | 419 | 0 % |
| 5.3 | 250 | 996 | 280 | 539 | 0 % |
| 5.8 | 250 | 881 | 261 | 881 | 0 % |
| 6.3 | 250 | 939 | 68 | 136 | 1 % |
| 6.8 | 215 | 334 | 69 | 429 | 3 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
