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
| Peticiones hechas | 6 359 |
| Peticiones correctas (2xx) | 6 347 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.7 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 59 |
| Tiempo p99 (ms) | 345 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 10 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 173 274 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 359 | 20.7 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 4 / 4 | 0 % | 135 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2416; fallidas: 12.
- Acciones hechas: panel 296, agenda 252, historia 233, reportes 0, recepcion 319, atencion 159, cobro 97, cancelar 55; sin trabajo disponible: 257.
- CPU del backend: 30 % de media y 81 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 22 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 319 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.2 %).
- GC: 386 pausas, 6.5 s en total (2 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 178 MB (tope -Xmx192m).
- Clases cargadas: 25198 en total, 174 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2043 |
| 0.0 | 11 | 11 | 3772 |
| 0.0 | 21 | 22 | 5802 |
| 0.1 | 53 | 53 | 12294 |
| 0.1 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16117 |
| 0.3 | 79 | 80 | 17107 |
| 0.3 | 83 | 85 | 17859 |
| 1.1 | 111 | 113 | 24209 |
| 1.9 | 110 | 114 | 24617 |
| 2.5 | 110 | 114 | 24879 |
| 3.3 | 111 | 114 | 25075 |
| 4.1 | 111 | 114 | 25123 |
| 4.9 | 111 | 114 | 25197 |

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
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 396 | 5.9 | 14.94 | 27 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 396 | 3.7 | 9.44 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 233 | 3.2 | 13.75 | 15 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 648 | 1.5 | 2.32 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 648 | 0.6 | 0.99 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 648 | 0.6 | 0.97 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 396 | 0.5 | 1.25 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 319 | 0.4 | 1.24 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 233 | 0.4 | 1.69 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 252 | 0.4 | 1.56 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 319 | 319 | coincide |
| Mascotas | 319 | 319 | coincide |
| Citas agendadas | 309 | 309 | coincide |
| Diagnósticos | 157 | 157 | coincide |
| Recetas | 157 | 157 | coincide |
| Pagos | 97 | 97 | coincide |
| Citas completadas | 157 | 157 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 319 | 0 | 0.0 % | 23 | 56 | 63 |
| Registrar mascota | 319 | 0 | 0.0 % | 15 | 36 | 42 |
| Agendar cita | 309 | 10 | 3.1 % | 19 | 53 | 73 |
| Recepción (llegada) | 159 | 0 | 0.0 % | 11 | 26 | 30 |
| Iniciar atención | 157 | 2 | 1.3 % | 12 | 37 | 49 |
| Leer la consulta | 157 | 0 | 0.0 % | 7 | 23 | 30 |
| Registrar diagnóstico | 157 | 0 | 0.0 % | 7 | 25 | 32 |
| Registrar receta | 157 | 0 | 0.0 % | 8 | 22 | 35 |
| Actualizar mascota | 157 | 0 | 0.0 % | 8 | 26 | 38 |
| Cerrar la consulta | 157 | 0 | 0.0 % | 13 | 43 | 83 |
| Caja: crear punto de cobro | 72 | 0 | 0.0 % | 9 | 23 | 254 |
| Caja: vincular equipo | 72 | 0 | 0.0 % | 7 | 24 | 291 |
| Caja: abrir la caja | 72 | 0 | 0.0 % | 9 | 27 | 43 |
| Cobrar | 97 | 0 | 0.0 % | 11 | 33 | 48 |
| Cancelar cita | 55 | 0 | 0.0 % | 14 | 35 | 37 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 354 | 463 | 605 | 24 % |
| panel | 396 | 0 (0 %) | 55 | 98 | 136 | 16 % |
| apoderado | 319 | 0 (0 %) | 23 | 56 | 63 | 6 % |
| propietarios | 252 | 0 (0 %) | 23 | 85 | 125 | 6 % |
| cita | 319 | 10 (3 %) | 19 | 53 | 73 | 6 % |
| lista_de_historias | 233 | 0 (0 %) | 25 | 48 | 73 | 5 % |
| citas_del_mes | 252 | 0 (0 %) | 20 | 41 | 62 | 4 % |
| mascota | 319 | 0 (0 %) | 15 | 36 | 42 | 4 % |
| historia | 233 | 0 (0 %) | 13 | 39 | 52 | 3 % |
| navegacion | 396 | 0 (0 %) | 7 | 19 | 34 | 3 % |
| cumplimiento | 396 | 0 (0 %) | 5 | 23 | 28 | 2 % |
| privacidad | 396 | 0 (0 %) | 6 | 16 | 22 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 351 | 384 | 0 % |
| 0.1 | 21 | 131 | 338 | 409 | 0 % |
| 0.4 | 35 | 168 | 324 | 372 | 0 % |
| 0.7 | 49 | 245 | 319 | 358 | 0 % |
| 0.9 | 62 | 280 | 287 | 408 | 0 % |
| 1.1 | 76 | 332 | 101 | 421 | 0 % |
| 1.4 | 89 | 387 | 95 | 446 | 0 % |
| 1.6 | 100 | 437 | 62 | 373 | 0 % |
| 1.9 | 100 | 416 | 50 | 76 | 0 % |
| 2.1 | 100 | 431 | 46 | 62 | 0 % |
| 2.4 | 100 | 416 | 59 | 131 | 0 % |
| 2.6 | 100 | 401 | 52 | 81 | 0 % |
| 2.9 | 100 | 431 | 47 | 61 | 0 % |
| 3.1 | 100 | 351 | 48 | 66 | 1 % |
| 3.4 | 100 | 393 | 44 | 68 | 0 % |
| 3.6 | 100 | 378 | 49 | 100 | 0 % |
| 3.9 | 100 | 451 | 55 | 72 | 0 % |
| 4.2 | 100 | 384 | 56 | 102 | 1 % |
| 4.4 | 98 | 212 | 54 | 60 | 0 % |
| 4.7 | 58 | 59 | 57 | 62 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
