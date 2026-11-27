# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Usuarios sin degradación | 50 |
| Peticiones hechas | 2 810 |
| Peticiones correctas (2xx) | 2 803 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 11.3 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 55 |
| Tiempo p99 (ms) | 343 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 20 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 426 |
| Sentencias SQL | 75 821 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 810 | 11.3 | no aplica (ramping-vus) | 0.2 % | 0 | 1 / 2 / 3 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1031; fallidas: 7.
- Acciones hechas: panel 135, agenda 105, historia 99, reportes 0, recepcion 143, atencion 63, cobro 39, cancelar 30; sin trabajo disponible: 108.
- CPU del backend: 20 % de media y 58 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 15 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 426 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 143 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 309 pausas, 6.6 s en total (3 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25033 en total, 438 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1574 |
| 0.0 | 10 | 10 | 3447 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 22 | 23 | 5987 |
| 0.1 | 46 | 47 | 11142 |
| 0.1 | 67 | 68 | 15027 |
| 0.2 | 74 | 75 | 16103 |
| 0.2 | 74 | 75 | 16117 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17216 |
| 0.4 | 102 | 103 | 21607 |
| 1.2 | 111 | 114 | 24225 |
| 2.2 | 110 | 114 | 24653 |
| 3.4 | 110 | 114 | 24953 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 423 |
| `java.lang.invoke.LambdaForm$DMH/` | 14 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 437 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 185 | 2.6 | 13.8 | 28 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 185 | 1.4 | 7.36 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.3 | 13.11 | 14 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 290 | 0.6 | 2.06 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 290 | 0.3 | 0.95 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 290 | 0.3 | 0.88 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 185 | 0.2 | 1.23 | 3 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 143 | 0.2 | 1.32 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 105 | 0.2 | 1.52 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 99 | 0.2 | 1.56 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 143 | 143 | coincide |
| Mascotas | 143 | 143 | coincide |
| Citas agendadas | 136 | 136 | coincide |
| Diagnósticos | 63 | 63 | coincide |
| Recetas | 63 | 63 | coincide |
| Pagos | 39 | 39 | coincide |
| Citas completadas | 63 | 63 | coincide |
| Citas canceladas | 30 | 30 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 143 | 0 | 0.0 % | 23 | 53 | 94 |
| Registrar mascota | 143 | 0 | 0.0 % | 15 | 32 | 261 |
| Agendar cita | 136 | 7 | 4.9 % | 18 | 44 | 118 |
| Recepción (llegada) | 63 | 0 | 0.0 % | 11 | 26 | 222 |
| Iniciar atención | 63 | 0 | 0.0 % | 13 | 32 | 74 |
| Leer la consulta | 63 | 0 | 0.0 % | 7 | 15 | 39 |
| Registrar diagnóstico | 63 | 0 | 0.0 % | 7 | 15 | 32 |
| Registrar receta | 63 | 0 | 0.0 % | 8 | 24 | 39 |
| Actualizar mascota | 63 | 0 | 0.0 % | 9 | 24 | 31 |
| Cerrar la consulta | 63 | 0 | 0.0 % | 15 | 31 | 93 |
| Caja: crear punto de cobro | 33 | 0 | 0.0 % | 9 | 24 | 27 |
| Caja: vincular equipo | 33 | 0 | 0.0 % | 7 | 19 | 21 |
| Caja: abrir la caja | 33 | 0 | 0.0 % | 8 | 18 | 20 |
| Cobrar | 39 | 0 | 0.0 % | 11 | 19 | 22 |
| Cancelar cita | 30 | 0 | 0.0 % | 14 | 30 | 37 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 344 | 426 | 1 211 | 27 % |
| panel | 185 | 0 (0 %) | 50 | 72 | 123 | 15 % |
| apoderado | 143 | 0 (0 %) | 23 | 53 | 94 | 6 % |
| lista_de_historias | 99 | 0 (0 %) | 26 | 43 | 425 | 5 % |
| cita | 143 | 7 (5 %) | 18 | 44 | 118 | 5 % |
| propietarios | 105 | 0 (0 %) | 22 | 59 | 126 | 5 % |
| mascota | 143 | 0 (0 %) | 15 | 32 | 261 | 5 % |
| citas_del_mes | 105 | 0 (0 %) | 20 | 38 | 56 | 4 % |
| navegacion | 185 | 0 (0 %) | 8 | 16 | 45 | 3 % |
| historia | 99 | 0 (0 %) | 14 | 40 | 135 | 3 % |
| avisos_de_cumplimiento | 185 | 0 (0 %) | 5 | 16 | 263 | 3 % |
| cumplimiento | 185 | 0 (0 %) | 5 | 16 | 27 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 7 | 49 | 349 | 391 | 0 % |
| 0.1 | 20 | 128 | 351 | 624 | 0 % |
| 0.4 | 34 | 163 | 328 | 396 | 0 % |
| 0.6 | 48 | 244 | 326 | 371 | 0 % |
| 0.9 | 50 | 188 | 50 | 319 | 1 % |
| 1.1 | 50 | 173 | 48 | 261 | 1 % |
| 1.4 | 50 | 209 | 38 | 59 | 0 % |
| 1.6 | 50 | 206 | 49 | 59 | 0 % |
| 1.9 | 50 | 191 | 43 | 63 | 0 % |
| 2.1 | 50 | 229 | 47 | 55 | 0 % |
| 2.4 | 50 | 211 | 54 | 222 | 0 % |
| 2.6 | 50 | 193 | 31 | 50 | 0 % |
| 2.9 | 50 | 205 | 47 | 59 | 0 % |
| 3.1 | 50 | 190 | 49 | 62 | 1 % |
| 3.4 | 50 | 161 | 47 | 58 | 0 % |
| 3.6 | 41 | 70 | 26 | 48 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
