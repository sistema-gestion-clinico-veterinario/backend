# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 254 |
| Peticiones correctas (2xx) | 6 238 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 20.6 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 120 |
| Tiempo p99 (ms) | 388 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 38 |
| Tiempo con la CPU al tope (%) | 5 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 9 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 168 056 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 254 | 20.6 | no aplica (ramping-vus) | 0.3 % | 0 | 1 / 4 / 4 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2362; fallidas: 16.
- Acciones hechas: panel 291, agenda 238, historia 233, reportes 22, recepcion 315, atencion 155, cobro 92, cancelar 56; sin trabajo disponible: 254.
- CPU del backend: 38 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 9 % de media y 22 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 315 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.6 %).
- GC: 491 pausas, 27.4 s en total (8 % del tiempo de vida de la JVM); 115 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 184 MB (tope -Xmx192m).
- Clases cargadas: 25419 en total, 179 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8294 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 85 | 86 | 18044 |
| 1.2 | 111 | 114 | 24466 |
| 2.0 | 110 | 114 | 24816 |
| 2.5 | 111 | 114 | 25034 |
| 3.1 | 111 | 114 | 25278 |
| 3.6 | 111 | 114 | 25327 |
| 4.3 | 111 | 114 | 25351 |
| 5.0 | 111 | 114 | 25418 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 174 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 178 |
| `veterinaria.vargasvet.service.reportes` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 391 | 2.9 | 7.4 | 14 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 391 | 2.6 | 6.57 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 2.3 | 53.4 | 12 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 233 | 1.7 | 7.32 | 8 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 238 | 1.6 | 6.69 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 629 | 1.5 | 2.34 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 629 | 0.6 | 0.96 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 391 | 0.5 | 1.26 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 233 | 0.4 | 1.75 | 2 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 32 | 0.4 | 12.37 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 315 | 315 | coincide |
| Mascotas | 315 | 315 | coincide |
| Citas agendadas | 301 | 301 | coincide |
| Diagnósticos | 153 | 153 | coincide |
| Recetas | 153 | 153 | coincide |
| Pagos | 92 | 92 | coincide |
| Citas completadas | 153 | 153 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 315 | 0 | 0.0 % | 24 | 195 | 290 |
| Registrar mascota | 315 | 0 | 0.0 % | 15 | 52 | 244 |
| Agendar cita | 301 | 14 | 4.4 % | 19 | 84 | 337 |
| Recepción (llegada) | 155 | 0 | 0.0 % | 11 | 161 | 275 |
| Iniciar atención | 153 | 2 | 1.3 % | 13 | 57 | 259 |
| Leer la consulta | 153 | 0 | 0.0 % | 8 | 30 | 51 |
| Registrar diagnóstico | 153 | 0 | 0.0 % | 7 | 29 | 46 |
| Registrar receta | 153 | 0 | 0.0 % | 8 | 30 | 48 |
| Actualizar mascota | 153 | 0 | 0.0 % | 9 | 31 | 323 |
| Cerrar la consulta | 153 | 0 | 0.0 % | 14 | 80 | 291 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 8 | 232 | 309 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 7 | 23 | 35 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 9 | 28 | 44 |
| Cobrar | 92 | 0 | 0.0 % | 12 | 41 | 313 |
| Cancelar cita | 56 | 0 | 0.0 % | 13 | 175 | 268 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 353 | 1 026 | 1 879 | 19 % |
| reporte_clinico | 22 | 0 (0 %) | 1 300 | 2 057 | 2 077 | 13 % |
| panel | 391 | 0 (0 %) | 43 | 284 | 390 | 11 % |
| propietarios | 238 | 0 (0 %) | 25 | 339 | 394 | 6 % |
| apoderado | 315 | 0 (0 %) | 24 | 195 | 290 | 6 % |
| cita | 315 | 14 (4 %) | 19 | 84 | 337 | 5 % |
| citas_del_mes | 238 | 0 (0 %) | 24 | 194 | 320 | 4 % |
| lista_de_historias | 233 | 0 (0 %) | 20 | 166 | 286 | 4 % |
| mascota | 315 | 0 (0 %) | 15 | 52 | 244 | 3 % |
| navegacion | 391 | 0 (0 %) | 7 | 88 | 251 | 3 % |
| historia | 233 | 0 (0 %) | 13 | 45 | 299 | 2 % |
| cumplimiento | 391 | 0 (0 %) | 6 | 27 | 265 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 345 | 553 | 0 % |
| 0.2 | 27 | 143 | 408 | 1 548 | 0 % |
| 0.5 | 40 | 217 | 324 | 352 | 0 % |
| 0.8 | 54 | 247 | 319 | 370 | 0 % |
| 1.0 | 68 | 278 | 323 | 920 | 0 % |
| 1.2 | 81 | 347 | 320 | 1 026 | 0 % |
| 1.5 | 95 | 423 | 82 | 455 | 0 % |
| 1.8 | 100 | 426 | 265 | 405 | 0 % |
| 2.0 | 100 | 414 | 49 | 232 | 0 % |
| 2.2 | 100 | 409 | 127 | 399 | 0 % |
| 2.5 | 100 | 393 | 88 | 360 | 0 % |
| 2.8 | 100 | 418 | 271 | 394 | 0 % |
| 3.0 | 100 | 402 | 80 | 307 | 0 % |
| 3.2 | 100 | 375 | 43 | 257 | 0 % |
| 3.5 | 100 | 379 | 37 | 60 | 0 % |
| 3.8 | 100 | 400 | 54 | 290 | 1 % |
| 4.0 | 100 | 385 | 39 | 253 | 0 % |
| 4.2 | 100 | 358 | 209 | 356 | 1 % |
| 4.5 | 86 | 146 | 215 | 1 004 | 2 % |
| 4.8 | 39 | 3 | 25 | 25 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
