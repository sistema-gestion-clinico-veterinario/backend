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
| Usuarios sin degradación | 10 |
| Peticiones hechas | 2 817 |
| Peticiones correctas (2xx) | 2 813 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.3 |
| Tiempo mediano (ms) | 11 |
| Tiempo p95 (ms) | 53 |
| Tiempo p99 (ms) | 331 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 18 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 75 963 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 817 | 11.3 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 3 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1035; fallidas: 4.
- Acciones hechas: panel 136, agenda 105, historia 99, reportes 0, recepcion 143, atencion 64, cobro 36, cancelar 30; sin trabajo disponible: 107.
- CPU del backend: 18 % de media y 87 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 14 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.3 s.
- Correo: el servidor de pruebas recibió 143 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 307 pausas, 5.6 s en total (2 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 179 MB (tope -Xmx192m).
- Clases cargadas: 25055 en total, 425 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1574 |
| 0.0 | 10 | 10 | 3447 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 22 | 23 | 5999 |
| 0.1 | 47 | 48 | 11190 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16103 |
| 0.2 | 74 | 75 | 16127 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17216 |
| 0.4 | 105 | 107 | 22657 |
| 1.2 | 111 | 114 | 24254 |
| 2.3 | 110 | 114 | 24668 |
| 3.5 | 110 | 114 | 24958 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 414 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 425 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 186 | 2.6 | 14.02 | 30 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 186 | 1.3 | 6.98 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.3 | 12.87 | 14 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 291 | 0.6 | 2.06 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 291 | 0.3 | 0.88 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 291 | 0.3 | 0.87 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 186 | 0.2 | 1.16 | 2 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 143 | 0.2 | 1.22 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 105 | 0.2 | 1.55 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 99 | 0.1 | 1.41 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 143 | 143 | coincide |
| Mascotas | 143 | 143 | coincide |
| Citas agendadas | 139 | 139 | coincide |
| Diagnósticos | 64 | 64 | coincide |
| Recetas | 64 | 64 | coincide |
| Pagos | 36 | 36 | coincide |
| Citas completadas | 64 | 64 | coincide |
| Citas canceladas | 30 | 30 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 143 | 0 | 0.0 % | 21 | 49 | 79 |
| Registrar mascota | 143 | 0 | 0.0 % | 14 | 33 | 51 |
| Agendar cita | 139 | 4 | 2.8 % | 17 | 41 | 68 |
| Recepción (llegada) | 64 | 0 | 0.0 % | 11 | 25 | 37 |
| Iniciar atención | 64 | 0 | 0.0 % | 13 | 28 | 40 |
| Leer la consulta | 64 | 0 | 0.0 % | 7 | 17 | 25 |
| Registrar diagnóstico | 64 | 0 | 0.0 % | 7 | 17 | 25 |
| Registrar receta | 64 | 0 | 0.0 % | 8 | 17 | 29 |
| Actualizar mascota | 64 | 0 | 0.0 % | 8 | 16 | 25 |
| Cerrar la consulta | 64 | 0 | 0.0 % | 14 | 36 | 71 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 9 | 22 | 25 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 7 | 14 | 21 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 8 | 17 | 21 |
| Cobrar | 36 | 0 | 0.0 % | 10 | 22 | 29 |
| Cancelar cita | 30 | 0 | 0.0 % | 14 | 37 | 44 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 335 | 431 | 1 175 | 28 % |
| panel | 186 | 0 (0 %) | 49 | 79 | 288 | 16 % |
| apoderado | 143 | 0 (0 %) | 21 | 49 | 79 | 6 % |
| cita | 143 | 4 (3 %) | 17 | 41 | 68 | 5 % |
| propietarios | 105 | 0 (0 %) | 22 | 68 | 110 | 5 % |
| lista_de_historias | 99 | 0 (0 %) | 24 | 44 | 248 | 4 % |
| mascota | 143 | 0 (0 %) | 14 | 33 | 51 | 4 % |
| citas_del_mes | 105 | 0 (0 %) | 20 | 38 | 50 | 4 % |
| historia | 99 | 0 (0 %) | 13 | 37 | 255 | 3 % |
| navegacion | 186 | 0 (0 %) | 8 | 15 | 24 | 2 % |
| avisos_de_cumplimiento | 186 | 0 (0 %) | 5 | 14 | 29 | 2 % |
| cumplimiento | 186 | 0 (0 %) | 5 | 14 | 28 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 77 | 342 | 358 | 0 % |
| 0.2 | 24 | 146 | 327 | 533 | 1 % |
| 0.5 | 38 | 178 | 323 | 398 | 1 % |
| 0.7 | 50 | 242 | 288 | 377 | 0 % |
| 0.9 | 50 | 184 | 33 | 48 | 0 % |
| 1.2 | 50 | 178 | 42 | 67 | 0 % |
| 1.4 | 50 | 209 | 46 | 58 | 0 % |
| 1.7 | 50 | 215 | 47 | 61 | 0 % |
| 1.9 | 50 | 211 | 44 | 60 | 0 % |
| 2.2 | 50 | 177 | 47 | 64 | 0 % |
| 2.5 | 50 | 218 | 49 | 77 | 0 % |
| 2.7 | 50 | 195 | 45 | 50 | 0 % |
| 3.0 | 50 | 221 | 46 | 61 | 0 % |
| 3.2 | 50 | 190 | 49 | 78 | 0 % |
| 3.5 | 49 | 137 | 46 | 68 | 0 % |
| 3.7 | 36 | 39 | 74 | 76 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
