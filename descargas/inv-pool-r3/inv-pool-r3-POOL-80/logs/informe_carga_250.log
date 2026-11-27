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
| Usuarios sin degradación | 17 |
| Peticiones hechas | 19 951 |
| Peticiones correctas (2xx) | 19 930 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 23 |
| Tiempo p95 (ms) | 258 |
| Tiempo p99 (ms) | 855 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 59 |
| Tiempo con la CPU al tope (%) | 20 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 300 |
| Sentencias SQL | 541 361 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 951 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 9 / 12 | 0 % | 284 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7293; fallidas: 21.
- Acciones hechas: panel 995, agenda 815, historia 730, reportes 94, recepcion 983, atencion 473, cobro 321, cancelar 195; sin trabajo disponible: 735.
- CPU del backend: 59 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 20 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 300 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **17 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.0 s.
- Correo: el servidor de pruebas recibió 983 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 495 pausas, 47.6 s en total (10 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 955 MB (tope -Xmx1g).
- Clases cargadas: 25562 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.1 | 53 | 54 | 11400 |
| 0.2 | 80 | 81 | 16083 |
| 0.3 | 86 | 87 | 17080 |
| 0.9 | 119 | 121 | 22876 |
| 2.1 | 131 | 134 | 24917 |
| 3.0 | 133 | 136 | 25313 |
| 3.6 | 134 | 137 | 25398 |
| 4.1 | 135 | 138 | 25399 |
| 4.6 | 135 | 138 | 25474 |
| 5.2 | 136 | 139 | 25475 |
| 5.9 | 136 | 139 | 25543 |
| 6.6 | 136 | 139 | 25561 |
| 7.5 | 136 | 139 | 25562 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 159 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 81.5 | 433.62 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1245 | 52.1 | 41.86 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1245 | 43.4 | 34.89 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 730 | 29.3 | 40.18 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 23.9 | 127.22 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1245 | 19.7 | 15.85 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 815 | 17.8 | 21.88 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2060 | 14.1 | 6.85 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.8 | 50.62 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and m1_0.activo=$5 and (cast($2 as bigint) is null or exists(select $6 from citas c2_0 where c2_0.mascota_` | 94 | 3.3 | 35.34 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 983 | 983 | coincide |
| Mascotas | 983 | 983 | coincide |
| Citas agendadas | 969 | 969 | coincide |
| Diagnósticos | 466 | 466 | coincide |
| Recetas | 466 | 466 | coincide |
| Pagos | 321 | 321 | coincide |
| Citas completadas | 466 | 466 | coincide |
| Citas canceladas | 195 | 195 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 983 | 0 | 0.0 % | 43 | 294 | 722 |
| Registrar mascota | 983 | 0 | 0.0 % | 25 | 82 | 198 |
| Agendar cita | 969 | 14 | 1.4 % | 36 | 122 | 222 |
| Recepción (llegada) | 473 | 0 | 0.0 % | 22 | 278 | 608 |
| Iniciar atención | 466 | 7 | 1.5 % | 30 | 109 | 218 |
| Leer la consulta | 466 | 0 | 0.0 % | 16 | 55 | 123 |
| Registrar diagnóstico | 466 | 0 | 0.0 % | 17 | 49 | 132 |
| Registrar receta | 466 | 0 | 0.0 % | 18 | 54 | 129 |
| Actualizar mascota | 466 | 0 | 0.0 % | 17 | 56 | 176 |
| Cerrar la consulta | 466 | 0 | 0.0 % | 30 | 87 | 204 |
| Caja: crear punto de cobro | 191 | 0 | 0.0 % | 16 | 197 | 962 |
| Caja: vincular equipo | 191 | 0 | 0.0 % | 15 | 46 | 122 |
| Caja: abrir la caja | 191 | 0 | 0.0 % | 20 | 66 | 133 |
| Cobrar | 321 | 0 | 0.0 % | 25 | 171 | 1 163 |
| Cancelar cita | 195 | 0 | 0.0 % | 24 | 231 | 1 003 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 775 | 8 787 | 11 798 | 22 % |
| panel | 1245 | 0 (0 %) | 91 | 586 | 1 333 | 15 % |
| login | 250 | 0 (0 %) | 378 | 1 458 | 2 097 | 9 % |
| propietarios | 815 | 0 (0 %) | 78 | 278 | 591 | 6 % |
| apoderado | 983 | 0 (0 %) | 43 | 294 | 722 | 5 % |
| lista_de_historias | 730 | 0 (0 %) | 50 | 429 | 800 | 5 % |
| citas_del_mes | 815 | 0 (0 %) | 41 | 326 | 811 | 5 % |
| navegacion | 1245 | 0 (0 %) | 12 | 234 | 540 | 3 % |
| cita | 983 | 14 (1 %) | 36 | 122 | 222 | 3 % |
| privacidad | 1245 | 0 (0 %) | 15 | 150 | 366 | 3 % |
| mascota | 983 | 0 (0 %) | 25 | 82 | 198 | 2 % |
| historia | 730 | 0 (0 %) | 28 | 89 | 188 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 20 | 344 | 344 | 0 % |
| 0.3 | 32 | 180 | 485 | 1 446 | 1 % |
| 0.8 | 62 | 269 | 268 | 481 | 0 % |
| 1.3 | 92 | 385 | 148 | 521 | 0 % |
| 1.8 | 122 | 573 | 93 | 500 | 0 % |
| 2.3 | 152 | 687 | 164 | 1 485 | 0 % |
| 2.8 | 182 | 809 | 125 | 632 | 0 % |
| 3.3 | 212 | 891 | 104 | 505 | 0 % |
| 3.8 | 242 | 999 | 286 | 888 | 0 % |
| 4.3 | 250 | 946 | 272 | 1 040 | 0 % |
| 4.8 | 250 | 941 | 176 | 595 | 0 % |
| 5.3 | 250 | 982 | 178 | 493 | 0 % |
| 5.8 | 250 | 929 | 307 | 654 | 0 % |
| 6.3 | 250 | 968 | 115 | 317 | 0 % |
| 6.8 | 233 | 416 | 955 | 6 667 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
