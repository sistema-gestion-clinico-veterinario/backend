# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Peticiones hechas | 2 800 |
| Peticiones correctas (2xx) | 2 793 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 11.2 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 84 |
| Tiempo p99 (ms) | 361 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 27 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 487 |
| Sentencias SQL | 75 147 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 800 | 11.2 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1018; fallidas: 7.
- Acciones hechas: panel 133, agenda 100, historia 101, reportes 8, recepcion 145, atencion 62, cobro 35, cancelar 28; sin trabajo disponible: 109.
- CPU del backend: 27 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 23 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 487 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 145 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 330 pausas, 17.4 s en total (7 % del tiempo de vida de la JVM); 62 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 246 MB (tope -Xmx256m).
- Clases cargadas: 25248 en total, 415 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1631 |
| 0.0 | 10 | 11 | 3591 |
| 0.0 | 14 | 14 | 4215 |
| 0.0 | 26 | 27 | 6882 |
| 0.1 | 53 | 54 | 12509 |
| 0.1 | 73 | 74 | 16051 |
| 0.2 | 74 | 75 | 16106 |
| 0.2 | 79 | 80 | 17088 |
| 0.2 | 80 | 81 | 17305 |
| 0.8 | 109 | 112 | 23740 |
| 1.5 | 112 | 115 | 24642 |
| 2.0 | 110 | 115 | 24833 |
| 2.9 | 111 | 115 | 25108 |
| 3.5 | 111 | 115 | 25169 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 401 |
| `java.lang.invoke.LambdaForm$DMH/` | 14 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 415 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.1 | 195.64 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 2.4 | 13.05 | 18 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 101 | 1.3 | 12.78 | 10 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.2 | 6.81 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.1 | 67.19 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.03 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.2 | 0.86 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 283 | 0.2 | 0.82 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.16 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 145 | 0.2 | 1.16 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 145 | 145 | coincide |
| Mascotas | 145 | 145 | coincide |
| Citas agendadas | 138 | 138 | coincide |
| Diagnósticos | 62 | 62 | coincide |
| Recetas | 62 | 62 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 62 | 62 | coincide |
| Citas canceladas | 28 | 28 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 145 | 0 | 0.0 % | 21 | 184 | 359 |
| Registrar mascota | 145 | 0 | 0.0 % | 14 | 46 | 330 |
| Agendar cita | 138 | 7 | 4.8 % | 18 | 88 | 269 |
| Recepción (llegada) | 62 | 0 | 0.0 % | 10 | 156 | 352 |
| Iniciar atención | 62 | 0 | 0.0 % | 13 | 42 | 361 |
| Leer la consulta | 62 | 0 | 0.0 % | 7 | 28 | 45 |
| Registrar diagnóstico | 62 | 0 | 0.0 % | 7 | 29 | 213 |
| Registrar receta | 62 | 0 | 0.0 % | 8 | 30 | 40 |
| Actualizar mascota | 62 | 0 | 0.0 % | 9 | 28 | 31 |
| Cerrar la consulta | 62 | 0 | 0.0 % | 16 | 79 | 353 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 9 | 23 | 65 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 7 | 23 | 32 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 8 | 29 | 53 |
| Cobrar | 35 | 0 | 0.0 % | 12 | 33 | 55 |
| Cancelar cita | 28 | 0 | 0.0 % | 13 | 74 | 159 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 8 | 0 (0 %) | 3 177 | 4 964 | 4 964 | 24 % |
| login | 50 | 0 (0 %) | 340 | 1 488 | 1 966 | 21 % |
| panel | 183 | 0 (0 %) | 49 | 92 | 160 | 9 % |
| apoderado | 145 | 0 (0 %) | 21 | 184 | 359 | 5 % |
| citas_del_mes | 100 | 0 (0 %) | 19 | 271 | 379 | 4 % |
| cita | 145 | 7 (5 %) | 18 | 88 | 269 | 4 % |
| propietarios | 100 | 0 (0 %) | 23 | 123 | 462 | 4 % |
| lista_de_historias | 101 | 0 (0 %) | 24 | 76 | 248 | 3 % |
| mascota | 145 | 0 (0 %) | 14 | 46 | 330 | 3 % |
| navegacion | 183 | 0 (0 %) | 7 | 29 | 214 | 2 % |
| cerrar_consulta | 62 | 0 (0 %) | 16 | 79 | 353 | 2 % |
| historia | 101 | 0 (0 %) | 12 | 38 | 57 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 340 | 361 | 0 % |
| 0.2 | 24 | 147 | 485 | 1 966 | 0 % |
| 0.5 | 38 | 185 | 323 | 380 | 0 % |
| 0.7 | 50 | 236 | 314 | 357 | 0 % |
| 0.9 | 50 | 171 | 104 | 417 | 1 % |
| 1.2 | 50 | 166 | 74 | 368 | 2 % |
| 1.4 | 50 | 214 | 84 | 341 | 0 % |
| 1.7 | 50 | 218 | 45 | 55 | 0 % |
| 1.9 | 50 | 223 | 74 | 379 | 0 % |
| 2.2 | 50 | 179 | 62 | 177 | 1 % |
| 2.5 | 50 | 216 | 92 | 297 | 0 % |
| 2.7 | 50 | 192 | 45 | 71 | 0 % |
| 3.0 | 50 | 211 | 107 | 352 | 0 % |
| 3.2 | 50 | 186 | 45 | 49 | 1 % |
| 3.5 | 50 | 143 | 46 | 60 | 0 % |
| 3.7 | 35 | 43 | 27 | 50 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
