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
| Peticiones hechas | 19 948 |
| Peticiones correctas (2xx) | 19 929 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.5 |
| Tiempo mediano (ms) | 21 |
| Tiempo p95 (ms) | 255 |
| Tiempo p99 (ms) | 778 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 57 |
| Tiempo con la CPU al tope (%) | 18 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 |
| Memoria pico (MB, de 2048) | 1 242 |
| Sentencias SQL | 543 353 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 948 | 44.5 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 280 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7280; fallidas: 19.
- Acciones hechas: panel 997, agenda 816, historia 728, reportes 95, recepcion 983, atencion 474, cobro 317, cancelar 201; sin trabajo disponible: 735.
- CPU del backend: 57 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 18 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 95 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 242 MB (61 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 983 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 506 pausas, 44.5 s en total (10 % del tiempo de vida de la JVM); 50 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 884 MB (tope -Xmx1g).
- Clases cargadas: 25564 en total, 163 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 20 | 4866 |
| 0.1 | 56 | 56 | 11914 |
| 0.2 | 80 | 81 | 16108 |
| 0.3 | 86 | 87 | 17107 |
| 0.9 | 121 | 123 | 23242 |
| 2.2 | 132 | 134 | 24970 |
| 3.1 | 133 | 136 | 25316 |
| 3.7 | 134 | 137 | 25401 |
| 4.3 | 135 | 138 | 25476 |
| 4.9 | 135 | 138 | 25477 |
| 5.4 | 136 | 139 | 25517 |
| 6.1 | 136 | 139 | 25560 |
| 6.7 | 136 | 139 | 25563 |
| 7.6 | 136 | 139 | 25564 |

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
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 190 | 59.6 | 313.92 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1247 | 40.5 | 32.46 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1247 | 36.1 | 28.98 | 15 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 190 | 20.8 | 109.61 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 728 | 20.5 | 28.12 | 9 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1247 | 11.6 | 9.3 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2063 | 10.0 | 4.83 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 95 | 3.3 | 34.3 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2063 | 3.2 | 1.55 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111122 | 3.0 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 983 | 983 | coincide |
| Mascotas | 983 | 983 | coincide |
| Citas agendadas | 974 | 974 | coincide |
| Diagnósticos | 464 | 464 | coincide |
| Recetas | 464 | 464 | coincide |
| Pagos | 317 | 317 | coincide |
| Citas completadas | 464 | 464 | coincide |
| Citas canceladas | 201 | 201 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 983 | 0 | 0.0 % | 38 | 279 | 620 |
| Registrar mascota | 983 | 0 | 0.0 % | 21 | 117 | 301 |
| Agendar cita | 974 | 9 | 0.9 % | 31 | 146 | 500 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 19 | 246 | 700 |
| Iniciar atención | 464 | 10 | 2.1 % | 27 | 133 | 226 |
| Leer la consulta | 464 | 0 | 0.0 % | 15 | 99 | 249 |
| Registrar diagnóstico | 464 | 0 | 0.0 % | 14 | 93 | 349 |
| Registrar receta | 464 | 0 | 0.0 % | 15 | 99 | 380 |
| Actualizar mascota | 464 | 0 | 0.0 % | 15 | 89 | 273 |
| Cerrar la consulta | 464 | 0 | 0.0 % | 27 | 140 | 591 |
| Caja: crear punto de cobro | 188 | 0 | 0.0 % | 15 | 262 | 783 |
| Caja: vincular equipo | 188 | 0 | 0.0 % | 14 | 80 | 166 |
| Caja: abrir la caja | 188 | 0 | 0.0 % | 18 | 85 | 478 |
| Cobrar | 317 | 0 | 0.0 % | 21 | 201 | 458 |
| Cancelar cita | 201 | 0 | 0.0 % | 21 | 176 | 268 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 95 | 0 (0 %) | 2 385 | 5 450 | 6 246 | 19 % |
| panel | 1247 | 0 (0 %) | 90 | 475 | 788 | 14 % |
| login | 250 | 0 (0 %) | 360 | 1 427 | 1 865 | 9 % |
| propietarios | 816 | 0 (0 %) | 70 | 311 | 708 | 6 % |
| apoderado | 983 | 0 (0 %) | 38 | 279 | 620 | 5 % |
| lista_de_historias | 728 | 0 (0 %) | 40 | 359 | 758 | 5 % |
| citas_del_mes | 816 | 0 (0 %) | 30 | 390 | 707 | 4 % |
| cita | 983 | 9 (1 %) | 31 | 146 | 500 | 4 % |
| navegacion | 1247 | 0 (0 %) | 11 | 178 | 657 | 4 % |
| privacidad | 1247 | 0 (0 %) | 14 | 118 | 305 | 3 % |
| mascota | 983 | 0 (0 %) | 21 | 117 | 301 | 3 % |
| empleados | 816 | 0 (0 %) | 19 | 134 | 282 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 84 | 303 | 339 | 0 % |
| 0.5 | 42 | 211 | 291 | 445 | 1 % |
| 1.0 | 72 | 313 | 287 | 497 | 0 % |
| 1.5 | 102 | 454 | 73 | 376 | 0 % |
| 2.0 | 132 | 602 | 88 | 369 | 0 % |
| 2.5 | 162 | 633 | 269 | 819 | 0 % |
| 3.0 | 192 | 774 | 231 | 1 680 | 0 % |
| 3.5 | 222 | 952 | 102 | 472 | 0 % |
| 4.0 | 250 | 1033 | 229 | 458 | 0 % |
| 4.5 | 250 | 928 | 388 | 872 | 0 % |
| 5.0 | 250 | 939 | 288 | 791 | 0 % |
| 5.5 | 250 | 952 | 95 | 373 | 0 % |
| 6.0 | 250 | 878 | 166 | 497 | 0 % |
| 6.5 | 250 | 893 | 135 | 697 | 0 % |
| 7.0 | 164 | 117 | 131 | 1 564 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
