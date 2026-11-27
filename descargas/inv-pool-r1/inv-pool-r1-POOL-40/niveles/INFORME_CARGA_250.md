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
| Peticiones hechas | 19 905 |
| Peticiones correctas (2xx) | 19 829 |
| Errores (%) | 0.4 % |
| Ritmo medio (peticiones/s) | 44.6 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 271 |
| Tiempo p99 (ms) | 1 184 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 67 |
| Tiempo con la CPU al tope (%) | 28 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 340 |
| Sentencias SQL | 542 039 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 905 | 44.6 | no aplica (ramping-vus) | 0.4 % | 0 | 3 / 9 / 12 | 0 % | 283 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7204; fallidas: 76.
- Acciones hechas: panel 995, agenda 813, historia 728, reportes 94, recepcion 989, atencion 470, cobro 320, cancelar 192; sin trabajo disponible: 749.
- CPU del backend: 67 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 28 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 100 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 340 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.9 s.
- Correo: el servidor de pruebas recibió 989 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 134 MB (+0.0 %).
- GC: 494 pausas, 53.6 s en total (12 % del tiempo de vida de la JVM); 54 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25560 en total, 214 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.2 | 53 | 54 | 11374 |
| 0.2 | 80 | 81 | 16083 |
| 0.4 | 86 | 87 | 17081 |
| 0.5 | 100 | 101 | 19329 |
| 2.0 | 131 | 133 | 24830 |
| 3.0 | 133 | 136 | 25312 |
| 3.5 | 134 | 137 | 25380 |
| 4.1 | 135 | 137 | 25398 |
| 4.7 | 135 | 138 | 25473 |
| 5.3 | 136 | 139 | 25473 |
| 6.0 | 136 | 139 | 25540 |
| 6.7 | 136 | 139 | 25560 |
| 7.7 | 134 | 140 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 202 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 210 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 78.7 | 418.42 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1245 | 49.3 | 39.6 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1245 | 40.9 | 32.88 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 728 | 27.7 | 38.04 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 174 | 21.3 | 122.66 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1245 | 14.4 | 11.59 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2058 | 12.9 | 6.28 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 1836 | 8.8 | 4.81 | 3 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.9 | 41.11 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2058 | 3.4 | 1.65 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 989 | 989 | coincide |
| Mascotas | 989 | 989 | coincide |
| Citas agendadas | 923 | 923 | coincide |
| Diagnósticos | 460 | 460 | coincide |
| Recetas | 460 | 460 | coincide |
| Pagos | 320 | 320 | coincide |
| Citas completadas | 460 | 460 | coincide |
| Citas canceladas | 192 | 192 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 989 | 0 | 0.0 % | 48 | 384 | 1 148 |
| Registrar mascota | 989 | 0 | 0.0 % | 27 | 105 | 202 |
| Agendar cita | 923 | 66 | 6.7 % | 38 | 143 | 336 |
| Recepción (llegada) | 470 | 0 | 0.0 % | 23 | 329 | 944 |
| Iniciar atención | 460 | 10 | 2.1 % | 33 | 148 | 300 |
| Leer la consulta | 460 | 0 | 0.0 % | 20 | 67 | 138 |
| Registrar diagnóstico | 460 | 0 | 0.0 % | 19 | 63 | 113 |
| Registrar receta | 460 | 0 | 0.0 % | 21 | 78 | 219 |
| Actualizar mascota | 460 | 0 | 0.0 % | 19 | 67 | 189 |
| Cerrar la consulta | 460 | 0 | 0.0 % | 34 | 145 | 1 096 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 19 | 153 | 558 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 16 | 51 | 147 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 19 | 65 | 190 |
| Cobrar | 320 | 0 | 0.0 % | 26 | 260 | 1 195 |
| Cancelar cita | 192 | 0 | 0.0 % | 28 | 562 | 1 032 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 697 | 14 743 | 15 639 | 23 % |
| panel | 1245 | 0 (0 %) | 104 | 688 | 1 683 | 15 % |
| login | 250 | 0 (0 %) | 415 | 1 719 | 2 485 | 9 % |
| apoderado | 989 | 0 (0 %) | 48 | 384 | 1 148 | 6 % |
| propietarios | 813 | 0 (0 %) | 83 | 302 | 896 | 6 % |
| lista_de_historias | 728 | 0 (0 %) | 50 | 428 | 1 169 | 5 % |
| citas_del_mes | 813 | 0 (0 %) | 37 | 400 | 937 | 4 % |
| navegacion | 1245 | 0 (0 %) | 13 | 174 | 842 | 3 % |
| cita | 989 | 66 (7 %) | 38 | 143 | 336 | 3 % |
| privacidad | 1245 | 0 (0 %) | 16 | 121 | 322 | 3 % |
| mascota | 989 | 0 (0 %) | 27 | 105 | 202 | 2 % |
| checkin | 470 | 0 (0 %) | 23 | 329 | 944 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 31 | 349 | 355 | 0 % |
| 0.3 | 34 | 182 | 334 | 768 | 1 % |
| 0.8 | 64 | 296 | 304 | 796 | 0 % |
| 1.3 | 94 | 388 | 111 | 524 | 0 % |
| 1.8 | 124 | 559 | 113 | 405 | 0 % |
| 2.3 | 154 | 679 | 131 | 690 | 0 % |
| 2.8 | 184 | 803 | 144 | 1 026 | 0 % |
| 3.3 | 214 | 904 | 165 | 566 | 0 % |
| 3.8 | 244 | 1014 | 189 | 733 | 0 % |
| 4.3 | 250 | 968 | 360 | 1 023 | 1 % |
| 4.8 | 250 | 927 | 112 | 213 | 0 % |
| 5.3 | 250 | 1016 | 109 | 278 | 0 % |
| 5.8 | 250 | 875 | 212 | 469 | 0 % |
| 6.3 | 250 | 953 | 67 | 120 | 0 % |
| 6.8 | 221 | 378 | 1 924 | 15 043 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
