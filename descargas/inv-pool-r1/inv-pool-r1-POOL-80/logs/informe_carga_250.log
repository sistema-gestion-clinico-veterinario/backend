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
| Peticiones correctas (2xx) | 19 926 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 26 |
| Tiempo p95 (ms) | 252 |
| Tiempo p99 (ms) | 866 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 63 |
| Tiempo con la CPU al tope (%) | 21 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 295 |
| Sentencias SQL | 543 521 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 948 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 10 / 14 | 0 % | 282 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7283; fallidas: 22.
- Acciones hechas: panel 996, agenda 814, historia 731, reportes 94, recepcion 980, atencion 474, cobro 320, cancelar 195; sin trabajo disponible: 731.
- CPU del backend: 63 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 21 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 98 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 295 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 980 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 472 pausas, 49.3 s en total (11 % del tiempo de vida de la JVM); 47 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 942 MB (tope -Xmx1g).
- Clases cargadas: 25562 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4697 |
| 0.1 | 50 | 51 | 11014 |
| 0.2 | 80 | 81 | 16081 |
| 0.3 | 82 | 83 | 16437 |
| 0.5 | 100 | 102 | 19434 |
| 1.9 | 131 | 133 | 24807 |
| 2.9 | 133 | 136 | 25313 |
| 3.5 | 134 | 137 | 25381 |
| 4.1 | 135 | 138 | 25449 |
| 4.8 | 135 | 138 | 25474 |
| 5.4 | 136 | 139 | 25514 |
| 6.1 | 136 | 139 | 25559 |
| 6.8 | 136 | 139 | 25561 |
| 7.7 | 136 | 139 | 25562 |

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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 79.9 | 424.82 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1246 | 49.5 | 39.7 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1246 | 41.5 | 33.32 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 29.9 | 40.94 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 22.4 | 119.09 | 7 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1246 | 17.5 | 14.01 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2060 | 13.5 | 6.55 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.6 | 48.42 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2060 | 3.6 | 1.74 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2060 | 3.5 | 1.69 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 980 | 980 | coincide |
| Mascotas | 980 | 980 | coincide |
| Citas agendadas | 965 | 965 | coincide |
| Diagnósticos | 467 | 467 | coincide |
| Recetas | 467 | 467 | coincide |
| Pagos | 320 | 320 | coincide |
| Citas completadas | 467 | 467 | coincide |
| Citas canceladas | 195 | 195 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 980 | 0 | 0.0 % | 51 | 315 | 658 |
| Registrar mascota | 980 | 0 | 0.0 % | 29 | 77 | 158 |
| Agendar cita | 965 | 15 | 1.5 % | 42 | 147 | 343 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 24 | 242 | 625 |
| Iniciar atención | 467 | 7 | 1.5 % | 36 | 100 | 283 |
| Leer la consulta | 467 | 0 | 0.0 % | 20 | 53 | 135 |
| Registrar diagnóstico | 467 | 0 | 0.0 % | 20 | 56 | 167 |
| Registrar receta | 467 | 0 | 0.0 % | 21 | 62 | 98 |
| Actualizar mascota | 467 | 0 | 0.0 % | 20 | 57 | 153 |
| Cerrar la consulta | 467 | 0 | 0.0 % | 36 | 107 | 261 |
| Caja: crear punto de cobro | 189 | 0 | 0.0 % | 16 | 253 | 828 |
| Caja: vincular equipo | 189 | 0 | 0.0 % | 17 | 51 | 96 |
| Caja: abrir la caja | 189 | 0 | 0.0 % | 20 | 80 | 204 |
| Cobrar | 320 | 0 | 0.0 % | 28 | 187 | 490 |
| Cancelar cita | 195 | 0 | 0.0 % | 31 | 272 | 685 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 3 093 | 6 495 | 8 784 | 22 % |
| panel | 1246 | 0 (0 %) | 110 | 549 | 1 093 | 14 % |
| login | 250 | 0 (0 %) | 429 | 1 906 | 2 742 | 11 % |
| propietarios | 814 | 0 (0 %) | 87 | 344 | 605 | 6 % |
| apoderado | 980 | 0 (0 %) | 51 | 315 | 658 | 5 % |
| lista_de_historias | 731 | 0 (0 %) | 58 | 405 | 747 | 5 % |
| citas_del_mes | 814 | 0 (0 %) | 37 | 254 | 665 | 4 % |
| cita | 980 | 15 (2 %) | 42 | 147 | 343 | 4 % |
| privacidad | 1246 | 0 (0 %) | 17 | 130 | 271 | 3 % |
| navegacion | 1246 | 0 (0 %) | 14 | 136 | 530 | 3 % |
| mascota | 980 | 0 (0 %) | 29 | 77 | 158 | 2 % |
| historia | 731 | 0 (0 %) | 34 | 113 | 249 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 77 | 331 | 354 | 0 % |
| 0.4 | 40 | 192 | 314 | 479 | 0 % |
| 0.9 | 70 | 301 | 306 | 631 | 0 % |
| 1.4 | 100 | 441 | 104 | 478 | 0 % |
| 1.9 | 130 | 572 | 84 | 352 | 0 % |
| 2.4 | 160 | 656 | 163 | 862 | 0 % |
| 2.9 | 190 | 749 | 270 | 2 286 | 0 % |
| 3.4 | 220 | 908 | 108 | 479 | 0 % |
| 3.9 | 250 | 1022 | 225 | 747 | 0 % |
| 4.4 | 250 | 955 | 426 | 994 | 0 % |
| 4.9 | 250 | 930 | 244 | 685 | 0 % |
| 5.4 | 250 | 1024 | 137 | 294 | 0 % |
| 5.9 | 250 | 918 | 162 | 438 | 0 % |
| 6.4 | 250 | 951 | 195 | 629 | 0 % |
| 6.9 | 186 | 203 | 61 | 166 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
