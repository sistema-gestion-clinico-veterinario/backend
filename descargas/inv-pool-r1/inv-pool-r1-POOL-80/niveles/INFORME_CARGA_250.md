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
| Peticiones hechas | 19 929 |
| Peticiones correctas (2xx) | 19 912 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 257 |
| Tiempo p99 (ms) | 854 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 65 |
| Tiempo con la CPU al tope (%) | 17 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 39 |
| Memoria pico (MB, de 2048) | 1 297 |
| Sentencias SQL | 542 866 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 929 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 13 | 0 % | 282 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7281; fallidas: 17.
- Acciones hechas: panel 996, agenda 812, historia 729, reportes 94, recepcion 976, atencion 476, cobro 319, cancelar 200; sin trabajo disponible: 731.
- CPU del backend: 65 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 17 % del tiempo.
- CPU de PostgreSQL: 39 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 297 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 976 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 471 pausas, 48.1 s en total (10 % del tiempo de vida de la JVM); 46 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 950 MB (tope -Xmx1g).
- Clases cargadas: 25567 en total, 214 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4697 |
| 0.1 | 50 | 50 | 11014 |
| 0.2 | 80 | 81 | 16079 |
| 0.3 | 86 | 87 | 17078 |
| 0.5 | 100 | 102 | 19411 |
| 1.9 | 131 | 133 | 24806 |
| 2.9 | 133 | 136 | 25319 |
| 3.7 | 134 | 137 | 25404 |
| 4.3 | 135 | 138 | 25463 |
| 4.9 | 135 | 138 | 25480 |
| 5.4 | 136 | 139 | 25519 |
| 6.1 | 136 | 139 | 25564 |
| 6.8 | 136 | 139 | 25567 |
| 7.6 | 136 | 139 | 25567 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 202 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 210 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 83.6 | 444.94 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1246 | 53.8 | 43.17 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1246 | 48.1 | 38.61 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 729 | 31.1 | 42.71 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 27.7 | 147.1 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1246 | 19.1 | 15.33 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2058 | 15.0 | 7.3 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.5 | 47.43 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2058 | 3.7 | 1.81 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2058 | 3.4 | 1.65 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 976 | 976 | coincide |
| Mascotas | 976 | 976 | coincide |
| Citas agendadas | 968 | 968 | coincide |
| Diagnósticos | 467 | 467 | coincide |
| Recetas | 467 | 467 | coincide |
| Pagos | 319 | 319 | coincide |
| Citas completadas | 467 | 467 | coincide |
| Citas canceladas | 200 | 200 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 976 | 0 | 0.0 % | 48 | 293 | 752 |
| Registrar mascota | 976 | 0 | 0.0 % | 26 | 74 | 183 |
| Agendar cita | 968 | 8 | 0.8 % | 38 | 129 | 275 |
| Recepción (llegada) | 476 | 0 | 0.0 % | 23 | 244 | 673 |
| Iniciar atención | 467 | 9 | 1.9 % | 33 | 124 | 270 |
| Leer la consulta | 467 | 0 | 0.0 % | 20 | 57 | 120 |
| Registrar diagnóstico | 467 | 0 | 0.0 % | 18 | 63 | 244 |
| Registrar receta | 467 | 0 | 0.0 % | 21 | 62 | 138 |
| Actualizar mascota | 467 | 0 | 0.0 % | 18 | 60 | 105 |
| Cerrar la consulta | 467 | 0 | 0.0 % | 34 | 97 | 259 |
| Caja: crear punto de cobro | 188 | 0 | 0.0 % | 18 | 170 | 643 |
| Caja: vincular equipo | 188 | 0 | 0.0 % | 18 | 50 | 119 |
| Caja: abrir la caja | 188 | 0 | 0.0 % | 21 | 72 | 162 |
| Cobrar | 319 | 0 | 0.0 % | 27 | 144 | 633 |
| Cancelar cita | 200 | 0 | 0.0 % | 27 | 358 | 782 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 3 205 | 7 443 | 8 262 | 22 % |
| panel | 1246 | 0 (0 %) | 100 | 699 | 1 043 | 15 % |
| login | 250 | 0 (0 %) | 395 | 1 454 | 2 302 | 9 % |
| propietarios | 812 | 0 (0 %) | 82 | 306 | 583 | 6 % |
| apoderado | 976 | 0 (0 %) | 48 | 293 | 752 | 5 % |
| lista_de_historias | 729 | 0 (0 %) | 49 | 374 | 811 | 5 % |
| citas_del_mes | 812 | 0 (0 %) | 36 | 273 | 847 | 4 % |
| cita | 976 | 8 (1 %) | 38 | 129 | 275 | 3 % |
| privacidad | 1246 | 0 (0 %) | 16 | 188 | 325 | 3 % |
| navegacion | 1246 | 0 (0 %) | 13 | 152 | 578 | 3 % |
| mascota | 976 | 0 (0 %) | 26 | 74 | 183 | 2 % |
| historia | 729 | 0 (0 %) | 30 | 90 | 209 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 14 | 342 | 342 | 0 % |
| 0.3 | 31 | 177 | 484 | 1 454 | 0 % |
| 0.8 | 61 | 276 | 348 | 1 054 | 0 % |
| 1.3 | 91 | 391 | 106 | 457 | 0 % |
| 1.8 | 121 | 576 | 108 | 544 | 0 % |
| 2.3 | 151 | 685 | 165 | 1 011 | 0 % |
| 2.8 | 181 | 791 | 127 | 518 | 0 % |
| 3.3 | 211 | 868 | 117 | 495 | 0 % |
| 3.8 | 241 | 1011 | 203 | 847 | 0 % |
| 4.3 | 250 | 945 | 392 | 913 | 0 % |
| 4.8 | 250 | 934 | 284 | 782 | 0 % |
| 5.3 | 250 | 985 | 169 | 622 | 0 % |
| 5.8 | 250 | 926 | 362 | 1 071 | 0 % |
| 6.3 | 250 | 1005 | 95 | 228 | 0 % |
| 6.8 | 237 | 467 | 430 | 1 576 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
