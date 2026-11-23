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
| Usuarios sin degradación | 8 |
| Peticiones hechas | 19 688 |
| Peticiones correctas (2xx) | 19 650 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 43.9 |
| Tiempo mediano (ms) | 37 |
| Tiempo p95 (ms) | 407 |
| Tiempo p99 (ms) | 1 158 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 66 |
| Tiempo con la CPU al tope (%) | 21 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 47 |
| Memoria pico (MB, de 2048) | 1 338 |
| Sentencias SQL | 536 188 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 688 | 43.9 | no aplica (ramping-vus) | 0.2 % | 0 | 3 / 10 / 12 | 0 % | 284 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7133; fallidas: 38.
- Acciones hechas: panel 983, agenda 804, historia 726, reportes 93, recepcion 971, atencion 467, cobro 308, cancelar 195; sin trabajo disponible: 734.
- CPU del backend: 66 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 21 % del tiempo.
- CPU de PostgreSQL: 47 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 338 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **8 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.2 s.
- Correo: el servidor de pruebas recibió 971 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 476 pausas, 51.9 s en total (11 % del tiempo de vida de la JVM); 50 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25565 en total, 180 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4700 |
| 0.2 | 50 | 50 | 11014 |
| 0.3 | 80 | 81 | 16080 |
| 0.4 | 82 | 83 | 16418 |
| 0.5 | 89 | 90 | 17412 |
| 1.7 | 129 | 132 | 24604 |
| 2.7 | 132 | 135 | 25064 |
| 3.4 | 134 | 137 | 25381 |
| 4.0 | 134 | 137 | 25402 |
| 4.8 | 135 | 138 | 25477 |
| 5.4 | 136 | 139 | 25478 |
| 6.1 | 136 | 139 | 25546 |
| 6.8 | 136 | 139 | 25564 |
| 7.8 | 136 | 139 | 25565 |

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
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 186 | 94.0 | 505.29 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1233 | 71.8 | 58.21 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1233 | 68.1 | 55.2 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 726 | 37.7 | 51.9 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 186 | 31.2 | 167.92 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1233 | 20.8 | 16.83 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2037 | 17.6 | 8.66 | 4 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2037 | 7.3 | 3.6 | 2 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 93 | 5.6 | 60.5 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2037 | 4.4 | 2.18 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 971 | 971 | coincide |
| Mascotas | 971 | 971 | coincide |
| Citas agendadas | 948 | 948 | coincide |
| Diagnósticos | 452 | 452 | coincide |
| Recetas | 452 | 452 | coincide |
| Pagos | 308 | 308 | coincide |
| Citas completadas | 452 | 452 | coincide |
| Citas canceladas | 195 | 195 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 971 | 0 | 0.0 % | 69 | 522 | 890 |
| Registrar mascota | 971 | 0 | 0.0 % | 38 | 238 | 550 |
| Agendar cita | 948 | 23 | 2.4 % | 55 | 261 | 488 |
| Recepción (llegada) | 467 | 0 | 0.0 % | 38 | 388 | 829 |
| Iniciar atención | 452 | 15 | 3.2 % | 49 | 224 | 459 |
| Leer la consulta | 452 | 0 | 0.0 % | 28 | 147 | 408 |
| Registrar diagnóstico | 452 | 0 | 0.0 % | 28 | 169 | 433 |
| Registrar receta | 452 | 0 | 0.0 % | 29 | 130 | 347 |
| Actualizar mascota | 452 | 0 | 0.0 % | 28 | 181 | 344 |
| Cerrar la consulta | 452 | 0 | 0.0 % | 47 | 199 | 390 |
| Caja: crear punto de cobro | 187 | 0 | 0.0 % | 25 | 406 | 777 |
| Caja: vincular equipo | 187 | 0 | 0.0 % | 22 | 142 | 392 |
| Caja: abrir la caja | 187 | 0 | 0.0 % | 30 | 152 | 301 |
| Cobrar | 308 | 0 | 0.0 % | 36 | 225 | 520 |
| Cancelar cita | 195 | 0 | 0.0 % | 45 | 544 | 1 077 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 93 | 0 (0 %) | 3 862 | 8 037 | 8 417 | 17 % |
| panel | 1233 | 0 (0 %) | 172 | 769 | 1 341 | 14 % |
| login | 250 | 0 (0 %) | 606 | 3 155 | 4 125 | 10 % |
| propietarios | 804 | 0 (0 %) | 139 | 493 | 1 078 | 7 % |
| apoderado | 971 | 0 (0 %) | 69 | 522 | 890 | 6 % |
| lista_de_historias | 726 | 0 (0 %) | 91 | 544 | 1 004 | 5 % |
| citas_del_mes | 804 | 0 (0 %) | 64 | 432 | 932 | 4 % |
| cita | 971 | 23 (2 %) | 55 | 261 | 488 | 4 % |
| navegacion | 1233 | 0 (0 %) | 20 | 240 | 595 | 3 % |
| privacidad | 1233 | 0 (0 %) | 24 | 224 | 384 | 3 % |
| mascota | 971 | 0 (0 %) | 38 | 238 | 550 | 3 % |
| historia | 726 | 0 (0 %) | 45 | 234 | 474 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 360 | 370 | 0 % |
| 0.4 | 38 | 185 | 365 | 761 | 1 % |
| 0.9 | 68 | 307 | 439 | 1 036 | 0 % |
| 1.4 | 98 | 433 | 211 | 745 | 0 % |
| 1.9 | 128 | 515 | 268 | 535 | 0 % |
| 2.4 | 158 | 627 | 188 | 830 | 0 % |
| 2.9 | 188 | 793 | 228 | 1 827 | 0 % |
| 3.4 | 218 | 955 | 212 | 649 | 0 % |
| 3.9 | 248 | 1015 | 385 | 1 062 | 0 % |
| 4.4 | 250 | 954 | 577 | 1 227 | 0 % |
| 4.9 | 250 | 949 | 180 | 428 | 0 % |
| 5.4 | 250 | 981 | 235 | 664 | 0 % |
| 5.9 | 250 | 839 | 638 | 1 784 | 0 % |
| 6.4 | 250 | 931 | 229 | 471 | 0 % |
| 6.9 | 193 | 243 | 190 | 663 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
