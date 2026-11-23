# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 284 |
| Peticiones correctas (2xx) | 6 277 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 16 |
| Tiempo p95 (ms) | 264 |
| Tiempo p99 (ms) | 637 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 38 |
| Tiempo con la CPU al tope (%) | 5 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 34 |
| Memoria pico (MB, de 2048) | 898 |
| Sentencias SQL | 172 616 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 284 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 4 | 0 % | 136 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2364; fallidas: 7.
- Acciones hechas: panel 290, agenda 237, historia 232, reportes 22, recepcion 313, atencion 154, cobro 94, cancelar 55; sin trabajo disponible: 255.
- CPU del backend: 38 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 34 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 898 MB (44 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.6 %).
- GC: 292 pausas, 14.9 s en total (4 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 435 MB (tope -Xmx1g).
- Clases cargadas: 25427 en total, 278 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3807 |
| 0.1 | 23 | 24 | 5664 |
| 0.1 | 45 | 46 | 10177 |
| 0.2 | 66 | 67 | 13753 |
| 0.2 | 80 | 81 | 16074 |
| 0.3 | 80 | 81 | 16112 |
| 0.4 | 86 | 87 | 17103 |
| 0.4 | 93 | 94 | 17982 |
| 1.3 | 127 | 130 | 24444 |
| 2.1 | 131 | 134 | 24896 |
| 2.8 | 133 | 136 | 25203 |
| 3.5 | 133 | 136 | 25302 |
| 4.2 | 134 | 137 | 25352 |
| 5.1 | 134 | 137 | 25425 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 268 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 278 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 390 | 46.6 | 119.57 | 38 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 390 | 19.6 | 50.38 | 16 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 390 | 14.6 | 37.44 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 8.6 | 195.94 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 390 | 6.3 | 16.18 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 390 | 5.9 | 15.18 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 390 | 4.6 | 11.87 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 4.2 | 18.23 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 2.8 | 63.73 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 627 | 1.5 | 2.39 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 307 | 307 | coincide |
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
| Registrar propietario | 313 | 0 | 0.0 % | 35 | 83 | 343 |
| Registrar mascota | 313 | 0 | 0.0 % | 23 | 48 | 88 |
| Agendar cita | 307 | 6 | 1.9 % | 28 | 71 | 118 |
| Recepción (llegada) | 154 | 0 | 0.0 % | 10 | 62 | 451 |
| Iniciar atención | 153 | 1 | 0.6 % | 17 | 52 | 80 |
| Leer la consulta | 153 | 0 | 0.0 % | 10 | 36 | 105 |
| Registrar diagnóstico | 153 | 0 | 0.0 % | 10 | 30 | 82 |
| Registrar receta | 153 | 0 | 0.0 % | 11 | 33 | 62 |
| Actualizar mascota | 153 | 0 | 0.0 % | 10 | 33 | 47 |
| Cerrar la consulta | 153 | 0 | 0.0 % | 19 | 70 | 181 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 11 | 34 | 58 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 12 | 28 | 40 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 11 | 34 | 488 |
| Cobrar | 94 | 0 | 0.0 % | 12 | 38 | 132 |
| Cancelar cita | 55 | 0 | 0.0 % | 24 | 49 | 66 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| panel | 390 | 0 (0 %) | 249 | 734 | 1 090 | 36 % |
| login | 100 | 0 (0 %) | 328 | 1 086 | 1 400 | 12 % |
| reporte_clinico | 22 | 0 (0 %) | 1 589 | 2 403 | 4 126 | 11 % |
| apoderado | 313 | 0 (0 %) | 35 | 83 | 343 | 4 % |
| propietarios | 237 | 0 (0 %) | 31 | 132 | 186 | 4 % |
| lista_de_historias | 232 | 0 (0 %) | 35 | 97 | 242 | 3 % |
| cita | 313 | 6 (2 %) | 28 | 71 | 118 | 3 % |
| citas_del_mes | 237 | 0 (0 %) | 22 | 103 | 301 | 2 % |
| mascota | 313 | 0 (0 %) | 23 | 48 | 88 | 2 % |
| navegacion | 390 | 0 (0 %) | 12 | 38 | 289 | 2 % |
| historia | 232 | 0 (0 %) | 21 | 52 | 109 | 2 % |
| cumplimiento | 390 | 0 (0 %) | 10 | 31 | 47 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 72 | 325 | 354 | 0 % |
| 0.2 | 24 | 145 | 551 | 1 400 | 1 % |
| 0.5 | 38 | 180 | 303 | 456 | 1 % |
| 0.7 | 51 | 241 | 278 | 403 | 0 % |
| 0.9 | 65 | 284 | 315 | 914 | 0 % |
| 1.2 | 79 | 333 | 361 | 538 | 0 % |
| 1.4 | 92 | 407 | 313 | 560 | 0 % |
| 1.7 | 100 | 422 | 314 | 840 | 0 % |
| 1.9 | 100 | 464 | 115 | 628 | 0 % |
| 2.2 | 100 | 393 | 103 | 517 | 0 % |
| 2.5 | 100 | 404 | 108 | 599 | 0 % |
| 2.7 | 100 | 416 | 284 | 595 | 0 % |
| 3.0 | 100 | 406 | 278 | 549 | 0 % |
| 3.2 | 100 | 361 | 183 | 586 | 0 % |
| 3.5 | 100 | 381 | 97 | 480 | 0 % |
| 3.7 | 100 | 366 | 180 | 551 | 0 % |
| 4.0 | 100 | 431 | 115 | 299 | 0 % |
| 4.2 | 100 | 379 | 242 | 1 054 | 0 % |
| 4.5 | 90 | 165 | 301 | 974 | 0 % |
| 4.7 | 50 | 34 | 541 | 575 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
