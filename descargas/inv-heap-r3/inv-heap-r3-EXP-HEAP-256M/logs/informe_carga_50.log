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
| Peticiones hechas | 2 784 |
| Peticiones correctas (2xx) | 2 776 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 11.0 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 113 |
| Tiempo p99 (ms) | 413 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 28 |
| Tiempo con la CPU al tope (%) | 11 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 488 |
| Sentencias SQL | 74 317 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 784 | 11.0 | no aplica (ramping-vus) | 0.3 % | 0 | 1 / 2 / 2 | 0 % | 83 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1010; fallidas: 8.
- Acciones hechas: panel 133, agenda 99, historia 99, reportes 8, recepcion 146, atencion 60, cobro 36, cancelar 28; sin trabajo disponible: 110.
- CPU del backend: 28 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 21 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 488 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 146 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 340 pausas, 20.4 s en total (8 % del tiempo de vida de la JVM); 65 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25247 en total, 411 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1745 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4594 |
| 0.1 | 32 | 32 | 7991 |
| 0.1 | 60 | 61 | 13638 |
| 0.1 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16115 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 81 | 82 | 17416 |
| 0.8 | 110 | 112 | 23811 |
| 1.6 | 110 | 115 | 24649 |
| 2.3 | 111 | 115 | 24901 |
| 3.0 | 111 | 115 | 25140 |
| 4.0 | 111 | 115 | 25246 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 396 |
| `java.lang.invoke.LambdaForm$DMH/` | 14 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 410 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.1 | 195.45 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 2.6 | 14.35 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.3 | 7.37 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.3 | 13.32 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.1 | 66.71 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 99 | 1.0 | 9.87 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 282 | 0.6 | 2.06 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 282 | 0.2 | 0.88 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.23 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 146 | 0.2 | 1.27 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 146 | 146 | coincide |
| Mascotas | 146 | 146 | coincide |
| Citas agendadas | 138 | 138 | coincide |
| Diagnósticos | 60 | 60 | coincide |
| Recetas | 60 | 60 | coincide |
| Pagos | 36 | 36 | coincide |
| Citas completadas | 60 | 60 | coincide |
| Citas canceladas | 28 | 28 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 146 | 0 | 0.0 % | 24 | 191 | 351 |
| Registrar mascota | 146 | 0 | 0.0 % | 16 | 52 | 241 |
| Agendar cita | 138 | 8 | 5.5 % | 20 | 63 | 397 |
| Recepción (llegada) | 60 | 0 | 0.0 % | 12 | 209 | 338 |
| Iniciar atención | 60 | 0 | 0.0 % | 14 | 46 | 403 |
| Leer la consulta | 60 | 0 | 0.0 % | 7 | 30 | 42 |
| Registrar diagnóstico | 60 | 0 | 0.0 % | 8 | 35 | 278 |
| Registrar receta | 60 | 0 | 0.0 % | 8 | 39 | 64 |
| Actualizar mascota | 60 | 0 | 0.0 % | 9 | 29 | 37 |
| Cerrar la consulta | 60 | 0 | 0.0 % | 16 | 71 | 293 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 10 | 118 | 184 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 8 | 26 | 404 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 8 | 24 | 32 |
| Cobrar | 36 | 0 | 0.0 % | 11 | 29 | 30 |
| Cancelar cita | 28 | 0 | 0.0 % | 14 | 51 | 361 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 8 | 0 (0 %) | 3 528 | 5 361 | 5 361 | 23 % |
| login | 50 | 0 (0 %) | 352 | 1 819 | 2 196 | 20 % |
| panel | 183 | 0 (0 %) | 50 | 111 | 540 | 9 % |
| apoderado | 146 | 0 (0 %) | 24 | 191 | 351 | 5 % |
| citas_del_mes | 99 | 0 (0 %) | 32 | 276 | 421 | 5 % |
| propietarios | 99 | 0 (0 %) | 23 | 128 | 492 | 4 % |
| cita | 146 | 8 (5 %) | 20 | 63 | 397 | 4 % |
| lista_de_historias | 99 | 0 (0 %) | 27 | 207 | 379 | 4 % |
| navegacion | 183 | 0 (0 %) | 8 | 50 | 317 | 3 % |
| mascota | 146 | 0 (0 %) | 16 | 52 | 241 | 3 % |
| historia | 99 | 0 (0 %) | 16 | 48 | 331 | 2 % |
| cumplimiento | 183 | 0 (0 %) | 6 | 27 | 82 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 91 | 340 | 591 | 0 % |
| 0.2 | 26 | 143 | 723 | 2 196 | 1 % |
| 0.5 | 39 | 210 | 322 | 386 | 1 % |
| 0.7 | 50 | 220 | 123 | 386 | 0 % |
| 1.0 | 50 | 169 | 180 | 449 | 0 % |
| 1.2 | 50 | 186 | 201 | 367 | 0 % |
| 1.5 | 50 | 207 | 49 | 285 | 0 % |
| 1.7 | 50 | 215 | 46 | 51 | 0 % |
| 2.0 | 50 | 202 | 197 | 409 | 0 % |
| 2.2 | 50 | 190 | 118 | 421 | 0 % |
| 2.5 | 50 | 212 | 209 | 448 | 0 % |
| 2.7 | 50 | 208 | 46 | 49 | 0 % |
| 3.0 | 50 | 194 | 82 | 397 | 1 % |
| 3.2 | 50 | 213 | 46 | 62 | 0 % |
| 3.5 | 47 | 97 | 46 | 57 | 0 % |
| 3.7 | 31 | 27 | 28 | 45 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
