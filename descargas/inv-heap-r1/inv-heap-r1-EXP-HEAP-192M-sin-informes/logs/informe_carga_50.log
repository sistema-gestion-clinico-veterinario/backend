# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 2 817 |
| Peticiones correctas (2xx) | 2 809 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 11.3 |
| Tiempo mediano (ms) | 13 |
| Tiempo p95 (ms) | 64 |
| Tiempo p99 (ms) | 366 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 20 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 75 985 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 2 817 | 11.3 | no aplica (ramping-vus) | 0.3 % | 0 | 1 / 2 / 2 | 0 % | 87 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 1037; fallidas: 8.
- Acciones hechas: panel 135, agenda 105, historia 99, reportes 0, recepcion 143, atencion 65, cobro 36, cancelar 29; sin trabajo disponible: 109.
- CPU del backend: 20 % de media y 64 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 18 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 428 MB (83 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **50 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 143 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 304 pausas, 5.9 s en total (2 % del tiempo de vida de la JVM); 24 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25060 en total, 437 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 3 | 3 | 1574 |
| 0.0 | 10 | 10 | 3450 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 22 | 23 | 6003 |
| 0.1 | 47 | 48 | 11190 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16103 |
| 0.2 | 74 | 75 | 16123 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 79 | 80 | 17217 |
| 0.5 | 105 | 107 | 22616 |
| 1.3 | 111 | 114 | 24248 |
| 2.4 | 110 | 114 | 24676 |
| 3.6 | 110 | 114 | 24964 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 426 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 437 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 185 | 2.1 | 11.27 | 23 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 185 | 1.5 | 8.0 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 99 | 1.4 | 13.88 | 15 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 290 | 0.6 | 2.15 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 290 | 0.3 | 0.98 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 290 | 0.3 | 0.88 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 185 | 0.2 | 1.23 | 3 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 99 | 0.2 | 1.9 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 105 | 0.2 | 1.5 | 2 % |
| `select c1_0.historia_clinica_id,c1_0.id,c1_0.anamnesis,c1_0.cerrado_por,c1_0.cita_id,c1_0.clinical_template_id,c1_0.created_at,c1_0.desparasitacion_aplicada,c1_0.estado,c1_0.examen_fisico,c1_0.fecha_cierre,c1_0.fecha_con` | 99 | 0.1 | 1.39 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 143 | 143 | coincide |
| Mascotas | 143 | 143 | coincide |
| Citas agendadas | 135 | 135 | coincide |
| Diagnósticos | 65 | 65 | coincide |
| Recetas | 65 | 65 | coincide |
| Pagos | 36 | 36 | coincide |
| Citas completadas | 65 | 65 | coincide |
| Citas canceladas | 29 | 29 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 143 | 0 | 0.0 % | 22 | 96 | 187 |
| Registrar mascota | 143 | 0 | 0.0 % | 15 | 50 | 339 |
| Agendar cita | 135 | 8 | 5.6 % | 18 | 60 | 216 |
| Recepción (llegada) | 65 | 0 | 0.0 % | 10 | 29 | 35 |
| Iniciar atención | 65 | 0 | 0.0 % | 13 | 60 | 102 |
| Leer la consulta | 65 | 0 | 0.0 % | 7 | 17 | 108 |
| Registrar diagnóstico | 65 | 0 | 0.0 % | 7 | 24 | 93 |
| Registrar receta | 65 | 0 | 0.0 % | 8 | 27 | 290 |
| Actualizar mascota | 65 | 0 | 0.0 % | 8 | 21 | 32 |
| Cerrar la consulta | 65 | 0 | 0.0 % | 15 | 81 | 207 |
| Caja: crear punto de cobro | 32 | 0 | 0.0 % | 11 | 97 | 118 |
| Caja: vincular equipo | 32 | 0 | 0.0 % | 8 | 36 | 39 |
| Caja: abrir la caja | 32 | 0 | 0.0 % | 8 | 22 | 38 |
| Cobrar | 36 | 0 | 0.0 % | 12 | 28 | 34 |
| Cancelar cita | 29 | 0 | 0.0 % | 14 | 46 | 93 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 50 | 0 (0 %) | 373 | 698 | 1 203 | 27 % |
| panel | 185 | 0 (0 %) | 51 | 83 | 138 | 13 % |
| apoderado | 143 | 0 (0 %) | 22 | 96 | 187 | 6 % |
| cita | 143 | 8 (6 %) | 18 | 60 | 216 | 5 % |
| propietarios | 105 | 0 (0 %) | 30 | 62 | 135 | 5 % |
| lista_de_historias | 99 | 0 (0 %) | 27 | 84 | 337 | 5 % |
| mascota | 143 | 0 (0 %) | 15 | 50 | 339 | 5 % |
| citas_del_mes | 105 | 0 (0 %) | 20 | 52 | 116 | 4 % |
| navegacion | 185 | 0 (0 %) | 7 | 21 | 141 | 3 % |
| historia | 99 | 0 (0 %) | 15 | 54 | 151 | 3 % |
| avisos_de_cumplimiento | 185 | 0 (0 %) | 6 | 22 | 85 | 2 % |
| cumplimiento | 185 | 0 (0 %) | 6 | 23 | 48 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 7 | 49 | 408 | 454 | 0 % |
| 0.1 | 20 | 128 | 372 | 698 | 0 % |
| 0.4 | 34 | 163 | 341 | 511 | 0 % |
| 0.6 | 48 | 244 | 340 | 466 | 0 % |
| 0.9 | 50 | 188 | 60 | 335 | 1 % |
| 1.1 | 50 | 169 | 47 | 59 | 2 % |
| 1.4 | 50 | 208 | 58 | 126 | 0 % |
| 1.6 | 50 | 206 | 45 | 60 | 0 % |
| 1.9 | 50 | 185 | 44 | 61 | 0 % |
| 2.1 | 50 | 239 | 49 | 102 | 0 % |
| 2.4 | 50 | 205 | 46 | 70 | 0 % |
| 2.6 | 50 | 199 | 59 | 101 | 0 % |
| 2.9 | 50 | 201 | 69 | 175 | 0 % |
| 3.1 | 50 | 199 | 50 | 75 | 1 % |
| 3.4 | 50 | 164 | 45 | 67 | 0 % |
| 3.6 | 41 | 70 | 33 | 50 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
