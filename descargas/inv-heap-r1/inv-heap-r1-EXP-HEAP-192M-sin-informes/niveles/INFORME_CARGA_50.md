# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 2 825 |
| Peticiones correctas (2xx) | 2 822 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 11.4 |
| Tiempo mediano (ms) | 12 |
| Tiempo p95 (ms) | 49 |
| Tiempo p99 (ms) | 279 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 19 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 76 148 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 825 | 11.4 | no aplica (ramping-vus) | 0.1 % | 0 | 1 / 2 / 3 | 0 % | 86 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1044; fallidas: 3.
- Acciones hechas: panel 136, agenda 105, historia 99, reportes 0, recepcion 143, atencion 65, cobro 37, cancelar 30; sin trabajo disponible: 109.
- CPU del backend: 19 % de media y 88 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 16 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.3 s.
- Correo: el servidor de pruebas recibió 143 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 308 pausas, 6.9 s en total (3 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25059 en total, 428 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1574 |
| 0.0 | 10 | 10 | 3448 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 22 | 23 | 6002 |
| 0.1 | 47 | 48 | 11189 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16102 |
| 0.2 | 74 | 75 | 16247 |
| 0.2 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17225 |
| 0.5 | 105 | 107 | 22653 |
| 1.3 | 111 | 114 | 24254 |
| 2.3 | 110 | 114 | 24672 |
| 3.5 | 110 | 114 | 24963 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 417 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 428 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 186 | 1.8 | 9.78 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 186 | 1.2 | 6.45 | 16 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.0 | 10.13 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 291 | 0.5 | 1.78 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 291 | 0.2 | 0.82 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 291 | 0.2 | 0.75 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 186 | 0.2 | 0.97 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 105 | 0.1 | 1.38 | 2 % |
| `insert into system_logs (category,company_id,correlation_id,dispositivo,duration_ms,error_type,event,http_method,ip_address,outcome,request_path,status_code,timestamp,user_email) values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$1` | 2825 | 0.1 | 0.05 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 99 | 0.1 | 1.41 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 143 | 143 | coincide |
| Mascotas | 143 | 143 | coincide |
| Citas agendadas | 140 | 140 | coincide |
| Diagnósticos | 65 | 65 | coincide |
| Recetas | 65 | 65 | coincide |
| Pagos | 37 | 37 | coincide |
| Citas completadas | 65 | 65 | coincide |
| Citas canceladas | 30 | 30 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 143 | 0 | 0.0 % | 20 | 41 | 133 |
| Registrar mascota | 143 | 0 | 0.0 % | 15 | 42 | 267 |
| Agendar cita | 140 | 3 | 2.1 % | 17 | 49 | 99 |
| Recepción (llegada) | 65 | 0 | 0.0 % | 10 | 23 | 42 |
| Iniciar atención | 65 | 0 | 0.0 % | 13 | 34 | 40 |
| Leer la consulta | 65 | 0 | 0.0 % | 8 | 16 | 26 |
| Registrar diagnóstico | 65 | 0 | 0.0 % | 8 | 22 | 27 |
| Registrar receta | 65 | 0 | 0.0 % | 9 | 21 | 32 |
| Actualizar mascota | 65 | 0 | 0.0 % | 10 | 22 | 41 |
| Cerrar la consulta | 65 | 0 | 0.0 % | 16 | 39 | 277 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 9 | 29 | 43 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 8 | 17 | 21 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 8 | 20 | 23 |
| Cobrar | 37 | 0 | 0.0 % | 12 | 28 | 29 |
| Cancelar cita | 30 | 0 | 0.0 % | 13 | 37 | 52 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 279 | 427 | 1 207 | 25 % |
| panel | 186 | 0 (0 %) | 44 | 71 | 91 | 14 % |
| apoderado | 143 | 0 (0 %) | 20 | 41 | 133 | 6 % |
| cita | 143 | 3 (2 %) | 17 | 49 | 99 | 5 % |
| mascota | 143 | 0 (0 %) | 15 | 42 | 267 | 5 % |
| propietarios | 105 | 0 (0 %) | 21 | 58 | 127 | 5 % |
| lista_de_historias | 99 | 0 (0 %) | 23 | 42 | 341 | 4 % |
| citas_del_mes | 105 | 0 (0 %) | 18 | 35 | 47 | 3 % |
| historia | 99 | 0 (0 %) | 14 | 39 | 275 | 3 % |
| navegacion | 186 | 0 (0 %) | 8 | 17 | 33 | 3 % |
| cerrar_consulta | 65 | 0 (0 %) | 16 | 39 | 277 | 2 % |
| cumplimiento | 186 | 0 (0 %) | 6 | 17 | 34 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 84 | 279 | 341 | 0 % |
| 0.2 | 26 | 150 | 294 | 520 | 0 % |
| 0.5 | 39 | 207 | 261 | 318 | 1 % |
| 0.7 | 50 | 216 | 263 | 317 | 0 % |
| 1.0 | 50 | 185 | 38 | 51 | 0 % |
| 1.2 | 50 | 182 | 38 | 267 | 0 % |
| 1.5 | 50 | 211 | 43 | 67 | 0 % |
| 1.7 | 50 | 209 | 42 | 48 | 0 % |
| 2.0 | 50 | 209 | 42 | 54 | 0 % |
| 2.2 | 50 | 174 | 42 | 51 | 0 % |
| 2.5 | 50 | 218 | 43 | 73 | 0 % |
| 2.7 | 50 | 203 | 39 | 49 | 0 % |
| 3.0 | 50 | 204 | 40 | 48 | 0 % |
| 3.2 | 50 | 229 | 41 | 55 | 0 % |
| 3.5 | 47 | 109 | 38 | 52 | 0 % |
| 3.7 | 32 | 35 | 66 | 71 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
