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
| Peticiones hechas | 6 264 |
| Peticiones correctas (2xx) | 6 259 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.5 |
| Tiempo mediano (ms) | 15 |
| Tiempo p95 (ms) | 200 |
| Tiempo p99 (ms) | 375 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 40 |
| Tiempo con la CPU al tope (%) | 5 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 10 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 168 259 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 264 | 20.5 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 4 / 4 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2390; fallidas: 5.
- Acciones hechas: panel 290, agenda 238, historia 233, reportes 22, recepcion 313, atencion 158, cobro 93, cancelar 54; sin trabajo disponible: 254.
- CPU del backend: 40 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 24 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 313 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.6 %).
- GC: 528 pausas, 33.9 s en total (10 % del tiempo de vida de la JVM); 154 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 184 MB (tope -Xmx192m).
- Clases cargadas: 25414 en total, 179 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 33 | 34 | 8317 |
| 0.1 | 67 | 68 | 15027 |
| 0.2 | 74 | 75 | 16106 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 83 | 84 | 17755 |
| 1.1 | 111 | 114 | 24454 |
| 1.9 | 110 | 115 | 24811 |
| 2.4 | 111 | 115 | 25013 |
| 3.0 | 111 | 115 | 25256 |
| 3.6 | 111 | 115 | 25322 |
| 4.3 | 111 | 115 | 25347 |
| 5.0 | 111 | 115 | 25413 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 174 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 178 |
| `java.util.concurrent.atomic` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 390 | 2.8 | 7.12 | 14 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 390 | 2.6 | 6.54 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 44 | 2.3 | 52.5 | 12 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 238 | 1.8 | 7.5 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 233 | 1.7 | 7.35 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 628 | 1.5 | 2.33 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 628 | 0.6 | 0.97 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 44 | 0.5 | 11.23 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 390 | 0.5 | 1.22 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 233 | 0.4 | 1.63 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 313 | 313 | coincide |
| Mascotas | 313 | 313 | coincide |
| Citas agendadas | 310 | 310 | coincide |
| Diagnósticos | 156 | 156 | coincide |
| Recetas | 156 | 156 | coincide |
| Pagos | 93 | 93 | coincide |
| Citas completadas | 156 | 156 | coincide |
| Citas canceladas | 54 | 54 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 313 | 0 | 0.0 % | 25 | 189 | 252 |
| Registrar mascota | 313 | 0 | 0.0 % | 16 | 45 | 258 |
| Agendar cita | 310 | 3 | 1.0 % | 21 | 70 | 239 |
| Recepción (llegada) | 158 | 0 | 0.0 % | 12 | 182 | 296 |
| Iniciar atención | 156 | 2 | 1.3 % | 16 | 48 | 239 |
| Leer la consulta | 156 | 0 | 0.0 % | 11 | 32 | 288 |
| Registrar diagnóstico | 156 | 0 | 0.0 % | 10 | 42 | 238 |
| Registrar receta | 156 | 0 | 0.0 % | 11 | 32 | 52 |
| Actualizar mascota | 156 | 0 | 0.0 % | 10 | 31 | 225 |
| Cerrar la consulta | 156 | 0 | 0.0 % | 17 | 72 | 302 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 9 | 219 | 277 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 9 | 29 | 35 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 11 | 30 | 255 |
| Cobrar | 93 | 0 | 0.0 % | 12 | 52 | 231 |
| Cancelar cita | 54 | 0 | 0.0 % | 15 | 218 | 258 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 359 | 1 027 | 2 172 | 18 % |
| reporte_clinico | 22 | 0 (0 %) | 1 371 | 1 942 | 2 829 | 12 % |
| panel | 390 | 0 (0 %) | 43 | 233 | 337 | 10 % |
| apoderado | 313 | 0 (0 %) | 25 | 189 | 252 | 6 % |
| citas_del_mes | 238 | 0 (0 %) | 26 | 242 | 292 | 6 % |
| propietarios | 238 | 0 (0 %) | 34 | 215 | 293 | 5 % |
| lista_de_historias | 233 | 0 (0 %) | 19 | 238 | 277 | 5 % |
| cita | 313 | 3 (1 %) | 21 | 70 | 239 | 4 % |
| navegacion | 390 | 0 (0 %) | 8 | 148 | 263 | 4 % |
| mascota | 313 | 0 (0 %) | 16 | 45 | 258 | 3 % |
| historia | 233 | 0 (0 %) | 15 | 76 | 286 | 3 % |
| checkin | 158 | 0 (0 %) | 12 | 182 | 296 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 352 | 352 | 0 % |
| 0.1 | 16 | 90 | 354 | 409 | 0 % |
| 0.3 | 29 | 170 | 374 | 2 172 | 0 % |
| 0.6 | 43 | 232 | 326 | 378 | 0 % |
| 0.8 | 57 | 245 | 319 | 450 | 0 % |
| 1.1 | 70 | 295 | 322 | 838 | 0 % |
| 1.3 | 84 | 377 | 243 | 864 | 0 % |
| 1.6 | 98 | 431 | 70 | 370 | 0 % |
| 1.8 | 100 | 404 | 200 | 388 | 0 % |
| 2.0 | 100 | 443 | 42 | 111 | 0 % |
| 2.3 | 100 | 420 | 91 | 259 | 0 % |
| 2.5 | 100 | 358 | 215 | 300 | 0 % |
| 2.8 | 100 | 416 | 85 | 312 | 0 % |
| 3.0 | 100 | 384 | 146 | 291 | 0 % |
| 3.3 | 100 | 383 | 200 | 292 | 0 % |
| 3.5 | 100 | 381 | 126 | 259 | 0 % |
| 3.8 | 100 | 392 | 126 | 261 | 0 % |
| 4.0 | 100 | 428 | 125 | 257 | 0 % |
| 4.3 | 100 | 297 | 280 | 1 403 | 0 % |
| 4.5 | 77 | 104 | 218 | 286 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
