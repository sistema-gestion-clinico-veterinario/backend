# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

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
| Veredicto | FALLA |
| Usuarios sin degradación | 49 |
| Peticiones hechas | 3 947 |
| Peticiones correctas (2xx) | 3 588 |
| Errores (%) | 9.1 % |
| Ritmo medio (peticiones/s) | 12.9 |
| Tiempo mediano (ms) | 21 |
| Tiempo p95 (ms) | 453 |
| Tiempo p99 (ms) | 1 798 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 47 |
| Tiempo con la CPU al tope (%) | 18 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 489 |
| Sentencias SQL | 101 453 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 947 | 12.9 | no aplica (ramping-vus) | 9.1 % | 0 | 1 / 2 / 4 | 0 % | 121 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1336; fallidas: 1.
- Acciones hechas: panel 175, agenda 142, historia 130, reportes 12, recepcion 176, atencion 88, cobro 54, cancelar 33; sin trabajo disponible: 138.
- CPU del backend: 47 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 18 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 19 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 489 MB (95 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.9 con unos 62 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **49 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.9 s.
- Correo: el servidor de pruebas recibió 176 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.6 %).
- GC: 400 pausas, 30.6 s en total (9 % del tiempo de vida de la JVM); 93 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 238 MB (tope -Xmx256m).
- Clases cargadas: 25350 en total, 312 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2041 |
| 0.0 | 11 | 11 | 3772 |
| 0.0 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12300 |
| 0.1 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16123 |
| 0.3 | 79 | 80 | 17214 |
| 0.7 | 109 | 111 | 23558 |
| 1.6 | 110 | 115 | 24661 |
| 2.2 | 110 | 115 | 24878 |
| 2.9 | 111 | 115 | 25099 |
| 3.5 | 111 | 115 | 25192 |
| 4.7 | 111 | 115 | 25318 |
| 5.1 | 111 | 115 | 25349 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 304 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 312 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 24 | 3.9 | 162.41 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 225 | 2.3 | 10.28 | 15 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 225 | 1.5 | 6.57 | 9 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.4 | 10.76 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 24 | 0.9 | 38.85 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 367 | 0.7 | 1.9 | 4 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 658 | 0.3 | 0.49 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 367 | 0.3 | 0.87 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 367 | 0.3 | 0.76 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 608 | 0.2 | 0.4 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 176 | 176 | coincide |
| Mascotas | 176 | 176 | coincide |
| Citas agendadas | 176 | 176 | coincide |
| Diagnósticos | 87 | 87 | coincide |
| Recetas | 87 | 87 | coincide |
| Pagos | 54 | 54 | coincide |
| Citas completadas | 87 | 87 | coincide |
| Citas canceladas | 33 | 33 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 176 | 0 | 0.0 % | 35 | 280 | 506 |
| Registrar mascota | 176 | 0 | 0.0 % | 23 | 105 | 436 |
| Agendar cita | 176 | 0 | 0.0 % | 31 | 140 | 507 |
| Recepción (llegada) | 88 | 0 | 0.0 % | 15 | 227 | 358 |
| Iniciar atención | 87 | 1 | 1.1 % | 18 | 112 | 168 |
| Leer la consulta | 87 | 0 | 0.0 % | 11 | 48 | 97 |
| Registrar diagnóstico | 87 | 0 | 0.0 % | 11 | 70 | 104 |
| Registrar receta | 87 | 0 | 0.0 % | 11 | 82 | 551 |
| Actualizar mascota | 87 | 0 | 0.0 % | 9 | 78 | 465 |
| Cerrar la consulta | 87 | 0 | 0.0 % | 17 | 113 | 237 |
| Caja: crear punto de cobro | 37 | 0 | 0.0 % | 19 | 421 | 445 |
| Caja: vincular equipo | 37 | 0 | 0.0 % | 12 | 79 | 89 |
| Caja: abrir la caja | 37 | 0 | 0.0 % | 16 | 69 | 86 |
| Cobrar | 54 | 0 | 0.0 % | 20 | 108 | 359 |
| Cancelar cita | 33 | 0 | 0.0 % | 25 | 313 | 337 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 408 | 358 (88 %) | 312 | 2 039 | 2 731 | 53 % |
| reporte_clinico | 12 | 0 (0 %) | 3 989 | 6 742 | 6 742 | 13 % |
| panel | 225 | 0 (0 %) | 56 | 201 | 586 | 5 % |
| apoderado | 176 | 0 (0 %) | 35 | 280 | 506 | 3 % |
| propietarios | 142 | 0 (0 %) | 35 | 255 | 572 | 3 % |
| cita | 176 | 0 (0 %) | 31 | 140 | 507 | 2 % |
| citas_del_mes | 142 | 0 (0 %) | 23 | 357 | 478 | 2 % |
| lista_de_historias | 130 | 0 (0 %) | 33 | 256 | 397 | 2 % |
| navegacion | 225 | 0 (0 %) | 10 | 131 | 429 | 2 % |
| mascota | 176 | 0 (0 %) | 23 | 105 | 436 | 2 % |
| empleados | 142 | 0 (0 %) | 13 | 87 | 477 | 1 % |
| historia | 130 | 0 (0 %) | 26 | 71 | 156 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 290 | 311 | 0 % |
| 0.1 | 21 | 123 | 1 189 | 1 854 | 0 % |
| 0.4 | 35 | 168 | 311 | 521 | 0 % |
| 0.7 | 49 | 253 | 252 | 316 | 0 % |
| 0.9 | 62 | 196 | 289 | 1 400 | 6 % |
| 1.1 | 76 | 185 | 337 | 1 150 | 9 % |
| 1.4 | 89 | 225 | 1 719 | 2 811 | 10 % |
| 1.6 | 100 | 242 | 472 | 780 | 12 % |
| 1.9 | 100 | 199 | 475 | 1 734 | 12 % |
| 2.1 | 100 | 247 | 631 | 1 491 | 14 % |
| 2.4 | 100 | 233 | 1 853 | 2 676 | 9 % |
| 2.6 | 100 | 234 | 1 108 | 2 101 | 13 % |
| 2.9 | 100 | 225 | 750 | 2 039 | 12 % |
| 3.1 | 100 | 218 | 292 | 490 | 11 % |
| 3.4 | 100 | 228 | 245 | 473 | 12 % |
| 3.6 | 100 | 219 | 44 | 272 | 13 % |
| 3.9 | 100 | 257 | 36 | 47 | 10 % |
| 4.2 | 100 | 223 | 307 | 583 | 12 % |
| 4.4 | 99 | 162 | 308 | 3 154 | 4 % |
| 4.7 | 57 | 54 | 371 | 3 692 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
