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
| Usuarios sin degradación | 2 |
| Peticiones hechas | 2 786 |
| Peticiones correctas (2xx) | 2 781 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 11.1 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 100 |
| Tiempo p99 (ms) | 395 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 27 |
| Tiempo con la CPU al tope (%) | 9 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 488 |
| Sentencias SQL | 74 817 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 786 | 11.1 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1009; fallidas: 5.
- Acciones hechas: panel 133, agenda 100, historia 100, reportes 8, recepcion 145, atencion 61, cobro 35, cancelar 29; sin trabajo disponible: 109.
- CPU del backend: 27 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 21 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 488 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **2 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 145 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 331 pausas, 20.3 s en total (8 % del tiempo de vida de la JVM); 65 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 239 MB (tope -Xmx256m).
- Clases cargadas: 25271 en total, 443 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1631 |
| 0.0 | 10 | 11 | 3591 |
| 0.0 | 14 | 14 | 4233 |
| 0.1 | 26 | 26 | 6797 |
| 0.1 | 53 | 54 | 12515 |
| 0.1 | 73 | 74 | 16051 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 80 | 81 | 17369 |
| 0.8 | 110 | 112 | 23820 |
| 1.5 | 112 | 115 | 24609 |
| 2.0 | 110 | 115 | 24859 |
| 2.9 | 111 | 115 | 25098 |
| 3.6 | 111 | 115 | 25191 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 425 |
| `java.lang.invoke.LambdaForm$DMH/` | 18 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 443 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 3.4 | 213.1 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 2.5 | 13.9 | 19 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 1.3 | 13.01 | 10 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.3 | 7.01 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.0 | 59.83 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.6 | 2.04 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.2 | 0.86 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 283 | 0.2 | 0.84 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.2 | 1.16 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 145 | 0.2 | 1.28 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 145 | 145 | coincide |
| Mascotas | 145 | 145 | coincide |
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
| Registrar propietario | 145 | 0 | 0.0 % | 23 | 169 | 380 |
| Registrar mascota | 145 | 0 | 0.0 % | 15 | 39 | 174 |
| Agendar cita | 141 | 4 | 2.8 % | 19 | 71 | 351 |
| Recepción (llegada) | 61 | 0 | 0.0 % | 13 | 252 | 322 |
| Iniciar atención | 60 | 1 | 1.6 % | 15 | 43 | 54 |
| Leer la consulta | 60 | 0 | 0.0 % | 8 | 29 | 39 |
| Registrar diagnóstico | 60 | 0 | 0.0 % | 8 | 33 | 442 |
| Registrar receta | 60 | 0 | 0.0 % | 9 | 40 | 349 |
| Actualizar mascota | 60 | 0 | 0.0 % | 9 | 32 | 63 |
| Cerrar la consulta | 60 | 0 | 0.0 % | 17 | 67 | 438 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 11 | 316 | 376 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 8 | 40 | 291 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 9 | 39 | 49 |
| Cobrar | 35 | 0 | 0.0 % | 12 | 33 | 48 |
| Cancelar cita | 29 | 0 | 0.0 % | 14 | 165 | 393 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 8 | 0 (0 %) | 3 564 | 4 732 | 4 732 | 24 % |
| login | 50 | 0 (0 %) | 343 | 1 467 | 1 723 | 19 % |
| panel | 183 | 0 (0 %) | 51 | 101 | 418 | 9 % |
| apoderado | 145 | 0 (0 %) | 23 | 169 | 380 | 5 % |
| cita | 145 | 4 (3 %) | 19 | 71 | 351 | 4 % |
| lista_de_historias | 100 | 0 (0 %) | 26 | 234 | 390 | 4 % |
| propietarios | 100 | 0 (0 %) | 23 | 130 | 255 | 4 % |
| citas_del_mes | 100 | 0 (0 %) | 20 | 221 | 447 | 3 % |
| navegacion | 183 | 0 (0 %) | 8 | 48 | 335 | 3 % |
| mascota | 145 | 0 (0 %) | 15 | 39 | 174 | 3 % |
| historia | 100 | 0 (0 %) | 14 | 40 | 258 | 2 % |
| checkin | 61 | 0 (0 %) | 13 | 252 | 322 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 17 | 354 | 354 | 0 % |
| 0.1 | 16 | 94 | 380 | 1 241 | 1 % |
| 0.3 | 29 | 165 | 397 | 1 723 | 0 % |
| 0.6 | 43 | 227 | 323 | 348 | 1 % |
| 0.8 | 50 | 207 | 132 | 422 | 0 % |
| 1.1 | 50 | 165 | 91 | 444 | 0 % |
| 1.3 | 50 | 196 | 264 | 3 523 | 0 % |
| 1.6 | 50 | 224 | 50 | 57 | 0 % |
| 1.8 | 50 | 185 | 48 | 299 | 0 % |
| 2.0 | 50 | 221 | 91 | 335 | 0 % |
| 2.3 | 50 | 216 | 118 | 373 | 0 % |
| 2.5 | 50 | 177 | 65 | 376 | 0 % |
| 2.8 | 50 | 187 | 48 | 131 | 0 % |
| 3.0 | 50 | 217 | 67 | 291 | 0 % |
| 3.3 | 50 | 204 | 47 | 53 | 0 % |
| 3.5 | 47 | 69 | 47 | 54 | 0 % |
| 3.8 | 24 | 15 | 24 | 24 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
