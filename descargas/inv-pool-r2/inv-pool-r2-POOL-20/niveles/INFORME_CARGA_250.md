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
| Peticiones hechas | 19 990 |
| Peticiones correctas (2xx) | 19 899 |
| Errores (%) | 0.5 % |
| Ritmo medio (peticiones/s) | 44.8 |
| Tiempo mediano (ms) | 21 |
| Tiempo p95 (ms) | 193 |
| Tiempo p99 (ms) | 706 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 62 |
| Tiempo con la CPU al tope (%) | 19 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 37 |
| Memoria pico (MB, de 2048) | 1 241 |
| Sentencias SQL | 543 961 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 990 | 44.8 | no aplica (ramping-vus) | 0.5 % | 0 | 3 / 8 / 11 | 0 % | 281 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7220; fallidas: 91.
- Acciones hechas: panel 1000, agenda 818, historia 731, reportes 94, recepcion 995, atencion 472, cobro 321, cancelar 192; sin trabajo disponible: 749.
- CPU del backend: 62 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 19 % del tiempo.
- CPU de PostgreSQL: 37 % de media y 90 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 241 MB (61 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 995 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 505 pausas, 43.5 s en total (9 % del tiempo de vida de la JVM); 51 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 904 MB (tope -Xmx1g).
- Clases cargadas: 25566 en total, 165 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 20 | 20 | 4864 |
| 0.2 | 56 | 56 | 11894 |
| 0.3 | 80 | 81 | 16086 |
| 0.4 | 86 | 87 | 17109 |
| 0.9 | 123 | 125 | 23763 |
| 2.3 | 132 | 134 | 24987 |
| 3.1 | 133 | 136 | 25318 |
| 3.7 | 134 | 137 | 25403 |
| 4.3 | 135 | 138 | 25478 |
| 4.9 | 135 | 138 | 25479 |
| 5.4 | 136 | 139 | 25518 |
| 6.1 | 136 | 139 | 25563 |
| 6.8 | 136 | 139 | 25565 |
| 7.7 | 136 | 139 | 25566 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 153 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 160 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 61.3 | 325.82 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1250 | 39.7 | 31.76 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1250 | 35.4 | 28.34 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 22.6 | 30.95 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 168 | 20.0 | 119.06 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 1779 | 19.2 | 10.77 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2068 | 11.4 | 5.52 | 4 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1250 | 9.9 | 7.93 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.5 | 36.82 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2068 | 3.2 | 1.53 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 995 | 995 | coincide |
| Mascotas | 995 | 995 | coincide |
| Citas agendadas | 915 | 915 | coincide |
| Diagnósticos | 461 | 461 | coincide |
| Recetas | 461 | 461 | coincide |
| Pagos | 321 | 321 | coincide |
| Citas completadas | 461 | 461 | coincide |
| Citas canceladas | 192 | 192 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 995 | 0 | 0.0 % | 41 | 230 | 594 |
| Registrar mascota | 995 | 0 | 0.0 % | 22 | 66 | 158 |
| Agendar cita | 915 | 80 | 8.0 % | 31 | 101 | 236 |
| Recepción (llegada) | 472 | 0 | 0.0 % | 18 | 155 | 517 |
| Iniciar atención | 461 | 11 | 2.3 % | 27 | 103 | 213 |
| Leer la consulta | 461 | 0 | 0.0 % | 16 | 47 | 114 |
| Registrar diagnóstico | 461 | 0 | 0.0 % | 15 | 47 | 121 |
| Registrar receta | 461 | 0 | 0.0 % | 16 | 49 | 153 |
| Actualizar mascota | 461 | 0 | 0.0 % | 16 | 47 | 127 |
| Cerrar la consulta | 461 | 0 | 0.0 % | 29 | 82 | 185 |
| Caja: crear punto de cobro | 188 | 0 | 0.0 % | 14 | 166 | 433 |
| Caja: vincular equipo | 188 | 0 | 0.0 % | 13 | 37 | 177 |
| Caja: abrir la caja | 188 | 0 | 0.0 % | 17 | 54 | 101 |
| Cobrar | 321 | 0 | 0.0 % | 21 | 76 | 517 |
| Cancelar cita | 192 | 0 | 0.0 % | 21 | 170 | 425 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 272 | 5 133 | 6 182 | 20 % |
| panel | 1250 | 0 (0 %) | 92 | 449 | 905 | 15 % |
| login | 250 | 0 (0 %) | 390 | 1 414 | 2 232 | 11 % |
| propietarios | 818 | 0 (0 %) | 70 | 250 | 393 | 6 % |
| apoderado | 995 | 0 (0 %) | 41 | 230 | 594 | 6 % |
| lista_de_historias | 731 | 0 (0 %) | 44 | 255 | 531 | 5 % |
| citas_del_mes | 818 | 0 (0 %) | 35 | 215 | 575 | 4 % |
| cita | 995 | 80 (8 %) | 31 | 101 | 236 | 3 % |
| navegacion | 1250 | 0 (0 %) | 10 | 109 | 464 | 3 % |
| privacidad | 1250 | 0 (0 %) | 14 | 75 | 168 | 3 % |
| mascota | 995 | 0 (0 %) | 22 | 66 | 158 | 2 % |
| empleados | 818 | 0 (0 %) | 20 | 63 | 445 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 82 | 319 | 343 | 0 % |
| 0.5 | 41 | 196 | 286 | 477 | 1 % |
| 0.9 | 71 | 301 | 278 | 530 | 0 % |
| 1.4 | 101 | 433 | 77 | 385 | 1 % |
| 1.9 | 131 | 592 | 84 | 391 | 0 % |
| 2.5 | 161 | 626 | 236 | 907 | 0 % |
| 3.0 | 191 | 779 | 343 | 1 734 | 0 % |
| 3.5 | 221 | 923 | 114 | 535 | 0 % |
| 4.0 | 250 | 1048 | 176 | 597 | 0 % |
| 4.5 | 250 | 981 | 347 | 974 | 1 % |
| 5.0 | 250 | 958 | 161 | 448 | 1 % |
| 5.5 | 250 | 1008 | 122 | 221 | 0 % |
| 6.0 | 250 | 882 | 196 | 869 | 1 % |
| 6.5 | 250 | 888 | 170 | 478 | 1 % |
| 7.0 | 174 | 161 | 100 | 896 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
