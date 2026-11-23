# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 12 |
| Peticiones hechas | 6 283 |
| Peticiones correctas (2xx) | 6 274 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 19 |
| Tiempo p95 (ms) | 128 |
| Tiempo p99 (ms) | 605 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 54 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 17 |
| Memoria pico (MB, de 2048) | 868 |
| Sentencias SQL | 169 617 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 283 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 2 / 5 / 6 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2367; fallidas: 9.
- Acciones hechas: panel 289, agenda 237, historia 232, reportes 22, recepcion 313, atencion 155, cobro 94, cancelar 55; sin trabajo disponible: 255.
- CPU del backend: 54 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 17 % de media y 67 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 868 MB (42 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **12 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.7 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.9 %).
- GC: 299 pausas, 17.5 s en total (5 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 411 MB (tope -Xmx1g).
- Clases cargadas: 25426 en total, 287 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3819 |
| 0.1 | 24 | 25 | 5764 |
| 0.2 | 49 | 50 | 10927 |
| 0.3 | 73 | 74 | 14887 |
| 0.4 | 80 | 81 | 16106 |
| 0.4 | 82 | 83 | 16412 |
| 0.5 | 87 | 88 | 17211 |
| 0.6 | 98 | 99 | 18803 |
| 1.7 | 129 | 131 | 24532 |
| 2.4 | 131 | 134 | 24913 |
| 3.2 | 133 | 136 | 25264 |
| 3.8 | 134 | 136 | 25302 |
| 4.7 | 134 | 137 | 25358 |
| 5.4 | 135 | 138 | 25425 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 276 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 286 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 10.4 | 235.64 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then $2 else $3 end),$4) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apo` | 389 | 8.7 | 22.4 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 389 | 5.9 | 15.23 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 4.7 | 20.17 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 2.9 | 65.08 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 626 | 2.1 | 3.3 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 626 | 0.8 | 1.31 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 626 | 0.8 | 1.26 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 389 | 0.6 | 1.65 | 1 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 237 | 0.5 | 2.15 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 306 | 306 | coincide |
| Diagnósticos | 153 | 153 | coincide |
| Recetas | 153 | 153 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 153 | 153 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 37 | 208 | 423 |
| Registrar mascota | 313 | 0 | 0.0 % | 24 | 81 | 155 |
| Agendar cita | 306 | 7 | 2.2 % | 31 | 120 | 203 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 13 | 77 | 293 |
| Iniciar atención | 153 | 2 | 1.3 % | 22 | 71 | 177 |
| Leer la consulta | 153 | 0 | 0.0 % | 12 | 39 | 50 |
| Registrar diagnóstico | 153 | 0 | 0.0 % | 13 | 43 | 139 |
| Registrar receta | 153 | 0 | 0.0 % | 14 | 50 | 127 |
| Actualizar mascota | 153 | 0 | 0.0 % | 13 | 49 | 91 |
| Cerrar la consulta | 153 | 0 | 0.0 % | 24 | 88 | 142 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 11 | 30 | 243 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 11 | 30 | 170 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 13 | 52 | 164 |
| Cobrar | 94 | 0 | 0.0 % | 15 | 58 | 133 |
| Cancelar cita | 55 | 0 | 0.0 % | 22 | 103 | 270 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 534 | 1 836 | 2 152 | 22 % |
| reporte_clinico | 22 | 0 (0 %) | 1 882 | 3 989 | 7 473 | 16 % |
| panel | 389 | 0 (0 %) | 87 | 205 | 281 | 12 % |
| apoderado | 313 | 0 (0 %) | 37 | 208 | 423 | 6 % |
| cita | 313 | 7 (2 %) | 31 | 120 | 203 | 5 % |
| propietarios | 237 | 0 (0 %) | 39 | 112 | 223 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 37 | 99 | 190 | 4 % |
| mascota | 313 | 0 (0 %) | 24 | 81 | 155 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 25 | 117 | 427 | 3 % |
| navegacion | 389 | 0 (0 %) | 12 | 51 | 168 | 2 % |
| cumplimiento | 389 | 0 (0 %) | 12 | 46 | 144 | 2 % |
| historia | 232 | 0 (0 %) | 23 | 67 | 162 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 84 | 407 | 470 | 0 % |
| 0.2 | 26 | 148 | 1 658 | 2 152 | 1 % |
| 0.5 | 39 | 177 | 578 | 934 | 1 % |
| 0.7 | 53 | 252 | 362 | 660 | 0 % |
| 1.0 | 67 | 287 | 371 | 919 | 0 % |
| 1.2 | 80 | 337 | 217 | 644 | 0 % |
| 1.5 | 94 | 434 | 163 | 949 | 0 % |
| 1.7 | 100 | 420 | 111 | 697 | 0 % |
| 2.0 | 100 | 437 | 70 | 118 | 0 % |
| 2.2 | 100 | 400 | 97 | 484 | 0 % |
| 2.5 | 100 | 403 | 88 | 239 | 0 % |
| 2.7 | 100 | 422 | 125 | 265 | 0 % |
| 3.0 | 100 | 399 | 103 | 235 | 0 % |
| 3.2 | 100 | 354 | 70 | 227 | 0 % |
| 3.5 | 100 | 421 | 50 | 73 | 0 % |
| 3.7 | 100 | 383 | 62 | 97 | 0 % |
| 4.0 | 100 | 415 | 68 | 167 | 0 % |
| 4.2 | 100 | 353 | 88 | 390 | 0 % |
| 4.5 | 86 | 148 | 136 | 1 632 | 0 % |
| 4.7 | 44 | 9 | 62 | 62 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
