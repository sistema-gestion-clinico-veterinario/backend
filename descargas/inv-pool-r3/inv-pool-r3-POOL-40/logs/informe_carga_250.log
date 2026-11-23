# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Peticiones hechas | 19 756 |
| Peticiones correctas (2xx) | 19 732 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.2 |
| Tiempo mediano (ms) | 34 |
| Tiempo p95 (ms) | 336 |
| Tiempo p99 (ms) | 1 024 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 68 |
| Tiempo con la CPU al tope (%) | 31 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 42 |
| Memoria pico (MB, de 2048) | 1 295 |
| Sentencias SQL | 538 481 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 756 | 44.2 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 10 / 12 | 0 % | 286 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7185; fallidas: 24.
- Acciones hechas: panel 986, agenda 809, historia 725, reportes 93, recepcion 967, atencion 475, cobro 312, cancelar 196; sin trabajo disponible: 724.
- CPU del backend: 68 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 31 % del tiempo.
- CPU de PostgreSQL: 42 % de media y 101 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 295 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.2 s.
- Correo: el servidor de pruebas recibió 967 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 470 pausas, 52.5 s en total (11 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 945 MB (tope -Xmx1g).
- Clases cargadas: 25562 en total, 180 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4660 |
| 0.1 | 49 | 50 | 10951 |
| 0.2 | 80 | 81 | 16056 |
| 0.3 | 82 | 83 | 16417 |
| 0.4 | 88 | 89 | 17306 |
| 1.7 | 129 | 132 | 24602 |
| 2.6 | 132 | 135 | 25061 |
| 3.4 | 134 | 137 | 25347 |
| 4.0 | 134 | 137 | 25399 |
| 4.7 | 135 | 138 | 25474 |
| 5.3 | 136 | 138 | 25475 |
| 5.9 | 136 | 139 | 25516 |
| 6.7 | 136 | 139 | 25561 |
| 7.5 | 136 | 139 | 25562 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 168 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 175 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 186 | 77.9 | 419.02 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1236 | 56.2 | 45.48 | 17 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1236 | 45.4 | 36.75 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 725 | 31.7 | 43.72 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 186 | 25.0 | 134.15 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1236 | 21.1 | 17.07 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2045 | 15.7 | 7.68 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 93 | 5.0 | 53.57 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2045 | 4.5 | 2.2 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110142 | 4.4 | 0.04 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 967 | 967 | coincide |
| Mascotas | 967 | 967 | coincide |
| Citas agendadas | 959 | 959 | coincide |
| Diagnósticos | 459 | 459 | coincide |
| Recetas | 459 | 459 | coincide |
| Pagos | 312 | 312 | coincide |
| Citas completadas | 459 | 459 | coincide |
| Citas canceladas | 196 | 196 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 967 | 0 | 0.0 % | 62 | 386 | 895 |
| Registrar mascota | 967 | 0 | 0.0 % | 34 | 134 | 383 |
| Agendar cita | 959 | 8 | 0.8 % | 47 | 230 | 640 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 33 | 401 | 796 |
| Iniciar atención | 459 | 16 | 3.4 % | 44 | 174 | 248 |
| Leer la consulta | 459 | 0 | 0.0 % | 24 | 101 | 412 |
| Registrar diagnóstico | 459 | 0 | 0.0 % | 24 | 81 | 253 |
| Registrar receta | 459 | 0 | 0.0 % | 27 | 105 | 300 |
| Actualizar mascota | 459 | 0 | 0.0 % | 27 | 103 | 231 |
| Cerrar la consulta | 459 | 0 | 0.0 % | 43 | 173 | 292 |
| Caja: crear punto de cobro | 185 | 0 | 0.0 % | 28 | 434 | 806 |
| Caja: vincular equipo | 185 | 0 | 0.0 % | 22 | 84 | 245 |
| Caja: abrir la caja | 185 | 0 | 0.0 % | 29 | 133 | 404 |
| Cobrar | 312 | 0 | 0.0 % | 38 | 216 | 586 |
| Cancelar cita | 196 | 0 | 0.0 % | 39 | 442 | 739 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 93 | 0 (0 %) | 3 791 | 8 533 | 10 337 | 19 % |
| panel | 1236 | 0 (0 %) | 146 | 591 | 948 | 13 % |
| login | 250 | 0 (0 %) | 566 | 3 280 | 4 653 | 12 % |
| propietarios | 809 | 0 (0 %) | 125 | 397 | 793 | 6 % |
| apoderado | 967 | 0 (0 %) | 62 | 386 | 895 | 5 % |
| lista_de_historias | 725 | 0 (0 %) | 77 | 482 | 801 | 5 % |
| citas_del_mes | 809 | 0 (0 %) | 51 | 455 | 756 | 4 % |
| cita | 967 | 8 (1 %) | 47 | 230 | 640 | 4 % |
| navegacion | 1236 | 0 (0 %) | 19 | 258 | 661 | 3 % |
| privacidad | 1236 | 0 (0 %) | 24 | 179 | 445 | 3 % |
| mascota | 967 | 0 (0 %) | 34 | 134 | 383 | 3 % |
| historia | 725 | 0 (0 %) | 41 | 160 | 336 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 42 | 363 | 386 | 0 % |
| 0.3 | 35 | 195 | 507 | 1 686 | 0 % |
| 0.8 | 65 | 289 | 299 | 819 | 0 % |
| 1.4 | 95 | 396 | 230 | 663 | 0 % |
| 1.9 | 125 | 568 | 221 | 737 | 0 % |
| 2.4 | 155 | 697 | 277 | 1 106 | 0 % |
| 2.9 | 185 | 749 | 191 | 1 053 | 0 % |
| 3.4 | 215 | 908 | 270 | 921 | 0 % |
| 3.9 | 245 | 1041 | 376 | 3 270 | 0 % |
| 4.3 | 250 | 918 | 599 | 1 375 | 0 % |
| 4.8 | 250 | 898 | 257 | 583 | 0 % |
| 5.3 | 250 | 990 | 291 | 887 | 0 % |
| 5.8 | 250 | 928 | 371 | 835 | 0 % |
| 6.3 | 250 | 974 | 158 | 320 | 0 % |
| 6.8 | 216 | 331 | 127 | 263 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
