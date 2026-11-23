# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 1 |
| Peticiones hechas | 2 788 |
| Peticiones correctas (2xx) | 2 778 |
| Errores (%) | 0.4 % |
| Ritmo medio (peticiones/s) | 11.0 |
| Tiempo mediano (ms) | 17 |
| Tiempo p95 (ms) | 148 |
| Tiempo p99 (ms) | 495 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 4 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 487 |
| Sentencias SQL | 74 950 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 788 | 11.0 | no aplica (ramping-vus) | 0.4 % | 0 | 1 / 2 / 2 | 0 % | 84 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1012; fallidas: 10.
- Acciones hechas: panel 133, agenda 99, historia 99, reportes 8, recepcion 148, atencion 61, cobro 33, cancelar 28; sin trabajo disponible: 111.
- CPU del backend: 32 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 4 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 23 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 487 MB (95 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **1 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.3 s.
- Correo: el servidor de pruebas recibió 148 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 331 pausas, 20.2 s en total (8 % del tiempo de vida de la JVM); 61 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 244 MB (tope -Xmx256m).
- Clases cargadas: 25251 en total, 419 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1630 |
| 0.0 | 10 | 11 | 3591 |
| 0.0 | 14 | 14 | 4233 |
| 0.1 | 26 | 26 | 6798 |
| 0.1 | 53 | 54 | 12515 |
| 0.2 | 73 | 74 | 16048 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17217 |
| 0.9 | 109 | 111 | 23685 |
| 1.6 | 112 | 115 | 24599 |
| 2.1 | 110 | 115 | 24833 |
| 3.0 | 111 | 115 | 25021 |
| 3.6 | 111 | 115 | 25172 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 401 |
| `java.lang.invoke.LambdaForm$DMH/` | 15 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 416 |
| `org.springframework.jdbc.core` | 1 |
| `com.zaxxer.hikari.pool` | 1 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 4.2 | 260.4 | 24 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 3.3 | 18.23 | 19 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.7 | 17.56 | 10 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.7 | 9.4 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 1.1 | 66.25 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 282 | 0.8 | 2.88 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 282 | 0.3 | 1.16 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 282 | 0.3 | 1.12 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.3 | 1.59 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 148 | 0.2 | 1.47 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 148 | 148 | coincide |
| Mascotas | 148 | 148 | coincide |
| Citas agendadas | 138 | 138 | coincide |
| Diagnósticos | 61 | 61 | coincide |
| Recetas | 61 | 61 | coincide |
| Pagos | 33 | 33 | coincide |
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
| Registrar propietario | 148 | 0 | 0.0 % | 27 | 211 | 401 |
| Registrar mascota | 148 | 0 | 0.0 % | 19 | 89 | 451 |
| Agendar cita | 138 | 10 | 6.8 % | 22 | 122 | 500 |
| Recepción (llegada) | 61 | 0 | 0.0 % | 13 | 134 | 436 |
| Iniciar atención | 61 | 0 | 0.0 % | 16 | 55 | 64 |
| Leer la consulta | 61 | 0 | 0.0 % | 8 | 34 | 56 |
| Registrar diagnóstico | 61 | 0 | 0.0 % | 8 | 34 | 100 |
| Registrar receta | 61 | 0 | 0.0 % | 10 | 33 | 448 |
| Actualizar mascota | 61 | 0 | 0.0 % | 10 | 31 | 62 |
| Cerrar la consulta | 61 | 0 | 0.0 % | 19 | 48 | 341 |
| Caja: crear punto de cobro | 30 | 0 | 0.0 % | 10 | 162 | 197 |
| Caja: vincular equipo | 30 | 0 | 0.0 % | 9 | 34 | 58 |
| Caja: abrir la caja | 30 | 0 | 0.0 % | 10 | 60 | 64 |
| Cobrar | 33 | 0 | 0.0 % | 14 | 63 | 74 |
| Cancelar cita | 28 | 0 | 0.0 % | 16 | 46 | 371 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 456 | 2 156 | 2 530 | 22 % |
| reporte_clinico | 8 | 0 (0 %) | 3 874 | 6 007 | 6 007 | 21 % |
| panel | 183 | 0 (0 %) | 66 | 167 | 530 | 10 % |
| apoderado | 148 | 0 (0 %) | 27 | 211 | 401 | 5 % |
| cita | 148 | 10 (7 %) | 22 | 122 | 500 | 4 % |
| citas_del_mes | 99 | 0 (0 %) | 24 | 330 | 485 | 4 % |
| propietarios | 99 | 0 (0 %) | 31 | 159 | 504 | 4 % |
| mascota | 148 | 0 (0 %) | 19 | 89 | 451 | 3 % |
| lista_de_historias | 99 | 0 (0 %) | 32 | 183 | 481 | 3 % |
| navegacion | 183 | 0 (0 %) | 9 | 50 | 272 | 2 % |
| cumplimiento | 183 | 0 (0 %) | 7 | 29 | 56 | 2 % |
| reporte_de_ventas | 8 | 0 (0 %) | 328 | 783 | 783 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 12 | 484 | 484 | 0 % |
| 0.0 | 15 | 92 | 440 | 994 | 1 % |
| 0.3 | 29 | 157 | 1 295 | 2 530 | 1 % |
| 0.5 | 42 | 227 | 414 | 476 | 0 % |
| 0.8 | 50 | 216 | 110 | 494 | 0 % |
| 1.0 | 50 | 164 | 174 | 475 | 1 % |
| 1.3 | 50 | 185 | 285 | 3 731 | 1 % |
| 1.5 | 50 | 226 | 63 | 94 | 0 % |
| 1.8 | 50 | 184 | 48 | 74 | 0 % |
| 2.0 | 50 | 200 | 162 | 500 | 0 % |
| 2.3 | 50 | 221 | 274 | 451 | 0 % |
| 2.5 | 50 | 182 | 142 | 504 | 0 % |
| 2.8 | 50 | 203 | 59 | 74 | 0 % |
| 3.0 | 50 | 215 | 96 | 274 | 1 % |
| 3.3 | 50 | 201 | 59 | 69 | 0 % |
| 3.5 | 47 | 87 | 34 | 60 | 0 % |
| 3.8 | 25 | 16 | 29 | 29 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
