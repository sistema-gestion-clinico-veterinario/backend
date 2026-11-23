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
| Usuarios sin degradación | 9 |
| Peticiones hechas | 19 950 |
| Peticiones correctas (2xx) | 19 918 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 23 |
| Tiempo p95 (ms) | 221 |
| Tiempo p99 (ms) | 828 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 61 |
| Tiempo con la CPU al tope (%) | 23 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 37 |
| Memoria pico (MB, de 2048) | 1 272 |
| Sentencias SQL | 543 560 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 950 | 44.4 | no aplica (ramping-vus) | 0.2 % | 0 | 3 / 8 / 11 | 0 % | 287 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7263; fallidas: 32.
- Acciones hechas: panel 997, agenda 816, historia 731, reportes 94, recepcion 980, atencion 473, cobro 319, cancelar 200; sin trabajo disponible: 736.
- CPU del backend: 61 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 23 % del tiempo.
- CPU de PostgreSQL: 37 % de media y 99 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 272 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 980 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 488 pausas, 46.0 s en total (10 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 907 MB (tope -Xmx1g).
- Clases cargadas: 25564 en total, 181 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.2 | 53 | 54 | 11396 |
| 0.3 | 80 | 81 | 16080 |
| 0.4 | 86 | 87 | 17106 |
| 0.6 | 114 | 116 | 22301 |
| 2.1 | 131 | 134 | 24918 |
| 3.0 | 133 | 136 | 25313 |
| 3.7 | 134 | 137 | 25397 |
| 4.3 | 135 | 138 | 25456 |
| 4.9 | 135 | 138 | 25476 |
| 5.4 | 136 | 139 | 25516 |
| 6.1 | 136 | 139 | 25561 |
| 6.8 | 136 | 139 | 25563 |
| 7.7 | 136 | 139 | 25564 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 168 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 176 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 70.2 | 373.45 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1247 | 44.0 | 35.25 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1247 | 37.6 | 30.16 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 25.6 | 34.97 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 19.9 | 105.71 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1247 | 14.4 | 11.53 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2063 | 12.0 | 5.8 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.4 | 35.8 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111107 | 3.3 | 0.03 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2063 | 3.2 | 1.54 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 980 | 980 | coincide |
| Mascotas | 980 | 980 | coincide |
| Citas agendadas | 957 | 957 | coincide |
| Diagnósticos | 464 | 464 | coincide |
| Recetas | 464 | 464 | coincide |
| Pagos | 319 | 319 | coincide |
| Citas completadas | 464 | 464 | coincide |
| Citas canceladas | 200 | 200 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 980 | 0 | 0.0 % | 45 | 295 | 685 |
| Registrar mascota | 980 | 0 | 0.0 % | 25 | 73 | 200 |
| Agendar cita | 957 | 23 | 2.3 % | 37 | 107 | 205 |
| Recepción (llegada) | 473 | 0 | 0.0 % | 21 | 199 | 660 |
| Iniciar atención | 464 | 9 | 1.9 % | 30 | 97 | 236 |
| Leer la consulta | 464 | 0 | 0.0 % | 17 | 53 | 198 |
| Registrar diagnóstico | 464 | 0 | 0.0 % | 16 | 48 | 112 |
| Registrar receta | 464 | 0 | 0.0 % | 18 | 53 | 83 |
| Actualizar mascota | 464 | 0 | 0.0 % | 17 | 53 | 140 |
| Cerrar la consulta | 464 | 0 | 0.0 % | 32 | 93 | 296 |
| Caja: crear punto de cobro | 190 | 0 | 0.0 % | 14 | 142 | 769 |
| Caja: vincular equipo | 190 | 0 | 0.0 % | 14 | 44 | 158 |
| Caja: abrir la caja | 190 | 0 | 0.0 % | 20 | 52 | 218 |
| Cobrar | 319 | 0 | 0.0 % | 23 | 86 | 183 |
| Cancelar cita | 200 | 0 | 0.0 % | 26 | 216 | 570 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 618 | 5 437 | 6 331 | 20 % |
| panel | 1247 | 0 (0 %) | 102 | 451 | 974 | 14 % |
| login | 250 | 0 (0 %) | 445 | 1 644 | 2 927 | 12 % |
| propietarios | 816 | 0 (0 %) | 80 | 288 | 693 | 6 % |
| apoderado | 980 | 0 (0 %) | 45 | 295 | 685 | 6 % |
| lista_de_historias | 731 | 0 (0 %) | 48 | 320 | 912 | 5 % |
| citas_del_mes | 816 | 0 (0 %) | 35 | 250 | 693 | 4 % |
| cita | 980 | 23 (2 %) | 37 | 107 | 205 | 3 % |
| navegacion | 1247 | 0 (0 %) | 13 | 142 | 493 | 3 % |
| privacidad | 1247 | 0 (0 %) | 16 | 114 | 230 | 3 % |
| mascota | 980 | 0 (0 %) | 25 | 73 | 200 | 2 % |
| historia | 731 | 0 (0 %) | 30 | 98 | 219 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 63 | 348 | 430 | 0 % |
| 0.4 | 39 | 188 | 365 | 585 | 1 % |
| 0.9 | 69 | 315 | 273 | 810 | 0 % |
| 1.4 | 99 | 425 | 134 | 532 | 0 % |
| 1.9 | 129 | 563 | 85 | 390 | 0 % |
| 2.4 | 159 | 646 | 160 | 972 | 0 % |
| 2.9 | 189 | 810 | 203 | 1 372 | 0 % |
| 3.4 | 219 | 932 | 172 | 678 | 0 % |
| 3.9 | 249 | 1039 | 190 | 564 | 0 % |
| 4.4 | 250 | 994 | 284 | 1 395 | 0 % |
| 4.9 | 250 | 957 | 175 | 705 | 0 % |
| 5.4 | 250 | 1026 | 218 | 658 | 0 % |
| 5.9 | 250 | 873 | 179 | 701 | 0 % |
| 6.4 | 250 | 967 | 95 | 193 | 0 % |
| 6.9 | 184 | 267 | 82 | 188 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
