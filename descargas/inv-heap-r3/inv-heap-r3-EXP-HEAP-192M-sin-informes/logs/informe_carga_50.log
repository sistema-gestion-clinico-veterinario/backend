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
| Peticiones hechas | 2 807 |
| Peticiones correctas (2xx) | 2 804 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.3 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 70 |
| Tiempo p99 (ms) | 437 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 23 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 75 857 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 807 | 11.3 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 2 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1032; fallidas: 3.
- Acciones hechas: panel 135, agenda 105, historia 99, reportes 0, recepcion 144, atencion 63, cobro 36, cancelar 30; sin trabajo disponible: 108.
- CPU del backend: 23 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 16 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 144 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 307 pausas, 6.8 s en total (3 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25041 en total, 437 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1575 |
| 0.0 | 10 | 10 | 3447 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 22 | 23 | 5999 |
| 0.1 | 47 | 48 | 11189 |
| 0.2 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16104 |
| 0.2 | 74 | 75 | 16247 |
| 0.3 | 79 | 80 | 17106 |
| 0.4 | 79 | 80 | 17225 |
| 0.6 | 105 | 107 | 22652 |
| 1.4 | 111 | 114 | 24256 |
| 2.5 | 110 | 114 | 24675 |
| 3.6 | 110 | 114 | 24963 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 422 |
| `java.lang.invoke.LambdaForm$DMH/` | 14 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 436 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 185 | 3.3 | 17.97 | 28 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 185 | 1.8 | 9.57 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.7 | 16.89 | 14 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 290 | 0.8 | 2.81 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 290 | 0.5 | 1.77 | 4 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 290 | 0.3 | 1.19 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 185 | 0.3 | 1.68 | 3 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 144 | 0.2 | 1.48 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 105 | 0.2 | 1.96 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 99 | 0.2 | 1.98 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 144 | 144 | coincide |
| Mascotas | 144 | 144 | coincide |
| Citas agendadas | 141 | 141 | coincide |
| Diagnósticos | 63 | 63 | coincide |
| Recetas | 63 | 63 | coincide |
| Pagos | 36 | 36 | coincide |
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
| Registrar propietario | 144 | 0 | 0.0 % | 25 | 57 | 236 |
| Registrar mascota | 144 | 0 | 0.0 % | 18 | 41 | 170 |
| Agendar cita | 141 | 3 | 2.1 % | 23 | 66 | 286 |
| Recepción (llegada) | 63 | 0 | 0.0 % | 12 | 31 | 35 |
| Iniciar atención | 63 | 0 | 0.0 % | 16 | 35 | 48 |
| Leer la consulta | 63 | 0 | 0.0 % | 8 | 23 | 33 |
| Registrar diagnóstico | 63 | 0 | 0.0 % | 8 | 22 | 52 |
| Registrar receta | 63 | 0 | 0.0 % | 9 | 26 | 61 |
| Actualizar mascota | 63 | 0 | 0.0 % | 10 | 24 | 31 |
| Cerrar la consulta | 63 | 0 | 0.0 % | 18 | 44 | 175 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 9 | 25 | 31 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 9 | 25 | 34 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 9 | 27 | 37 |
| Cobrar | 36 | 0 | 0.0 % | 13 | 31 | 32 |
| Cancelar cita | 30 | 0 | 0.0 % | 15 | 38 | 41 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 445 | 771 | 1 391 | 29 % |
| panel | 185 | 0 (0 %) | 65 | 100 | 133 | 15 % |
| cita | 144 | 3 (2 %) | 23 | 66 | 286 | 6 % |
| apoderado | 144 | 0 (0 %) | 25 | 57 | 236 | 6 % |
| lista_de_historias | 99 | 0 (0 %) | 31 | 55 | 594 | 5 % |
| propietarios | 105 | 0 (0 %) | 28 | 79 | 133 | 5 % |
| mascota | 144 | 0 (0 %) | 18 | 41 | 170 | 4 % |
| citas_del_mes | 105 | 0 (0 %) | 24 | 45 | 58 | 3 % |
| historia | 99 | 0 (0 %) | 15 | 42 | 223 | 2 % |
| navegacion | 185 | 0 (0 %) | 8 | 19 | 30 | 2 % |
| cumplimiento | 185 | 0 (0 %) | 7 | 22 | 36 | 2 % |
| avisos_de_cumplimiento | 185 | 0 (0 %) | 7 | 19 | 28 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 456 | 456 | 0 % |
| 0.1 | 16 | 107 | 468 | 1 039 | 0 % |
| 0.3 | 29 | 153 | 427 | 645 | 0 % |
| 0.6 | 43 | 231 | 415 | 520 | 1 % |
| 0.8 | 50 | 216 | 96 | 457 | 0 % |
| 1.1 | 50 | 161 | 41 | 79 | 0 % |
| 1.3 | 50 | 192 | 58 | 276 | 0 % |
| 1.6 | 50 | 219 | 62 | 100 | 0 % |
| 1.8 | 50 | 190 | 58 | 100 | 0 % |
| 2.0 | 50 | 215 | 58 | 66 | 0 % |
| 2.3 | 50 | 234 | 59 | 95 | 0 % |
| 2.5 | 50 | 175 | 56 | 95 | 0 % |
| 2.8 | 50 | 199 | 59 | 67 | 0 % |
| 3.0 | 50 | 195 | 59 | 92 | 1 % |
| 3.3 | 50 | 211 | 61 | 85 | 0 % |
| 3.5 | 47 | 83 | 30 | 62 | 0 % |
| 3.8 | 22 | 12 | 60 | 60 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
