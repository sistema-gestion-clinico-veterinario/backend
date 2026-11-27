# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 50 |
| Peticiones hechas | 2 791 |
| Peticiones correctas (2xx) | 2 783 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 11.1 |
| Tiempo mediano (ms) | 16 |
| Tiempo p95 (ms) | 118 |
| Tiempo p99 (ms) | 426 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 9 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 8 |
| Memoria pico (MB, de 512) | 488 |
| Sentencias SQL | 74 533 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 791 | 11.1 | no aplica (ramping-vus) | 0.3 % | 0 | 1 / 2 / 2 | 0 % | 87 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1014; fallidas: 8.
- Acciones hechas: panel 133, agenda 100, historia 99, reportes 8, recepcion 145, atencion 61, cobro 36, cancelar 28; sin trabajo disponible: 105.
- CPU del backend: 30 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 8 % de media y 27 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 488 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 145 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 338 pausas, 21.1 s en total (8 % del tiempo de vida de la JVM); 63 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 242 MB (tope -Xmx256m).
- Clases cargadas: 25247 en total, 381 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1746 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4592 |
| 0.1 | 32 | 32 | 7992 |
| 0.1 | 60 | 61 | 13639 |
| 0.2 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16115 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 81 | 82 | 17416 |
| 0.9 | 110 | 112 | 23896 |
| 1.6 | 110 | 115 | 24654 |
| 2.4 | 110 | 115 | 24899 |
| 3.1 | 111 | 115 | 25138 |
| 4.0 | 111 | 115 | 25246 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 369 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 380 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.9 | 243.44 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 3.1 | 17.03 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.8 | 9.71 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.5 | 94.35 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.4 | 13.94 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 100 | 0.9 | 9.2 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.21 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.3 | 0.97 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.27 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 145 | 0.2 | 1.38 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 145 | 145 | coincide |
| Mascotas | 145 | 145 | coincide |
| Citas agendadas | 137 | 137 | coincide |
| Diagnósticos | 61 | 61 | coincide |
| Recetas | 61 | 61 | coincide |
| Pagos | 36 | 36 | coincide |
| Citas completadas | 61 | 61 | coincide |
| Citas canceladas | 28 | 28 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 145 | 0 | 0.0 % | 27 | 184 | 417 |
| Registrar mascota | 145 | 0 | 0.0 % | 20 | 67 | 151 |
| Agendar cita | 137 | 8 | 5.5 % | 23 | 72 | 318 |
| Recepción (llegada) | 61 | 0 | 0.0 % | 11 | 98 | 155 |
| Iniciar atención | 61 | 0 | 0.0 % | 15 | 43 | 60 |
| Leer la consulta | 61 | 0 | 0.0 % | 9 | 34 | 53 |
| Registrar diagnóstico | 61 | 0 | 0.0 % | 9 | 31 | 41 |
| Registrar receta | 61 | 0 | 0.0 % | 9 | 31 | 52 |
| Actualizar mascota | 61 | 0 | 0.0 % | 10 | 30 | 80 |
| Cerrar la consulta | 61 | 0 | 0.0 % | 18 | 49 | 172 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 10 | 239 | 364 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 10 | 36 | 51 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 9 | 31 | 109 |
| Cobrar | 36 | 0 | 0.0 % | 13 | 38 | 43 |
| Cancelar cita | 28 | 0 | 0.0 % | 21 | 87 | 298 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 8 | 0 (0 %) | 3 818 | 5 679 | 5 679 | 23 % |
| login | 50 | 0 (0 %) | 375 | 1 650 | 1 949 | 19 % |
| panel | 183 | 0 (0 %) | 59 | 138 | 412 | 10 % |
| apoderado | 145 | 0 (0 %) | 27 | 184 | 417 | 5 % |
| propietarios | 100 | 0 (0 %) | 34 | 152 | 440 | 5 % |
| citas_del_mes | 100 | 0 (0 %) | 33 | 329 | 451 | 4 % |
| cita | 145 | 8 (6 %) | 23 | 72 | 318 | 4 % |
| lista_de_historias | 99 | 0 (0 %) | 28 | 200 | 403 | 3 % |
| mascota | 145 | 0 (0 %) | 20 | 67 | 151 | 3 % |
| navegacion | 183 | 0 (0 %) | 8 | 46 | 306 | 3 % |
| historia | 99 | 0 (0 %) | 18 | 54 | 353 | 2 % |
| privacidad | 183 | 0 (0 %) | 7 | 24 | 37 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 63 | 366 | 391 | 0 % |
| 0.2 | 23 | 147 | 1 053 | 1 949 | 0 % |
| 0.4 | 37 | 169 | 342 | 441 | 1 % |
| 0.7 | 50 | 238 | 336 | 415 | 0 % |
| 0.9 | 50 | 179 | 105 | 465 | 0 % |
| 1.2 | 50 | 172 | 91 | 387 | 0 % |
| 1.4 | 50 | 205 | 109 | 417 | 0 % |
| 1.7 | 50 | 215 | 58 | 70 | 1 % |
| 1.9 | 50 | 225 | 90 | 416 | 0 % |
| 2.2 | 50 | 168 | 53 | 306 | 0 % |
| 2.4 | 50 | 214 | 274 | 412 | 0 % |
| 2.7 | 50 | 204 | 40 | 68 | 0 % |
| 2.9 | 50 | 210 | 134 | 364 | 0 % |
| 3.2 | 50 | 184 | 50 | 61 | 1 % |
| 3.4 | 49 | 158 | 54 | 83 | 0 % |
| 3.7 | 38 | 40 | 40 | 50 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
