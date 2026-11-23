# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 256m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 8 |
| Peticiones hechas | 2 787 |
| Peticiones correctas (2xx) | 2 782 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 11.1 |
| Tiempo mediano (ms) | 15 |
| Tiempo p95 (ms) | 96 |
| Tiempo p99 (ms) | 434 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 28 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 486 |
| Sentencias SQL | 74 867 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 787 | 11.1 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1010; fallidas: 5.
- Acciones hechas: panel 133, agenda 100, historia 100, reportes 8, recepcion 146, atencion 60, cobro 35, cancelar 29; sin trabajo disponible: 109.
- CPU del backend: 28 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 21 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 486 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **8 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 146 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 339 pausas, 21.1 s en total (8 % del tiempo de vida de la JVM); 65 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 242 MB (tope -Xmx256m).
- Clases cargadas: 25266 en total, 442 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1748 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4591 |
| 0.1 | 32 | 32 | 7992 |
| 0.1 | 60 | 61 | 13639 |
| 0.1 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16115 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 81 | 82 | 17467 |
| 0.8 | 110 | 112 | 23907 |
| 1.5 | 110 | 115 | 24608 |
| 2.3 | 111 | 115 | 24920 |
| 3.0 | 111 | 115 | 25160 |
| 3.9 | 111 | 115 | 25266 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 423 |
| `java.lang.invoke.LambdaForm$DMH/` | 18 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 441 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.4 | 210.72 | 27 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then $2 else $3 end),$4) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apo` | 183 | 1.8 | 9.74 | 14 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 183 | 1.2 | 6.49 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.2 | 72.78 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 1.1 | 10.77 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.5 | 1.76 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 283 | 0.2 | 0.84 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 283 | 0.2 | 0.76 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.0 | 1 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 100 | 0.2 | 1.51 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 146 | 146 | coincide |
| Mascotas | 146 | 146 | coincide |
| Citas agendadas | 141 | 141 | coincide |
| Diagnósticos | 60 | 60 | coincide |
| Recetas | 60 | 60 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 60 | 60 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 146 | 0 | 0.0 % | 24 | 185 | 390 |
| Registrar mascota | 146 | 0 | 0.0 % | 17 | 53 | 73 |
| Agendar cita | 141 | 5 | 3.4 % | 20 | 85 | 484 |
| Recepción (llegada) | 60 | 0 | 0.0 % | 14 | 179 | 250 |
| Iniciar atención | 60 | 0 | 0.0 % | 19 | 56 | 424 |
| Leer la consulta | 60 | 0 | 0.0 % | 12 | 36 | 240 |
| Registrar diagnóstico | 60 | 0 | 0.0 % | 11 | 30 | 34 |
| Registrar receta | 60 | 0 | 0.0 % | 11 | 33 | 47 |
| Actualizar mascota | 60 | 0 | 0.0 % | 12 | 42 | 397 |
| Cerrar la consulta | 60 | 0 | 0.0 % | 21 | 68 | 296 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 10 | 286 | 381 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 10 | 48 | 391 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 10 | 45 | 52 |
| Cobrar | 35 | 0 | 0.0 % | 15 | 48 | 472 |
| Cancelar cita | 29 | 0 | 0.0 % | 16 | 277 | 406 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 8 | 0 (0 %) | 3 461 | 4 643 | 4 643 | 24 % |
| login | 50 | 0 (0 %) | 286 | 1 341 | 1 481 | 16 % |
| panel | 183 | 0 (0 %) | 47 | 96 | 449 | 9 % |
| apoderado | 146 | 0 (0 %) | 24 | 185 | 390 | 6 % |
| citas_del_mes | 100 | 0 (0 %) | 22 | 382 | 541 | 5 % |
| cita | 146 | 5 (3 %) | 20 | 85 | 484 | 4 % |
| lista_de_historias | 100 | 0 (0 %) | 25 | 304 | 470 | 4 % |
| propietarios | 100 | 0 (0 %) | 26 | 120 | 138 | 3 % |
| mascota | 146 | 0 (0 %) | 17 | 53 | 73 | 3 % |
| navegacion | 183 | 0 (0 %) | 9 | 39 | 295 | 3 % |
| historia | 100 | 0 (0 %) | 16 | 46 | 91 | 2 % |
| cumplimiento | 183 | 0 (0 %) | 7 | 27 | 46 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 290 | 330 | 0 % |
| 0.1 | 21 | 135 | 539 | 1 481 | 1 % |
| 0.4 | 35 | 158 | 283 | 323 | 1 % |
| 0.7 | 49 | 254 | 262 | 316 | 0 % |
| 0.9 | 50 | 182 | 88 | 484 | 0 % |
| 1.1 | 50 | 166 | 134 | 470 | 0 % |
| 1.4 | 50 | 208 | 108 | 390 | 0 % |
| 1.6 | 50 | 215 | 54 | 67 | 0 % |
| 1.9 | 50 | 192 | 250 | 683 | 0 % |
| 2.1 | 50 | 196 | 50 | 62 | 0 % |
| 2.4 | 50 | 219 | 310 | 466 | 0 % |
| 2.6 | 50 | 209 | 35 | 43 | 0 % |
| 2.9 | 50 | 204 | 56 | 419 | 0 % |
| 3.1 | 50 | 184 | 36 | 44 | 1 % |
| 3.4 | 50 | 159 | 36 | 56 | 0 % |
| 3.6 | 41 | 50 | 24 | 37 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
