# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 50 |
| Peticiones hechas | 2 773 |
| Peticiones correctas (2xx) | 2 772 |
| Errores (%) | 0.0 % |
| Ritmo medio (peticiones/s) | 11.1 |
| Tiempo mediano (ms) | 15 |
| Tiempo p95 (ms) | 73 |
| Tiempo p99 (ms) | 475 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 26 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 74 051 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 773 | 11.1 | no aplica (ramping-vus) | 0.0 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1022; fallidas: 1.
- Acciones hechas: panel 133, agenda 100, historia 100, reportes 8, recepcion 144, atencion 62, cobro 35, cancelar 29; sin trabajo disponible: 108.
- CPU del backend: 26 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 17 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 144 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 338 pausas, 12.4 s en total (5 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25257 en total, 473 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1744 |
| 0.0 | 11 | 11 | 3662 |
| 0.0 | 16 | 16 | 4591 |
| 0.1 | 32 | 32 | 7992 |
| 0.1 | 60 | 61 | 13639 |
| 0.2 | 74 | 74 | 16077 |
| 0.2 | 74 | 75 | 16115 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17215 |
| 0.6 | 105 | 107 | 22653 |
| 1.4 | 111 | 114 | 24487 |
| 2.2 | 110 | 114 | 24815 |
| 3.1 | 111 | 114 | 25120 |
| 4.2 | 111 | 114 | 25257 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 456 |
| `java.lang.invoke.LambdaForm$DMH/` | 16 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 472 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 183 | 1.6 | 9.0 | 16 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 183 | 1.5 | 8.09 | 14 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 16 | 1.1 | 66.88 | 10 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 100 | 0.9 | 8.72 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 283 | 0.8 | 2.8 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 100 | 0.7 | 7.08 | 7 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 16 | 0.5 | 30.16 | 5 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 283 | 0.3 | 1.21 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 183 | 0.3 | 1.61 | 3 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 100 | 0.2 | 1.96 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 144 | 144 | coincide |
| Mascotas | 144 | 144 | coincide |
| Citas agendadas | 143 | 143 | coincide |
| Diagnósticos | 62 | 62 | coincide |
| Recetas | 62 | 62 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 62 | 62 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 144 | 0 | 0.0 % | 26 | 62 | 204 |
| Registrar mascota | 144 | 0 | 0.0 % | 19 | 41 | 106 |
| Agendar cita | 143 | 1 | 0.7 % | 21 | 58 | 324 |
| Recepción (llegada) | 62 | 0 | 0.0 % | 13 | 32 | 40 |
| Iniciar atención | 62 | 0 | 0.0 % | 15 | 42 | 376 |
| Leer la consulta | 62 | 0 | 0.0 % | 8 | 26 | 28 |
| Registrar diagnóstico | 62 | 0 | 0.0 % | 9 | 26 | 43 |
| Registrar receta | 62 | 0 | 0.0 % | 9 | 28 | 65 |
| Actualizar mascota | 62 | 0 | 0.0 % | 11 | 29 | 42 |
| Cerrar la consulta | 62 | 0 | 0.0 % | 18 | 48 | 234 |
| Caja: crear punto de cobro | 31 | 0 | 0.0 % | 10 | 31 | 34 |
| Caja: vincular equipo | 31 | 0 | 0.0 % | 9 | 34 | 268 |
| Caja: abrir la caja | 31 | 0 | 0.0 % | 9 | 56 | 58 |
| Cobrar | 35 | 0 | 0.0 % | 14 | 48 | 51 |
| Cancelar cita | 29 | 0 | 0.0 % | 15 | 49 | 51 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 453 | 1 361 | 2 050 | 28 % |
| panel | 183 | 0 (0 %) | 52 | 92 | 335 | 11 % |
| reporte_clinico | 8 | 0 (0 %) | 1 359 | 2 906 | 2 906 | 11 % |
| cita | 144 | 1 (1 %) | 21 | 58 | 324 | 5 % |
| apoderado | 144 | 0 (0 %) | 26 | 62 | 204 | 5 % |
| propietarios | 100 | 0 (0 %) | 28 | 101 | 268 | 4 % |
| citas_del_mes | 100 | 0 (0 %) | 30 | 69 | 262 | 3 % |
| mascota | 144 | 0 (0 %) | 19 | 41 | 106 | 3 % |
| lista_de_historias | 100 | 0 (0 %) | 23 | 107 | 282 | 3 % |
| navegacion | 183 | 0 (0 %) | 9 | 24 | 176 | 3 % |
| historia | 100 | 0 (0 %) | 15 | 38 | 85 | 2 % |
| cumplimiento | 183 | 0 (0 %) | 7 | 24 | 34 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 7 | 46 | 454 | 711 | 0 % |
| 0.1 | 20 | 121 | 582 | 2 050 | 1 % |
| 0.4 | 34 | 159 | 466 | 549 | 0 % |
| 0.6 | 48 | 240 | 431 | 511 | 0 % |
| 0.9 | 50 | 197 | 88 | 570 | 0 % |
| 1.1 | 50 | 169 | 63 | 317 | 0 % |
| 1.4 | 50 | 186 | 56 | 338 | 0 % |
| 1.6 | 50 | 221 | 58 | 74 | 0 % |
| 1.9 | 50 | 188 | 47 | 268 | 0 % |
| 2.1 | 50 | 224 | 46 | 67 | 0 % |
| 2.4 | 50 | 205 | 96 | 338 | 0 % |
| 2.6 | 50 | 188 | 36 | 58 | 0 % |
| 2.9 | 50 | 220 | 51 | 74 | 0 % |
| 3.1 | 50 | 187 | 48 | 79 | 0 % |
| 3.4 | 50 | 167 | 58 | 93 | 0 % |
| 3.6 | 42 | 55 | 39 | 47 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
