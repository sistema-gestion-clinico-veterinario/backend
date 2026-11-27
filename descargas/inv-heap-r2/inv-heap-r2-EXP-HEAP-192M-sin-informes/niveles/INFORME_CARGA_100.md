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
| Peticiones hechas | 6 366 |
| Peticiones correctas (2xx) | 6 354 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.8 |
| Tiempo mediano (ms) | 11 |
| Tiempo p95 (ms) | 56 |
| Tiempo p99 (ms) | 342 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 10 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 172 622 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 366 | 20.8 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 3 / 4 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2423; fallidas: 12.
- Acciones hechas: panel 296, agenda 252, historia 233, reportes 0, recepcion 320, atencion 159, cobro 96, cancelar 55; sin trabajo disponible: 256.
- CPU del backend: 30 % de media y 74 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 29 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.7 con unos 58 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.3 s.
- Correo: el servidor de pruebas recibió 320 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.3 %).
- GC: 389 pausas, 6.5 s en total (2 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 184 MB (tope -Xmx192m).
- Clases cargadas: 25202 en total, 174 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2043 |
| 0.0 | 11 | 11 | 3772 |
| 0.0 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12294 |
| 0.1 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16116 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 83 | 84 | 17756 |
| 1.0 | 111 | 113 | 24092 |
| 1.9 | 110 | 114 | 24581 |
| 2.5 | 110 | 114 | 24811 |
| 3.2 | 111 | 114 | 25078 |
| 4.0 | 111 | 114 | 25127 |
| 4.8 | 111 | 114 | 25201 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 170 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 174 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 396 | 5.5 | 13.92 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 396 | 3.5 | 8.8 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 233 | 3.0 | 13.04 | 13 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 257 | 2.9 | 11.38 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 648 | 1.5 | 2.34 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 648 | 0.6 | 0.99 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 396 | 0.5 | 1.24 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 233 | 0.4 | 1.56 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 252 | 0.3 | 1.31 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 320 | 0.3 | 1.0 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 320 | 320 | coincide |
| Mascotas | 320 | 320 | coincide |
| Citas agendadas | 309 | 309 | coincide |
| Diagnósticos | 158 | 158 | coincide |
| Recetas | 158 | 158 | coincide |
| Pagos | 96 | 96 | coincide |
| Citas completadas | 158 | 158 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 320 | 0 | 0.0 % | 20 | 52 | 70 |
| Registrar mascota | 320 | 0 | 0.0 % | 13 | 33 | 43 |
| Agendar cita | 309 | 11 | 3.4 % | 17 | 47 | 78 |
| Recepción (llegada) | 159 | 0 | 0.0 % | 7 | 25 | 31 |
| Iniciar atención | 158 | 1 | 0.6 % | 11 | 37 | 41 |
| Leer la consulta | 158 | 0 | 0.0 % | 6 | 20 | 29 |
| Registrar diagnóstico | 158 | 0 | 0.0 % | 6 | 21 | 29 |
| Registrar receta | 158 | 0 | 0.0 % | 7 | 22 | 31 |
| Actualizar mascota | 158 | 0 | 0.0 % | 7 | 25 | 31 |
| Cerrar la consulta | 158 | 0 | 0.0 % | 12 | 42 | 49 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 6 | 21 | 31 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 6 | 18 | 43 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 7 | 22 | 37 |
| Cobrar | 96 | 0 | 0.0 % | 10 | 27 | 34 |
| Cancelar cita | 55 | 0 | 0.0 % | 11 | 34 | 37 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 350 | 515 | 1 201 | 26 % |
| panel | 396 | 0 (0 %) | 51 | 87 | 117 | 15 % |
| propietarios | 252 | 0 (0 %) | 24 | 83 | 133 | 6 % |
| apoderado | 320 | 0 (0 %) | 20 | 52 | 70 | 6 % |
| cita | 320 | 11 (3 %) | 17 | 47 | 78 | 5 % |
| citas_del_mes | 252 | 0 (0 %) | 28 | 49 | 64 | 5 % |
| lista_de_historias | 233 | 0 (0 %) | 22 | 44 | 65 | 4 % |
| mascota | 320 | 0 (0 %) | 13 | 33 | 43 | 4 % |
| historia | 233 | 0 (0 %) | 11 | 36 | 42 | 3 % |
| navegacion | 396 | 0 (0 %) | 6 | 19 | 28 | 2 % |
| cumplimiento | 396 | 0 (0 %) | 5 | 22 | 27 | 2 % |
| empleados | 252 | 0 (0 %) | 8 | 28 | 38 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 344 | 382 | 0 % |
| 0.1 | 21 | 129 | 344 | 600 | 0 % |
| 0.4 | 35 | 170 | 327 | 387 | 1 % |
| 0.7 | 49 | 252 | 319 | 363 | 0 % |
| 0.9 | 62 | 278 | 136 | 391 | 0 % |
| 1.1 | 76 | 327 | 189 | 409 | 0 % |
| 1.4 | 89 | 390 | 81 | 472 | 0 % |
| 1.6 | 100 | 437 | 63 | 368 | 0 % |
| 1.9 | 100 | 426 | 52 | 73 | 0 % |
| 2.1 | 100 | 429 | 44 | 58 | 0 % |
| 2.4 | 100 | 405 | 48 | 85 | 0 % |
| 2.6 | 100 | 408 | 44 | 62 | 0 % |
| 2.9 | 100 | 436 | 44 | 61 | 0 % |
| 3.1 | 100 | 359 | 47 | 80 | 0 % |
| 3.4 | 100 | 395 | 45 | 70 | 0 % |
| 3.6 | 100 | 366 | 50 | 70 | 0 % |
| 3.9 | 100 | 447 | 50 | 80 | 0 % |
| 4.2 | 100 | 391 | 51 | 73 | 0 % |
| 4.4 | 97 | 206 | 45 | 61 | 0 % |
| 4.7 | 58 | 59 | 56 | 62 | 5 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
