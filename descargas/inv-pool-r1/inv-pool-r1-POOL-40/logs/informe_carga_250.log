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
| Peticiones hechas | 19 935 |
| Peticiones correctas (2xx) | 19 915 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 249 |
| Tiempo p99 (ms) | 907 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 64 |
| Tiempo con la CPU al tope (%) | 17 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 |
| Memoria pico (MB, de 2048) | 1 307 |
| Sentencias SQL | 543 023 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 935 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 13 | 0 % | 285 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7275; fallidas: 20.
- Acciones hechas: panel 996, agenda 814, historia 730, reportes 94, recepcion 976, atencion 475, cobro 315, cancelar 202; sin trabajo disponible: 731.
- CPU del backend: 64 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 17 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 98 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 307 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 976 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 481 pausas, 49.3 s en total (11 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 961 MB (tope -Xmx1g).
- Clases cargadas: 25565 en total, 166 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4699 |
| 0.1 | 50 | 50 | 11015 |
| 0.3 | 80 | 81 | 16082 |
| 0.4 | 85 | 86 | 17020 |
| 0.5 | 94 | 95 | 18129 |
| 1.9 | 131 | 133 | 24804 |
| 2.8 | 133 | 136 | 25299 |
| 3.4 | 134 | 137 | 25383 |
| 4.0 | 134 | 137 | 25402 |
| 4.7 | 135 | 138 | 25476 |
| 5.3 | 136 | 139 | 25478 |
| 5.9 | 136 | 139 | 25546 |
| 6.7 | 136 | 139 | 25564 |
| 7.5 | 136 | 139 | 25565 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 153 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 161 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 79.0 | 420.45 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1246 | 50.0 | 40.14 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1246 | 41.2 | 33.03 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 730 | 28.6 | 39.16 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 25.9 | 137.93 | 9 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1246 | 17.0 | 13.68 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2060 | 13.3 | 6.47 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.0 | 42.32 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110879 | 3.4 | 0.03 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2060 | 3.3 | 1.61 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 976 | 976 | coincide |
| Mascotas | 976 | 976 | coincide |
| Citas agendadas | 965 | 965 | coincide |
| Diagnósticos | 466 | 466 | coincide |
| Recetas | 466 | 466 | coincide |
| Pagos | 315 | 315 | coincide |
| Citas completadas | 466 | 466 | coincide |
| Citas canceladas | 202 | 202 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 976 | 0 | 0.0 % | 45 | 356 | 738 |
| Registrar mascota | 976 | 0 | 0.0 % | 27 | 86 | 196 |
| Agendar cita | 965 | 11 | 1.1 % | 38 | 124 | 247 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 22 | 350 | 678 |
| Iniciar atención | 466 | 9 | 1.9 % | 32 | 102 | 249 |
| Leer la consulta | 466 | 0 | 0.0 % | 20 | 61 | 205 |
| Registrar diagnóstico | 466 | 0 | 0.0 % | 19 | 55 | 163 |
| Registrar receta | 466 | 0 | 0.0 % | 19 | 59 | 111 |
| Actualizar mascota | 466 | 0 | 0.0 % | 19 | 55 | 187 |
| Cerrar la consulta | 466 | 0 | 0.0 % | 35 | 98 | 232 |
| Caja: crear punto de cobro | 190 | 0 | 0.0 % | 16 | 196 | 675 |
| Caja: vincular equipo | 190 | 0 | 0.0 % | 16 | 51 | 85 |
| Caja: abrir la caja | 190 | 0 | 0.0 % | 20 | 61 | 160 |
| Cobrar | 315 | 0 | 0.0 % | 25 | 227 | 730 |
| Cancelar cita | 202 | 0 | 0.0 % | 25 | 357 | 931 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 3 074 | 6 536 | 8 024 | 21 % |
| panel | 1246 | 0 (0 %) | 107 | 562 | 1 080 | 14 % |
| login | 250 | 0 (0 %) | 435 | 1 647 | 2 504 | 11 % |
| propietarios | 814 | 0 (0 %) | 84 | 315 | 515 | 6 % |
| apoderado | 976 | 0 (0 %) | 45 | 356 | 738 | 6 % |
| lista_de_historias | 730 | 0 (0 %) | 52 | 454 | 1 013 | 5 % |
| citas_del_mes | 814 | 0 (0 %) | 36 | 302 | 602 | 4 % |
| cita | 976 | 11 (1 %) | 38 | 124 | 247 | 3 % |
| navegacion | 1246 | 0 (0 %) | 14 | 144 | 503 | 3 % |
| privacidad | 1246 | 0 (0 %) | 17 | 124 | 234 | 3 % |
| mascota | 976 | 0 (0 %) | 27 | 86 | 196 | 2 % |
| historia | 730 | 0 (0 %) | 32 | 102 | 201 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 335 | 353 | 0 % |
| 0.5 | 43 | 239 | 316 | 683 | 1 % |
| 1.0 | 73 | 298 | 331 | 979 | 0 % |
| 1.5 | 103 | 447 | 96 | 383 | 0 % |
| 2.0 | 133 | 647 | 88 | 404 | 0 % |
| 2.5 | 163 | 650 | 307 | 1 327 | 0 % |
| 3.0 | 193 | 761 | 221 | 1 885 | 0 % |
| 3.5 | 223 | 927 | 134 | 656 | 0 % |
| 4.0 | 250 | 972 | 229 | 895 | 0 % |
| 4.5 | 250 | 941 | 343 | 1 058 | 0 % |
| 5.0 | 250 | 987 | 240 | 526 | 0 % |
| 5.5 | 250 | 964 | 233 | 470 | 0 % |
| 6.0 | 250 | 890 | 150 | 353 | 0 % |
| 6.5 | 250 | 838 | 353 | 994 | 0 % |
| 7.0 | 157 | 111 | 66 | 1 009 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
