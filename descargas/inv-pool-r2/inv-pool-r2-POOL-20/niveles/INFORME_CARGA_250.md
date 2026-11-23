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
| Peticiones hechas | 20 050 |
| Peticiones correctas (2xx) | 20 032 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.8 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 183 |
| Tiempo p99 (ms) | 670 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 57 |
| Tiempo con la CPU al tope (%) | 14 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 36 |
| Memoria pico (MB, de 2048) | 1 260 |
| Sentencias SQL | 546 633 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 20 050 | 44.8 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 280 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7311; fallidas: 18.
- Acciones hechas: panel 1003, agenda 823, historia 733, reportes 95, recepcion 985, atencion 476, cobro 316, cancelar 203; sin trabajo disponible: 735.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 14 % del tiempo.
- CPU de PostgreSQL: 36 % de media y 84 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 260 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 985 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 484 pausas, 40.7 s en total (9 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 914 MB (tope -Xmx1g).
- Clases cargadas: 25566 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4699 |
| 0.1 | 50 | 50 | 11014 |
| 0.2 | 80 | 81 | 16083 |
| 0.3 | 86 | 87 | 17091 |
| 0.9 | 123 | 125 | 23758 |
| 2.2 | 132 | 134 | 24970 |
| 3.1 | 133 | 136 | 25317 |
| 3.6 | 134 | 137 | 25402 |
| 4.2 | 135 | 138 | 25454 |
| 4.7 | 135 | 138 | 25478 |
| 5.3 | 136 | 139 | 25479 |
| 5.9 | 136 | 139 | 25546 |
| 6.6 | 136 | 139 | 25565 |
| 7.4 | 136 | 139 | 25566 |

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
| `java.util.concurrent.locks` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 190 | 58.9 | 309.81 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1253 | 35.7 | 28.5 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1253 | 30.8 | 24.57 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 733 | 20.3 | 27.71 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 190 | 18.4 | 97.1 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2076 | 9.8 | 4.74 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1253 | 6.7 | 5.34 | 3 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 112000 | 3.2 | 0.03 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 95 | 3.2 | 33.21 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2076 | 3.1 | 1.51 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 985 | 985 | coincide |
| Mascotas | 985 | 985 | coincide |
| Citas agendadas | 974 | 974 | coincide |
| Diagnósticos | 469 | 469 | coincide |
| Recetas | 469 | 469 | coincide |
| Pagos | 316 | 316 | coincide |
| Citas completadas | 469 | 469 | coincide |
| Citas canceladas | 203 | 203 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 985 | 0 | 0.0 % | 35 | 201 | 602 |
| Registrar mascota | 985 | 0 | 0.0 % | 19 | 62 | 166 |
| Agendar cita | 974 | 11 | 1.1 % | 28 | 94 | 215 |
| Recepción (llegada) | 476 | 0 | 0.0 % | 16 | 166 | 574 |
| Iniciar atención | 469 | 7 | 1.5 % | 25 | 75 | 123 |
| Leer la consulta | 469 | 0 | 0.0 % | 13 | 48 | 86 |
| Registrar diagnóstico | 469 | 0 | 0.0 % | 13 | 44 | 106 |
| Registrar receta | 469 | 0 | 0.0 % | 15 | 48 | 167 |
| Actualizar mascota | 469 | 0 | 0.0 % | 14 | 44 | 82 |
| Cerrar la consulta | 469 | 0 | 0.0 % | 25 | 73 | 183 |
| Caja: crear punto de cobro | 186 | 0 | 0.0 % | 12 | 158 | 552 |
| Caja: vincular equipo | 186 | 0 | 0.0 % | 13 | 51 | 124 |
| Caja: abrir la caja | 186 | 0 | 0.0 % | 15 | 52 | 101 |
| Cobrar | 316 | 0 | 0.0 % | 20 | 113 | 379 |
| Cancelar cita | 203 | 0 | 0.0 % | 23 | 313 | 583 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 95 | 0 (0 %) | 2 208 | 5 055 | 5 690 | 21 % |
| panel | 1253 | 0 (0 %) | 80 | 357 | 734 | 14 % |
| login | 250 | 0 (0 %) | 360 | 1 384 | 2 124 | 12 % |
| propietarios | 823 | 0 (0 %) | 67 | 228 | 423 | 7 % |
| apoderado | 985 | 0 (0 %) | 35 | 201 | 602 | 5 % |
| lista_de_historias | 733 | 0 (0 %) | 39 | 222 | 617 | 5 % |
| citas_del_mes | 823 | 0 (0 %) | 27 | 208 | 655 | 4 % |
| cita | 985 | 11 (1 %) | 28 | 94 | 215 | 3 % |
| navegacion | 1253 | 0 (0 %) | 10 | 117 | 468 | 3 % |
| mascota | 985 | 0 (0 %) | 19 | 62 | 166 | 2 % |
| privacidad | 1253 | 0 (0 %) | 12 | 65 | 156 | 2 % |
| historia | 733 | 0 (0 %) | 24 | 65 | 136 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 15 | 106 | 313 | 510 | 0 % |
| 0.5 | 45 | 250 | 297 | 554 | 1 % |
| 1.0 | 75 | 331 | 257 | 478 | 0 % |
| 1.5 | 105 | 476 | 86 | 372 | 0 % |
| 2.0 | 135 | 617 | 67 | 325 | 0 % |
| 2.5 | 165 | 665 | 252 | 1 049 | 0 % |
| 3.0 | 195 | 782 | 273 | 1 536 | 0 % |
| 3.5 | 225 | 958 | 110 | 417 | 0 % |
| 4.0 | 250 | 1015 | 276 | 1 039 | 0 % |
| 4.5 | 250 | 1018 | 210 | 675 | 0 % |
| 5.0 | 250 | 1005 | 212 | 638 | 0 % |
| 5.5 | 250 | 925 | 144 | 621 | 0 % |
| 6.0 | 250 | 921 | 107 | 230 | 0 % |
| 6.5 | 250 | 891 | 378 | 1 017 | 0 % |
| 7.0 | 145 | 92 | 53 | 784 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
