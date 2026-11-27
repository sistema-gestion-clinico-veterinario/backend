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
| Usuarios sin degradación | 19 |
| Peticiones hechas | 19 911 |
| Peticiones correctas (2xx) | 19 885 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 264 |
| Tiempo p99 (ms) | 825 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 63 |
| Tiempo con la CPU al tope (%) | 19 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 39 |
| Memoria pico (MB, de 2048) | 1 282 |
| Sentencias SQL | 539 713 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 911 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 14 | 0 % | 282 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7263; fallidas: 26.
- Acciones hechas: panel 995, agenda 811, historia 729, reportes 94, recepcion 985, atencion 476, cobro 315, cancelar 197; sin trabajo disponible: 733.
- CPU del backend: 63 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 19 % del tiempo.
- CPU de PostgreSQL: 39 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 282 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **19 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 985 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 508 pausas, 48.9 s en total (10 % del tiempo de vida de la JVM); 51 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 908 MB (tope -Xmx1g).
- Clases cargadas: 25565 en total, 163 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 20 | 4861 |
| 0.2 | 55 | 56 | 11732 |
| 0.3 | 80 | 81 | 16083 |
| 0.4 | 86 | 87 | 17108 |
| 0.8 | 118 | 120 | 22818 |
| 2.2 | 131 | 134 | 24936 |
| 3.1 | 133 | 136 | 25317 |
| 3.7 | 134 | 137 | 25402 |
| 4.3 | 135 | 138 | 25477 |
| 4.9 | 135 | 138 | 25478 |
| 5.4 | 136 | 139 | 25517 |
| 6.1 | 136 | 139 | 25562 |
| 6.7 | 136 | 139 | 25564 |
| 7.5 | 136 | 139 | 25565 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 158 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 70.6 | 375.64 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1245 | 44.9 | 36.08 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1245 | 37.1 | 29.8 | 13 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 811 | 24.4 | 30.09 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 729 | 22.6 | 30.95 | 8 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 19.6 | 104.21 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2056 | 10.1 | 4.92 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1245 | 9.5 | 7.66 | 3 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.0 | 43.09 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2056 | 3.2 | 1.57 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 985 | 985 | coincide |
| Mascotas | 985 | 985 | coincide |
| Citas agendadas | 972 | 972 | coincide |
| Diagnósticos | 463 | 463 | coincide |
| Recetas | 463 | 463 | coincide |
| Pagos | 315 | 315 | coincide |
| Citas completadas | 463 | 463 | coincide |
| Citas canceladas | 197 | 197 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 985 | 0 | 0.0 % | 47 | 353 | 756 |
| Registrar mascota | 985 | 0 | 0.0 % | 27 | 116 | 310 |
| Agendar cita | 972 | 13 | 1.3 % | 38 | 139 | 335 |
| Recepción (llegada) | 476 | 0 | 0.0 % | 22 | 206 | 629 |
| Iniciar atención | 463 | 13 | 2.7 % | 32 | 167 | 390 |
| Leer la consulta | 463 | 0 | 0.0 % | 18 | 92 | 256 |
| Registrar diagnóstico | 463 | 0 | 0.0 % | 18 | 85 | 234 |
| Registrar receta | 463 | 0 | 0.0 % | 19 | 74 | 317 |
| Actualizar mascota | 463 | 0 | 0.0 % | 19 | 80 | 239 |
| Cerrar la consulta | 463 | 0 | 0.0 % | 33 | 134 | 242 |
| Caja: crear punto de cobro | 185 | 0 | 0.0 % | 19 | 157 | 530 |
| Caja: vincular equipo | 185 | 0 | 0.0 % | 16 | 51 | 154 |
| Caja: abrir la caja | 185 | 0 | 0.0 % | 20 | 61 | 186 |
| Cobrar | 315 | 0 | 0.0 % | 23 | 160 | 502 |
| Cancelar cita | 197 | 0 | 0.0 % | 28 | 484 | 781 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 910 | 5 269 | 5 996 | 19 % |
| panel | 1245 | 0 (0 %) | 103 | 449 | 825 | 13 % |
| login | 250 | 0 (0 %) | 405 | 1 743 | 2 621 | 10 % |
| citas_del_mes | 811 | 0 (0 %) | 53 | 437 | 798 | 6 % |
| propietarios | 811 | 0 (0 %) | 81 | 303 | 623 | 6 % |
| apoderado | 985 | 0 (0 %) | 47 | 353 | 756 | 6 % |
| lista_de_historias | 729 | 0 (0 %) | 51 | 335 | 791 | 5 % |
| navegacion | 1245 | 0 (0 %) | 13 | 218 | 632 | 4 % |
| cita | 985 | 13 (1 %) | 38 | 139 | 335 | 3 % |
| mascota | 985 | 0 (0 %) | 27 | 116 | 310 | 3 % |
| privacidad | 1245 | 0 (0 %) | 15 | 117 | 256 | 2 % |
| empleados | 811 | 0 (0 %) | 25 | 133 | 288 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 28 | 349 | 351 | 0 % |
| 0.3 | 34 | 190 | 412 | 1 253 | 1 % |
| 0.8 | 64 | 291 | 290 | 834 | 0 % |
| 1.3 | 94 | 388 | 126 | 787 | 0 % |
| 1.8 | 124 | 555 | 248 | 648 | 0 % |
| 2.3 | 154 | 685 | 142 | 867 | 0 % |
| 2.8 | 184 | 819 | 145 | 839 | 0 % |
| 3.3 | 214 | 915 | 156 | 465 | 0 % |
| 3.8 | 244 | 1017 | 244 | 881 | 0 % |
| 4.3 | 250 | 982 | 282 | 763 | 0 % |
| 4.8 | 250 | 948 | 137 | 537 | 0 % |
| 5.3 | 250 | 981 | 313 | 767 | 0 % |
| 5.8 | 250 | 908 | 246 | 713 | 0 % |
| 6.3 | 250 | 991 | 95 | 186 | 0 % |
| 6.8 | 216 | 329 | 65 | 214 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
