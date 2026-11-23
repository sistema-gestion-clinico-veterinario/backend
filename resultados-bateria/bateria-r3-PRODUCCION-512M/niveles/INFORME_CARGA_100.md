# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: PRODUCCION-512M (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: P, en el control: P)
- usadas en esta corrida: P para los niveles y P para el control (perfil PRODUCCION-512M)
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 11 |
| Peticiones hechas | 794 |
| Peticiones correctas (2xx) | 243 |
| Errores (%) | 69.4 % |
| Ritmo medio (peticiones/s) | 2.3 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 19 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 423 |
| Sentencias SQL | 6 776 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 794 | 2.3 | no aplica (ramping-vus) | 69.4 % | 551 | 0 / 1 / 2 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 53; fallidas: 32.
- Acciones hechas: panel 18, agenda 9, historia 9, reportes 1, recepcion 36, atencion 5, cobro 1, cancelar 3; sin trabajo disponible: 27.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 10 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 52 avisos en el registro.
- CPU del backend: 19 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 16 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 423 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 52 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **11 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 10 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 12 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.2 %).
- GC: 414 pausas, 52.8 s en total (17 % del tiempo de vida de la JVM); 190 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24587 en total, 321 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2133 |
| 0.0 | 11 | 12 | 3811 |
| 0.1 | 26 | 26 | 6807 |
| 0.1 | 61 | 61 | 13658 |
| 0.1 | 74 | 75 | 16095 |
| 0.2 | 75 | 76 | 16406 |
| 0.3 | 79 | 80 | 17209 |
| 0.8 | 106 | 108 | 22865 |
| 1.0 | 108 | 112 | 23861 |
| 1.1 | 108 | 112 | 23959 |
| 1.3 | 108 | 112 | 23969 |
| 1.4 | 108 | 112 | 23971 |
| 1.5 | 108 | 112 | 23973 |
| 1.7 | 109 | 112 | 24196 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 221 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 14 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |
| `java.lang.invoke.LambdaForm$BMH/` | 7 |
| `veterinaria.vargasvet.service.PuntoCobroService$$Lambda/` | 7 |
| `org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard$$Lambda/` | 5 |
| `veterinaria.vargasvet.service.impl.CajaServiceImpl$$Lambda/` | 5 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 4 |
| `jdk.proxy#.$Proxy#` | 4 |
| `org.hibernate.validator.internal.engine.valueextraction.ValueExtractorResolver$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 236 |
| `com.fasterxml.jackson.databind` | 20 |
| `veterinaria.vargasvet.service.impl` | 16 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 14 |
| `org.hibernate.validator.internal` | 11 |
| `veterinaria.vargasvet.service.PuntoCobroService` | 7 |
| `org.hibernate.persister.entity` | 5 |
| `jdk.proxy2.` | 4 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 24 | 2.0 | 82.68 | 46 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 24 | 0.7 | 27.35 | 15 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.5 | 269.89 | 13 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 24 | 0.3 | 12.55 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 24 | 0.3 | 11.39 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 24 | 0.2 | 6.71 | 4 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 5 | 0.1 | 12.08 | 1 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 29 | 0.1 | 1.74 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 29 | 0.0 | 0.8 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 30 | 0.0 | 0.7 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 9 | 12 | la base tiene más (ver nota) |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 8 | 8 | coincide |
| Diagnósticos | 3 | 3 | coincide |
| Recetas | 3 | 3 | coincide |
| Pagos | 1 | 1 | coincide |
| Citas completadas | 3 | 3 | coincide |
| Citas canceladas | 3 | 3 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 9 | 27 | 75.0 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 1 | 11.1 % | 35 | 30 000 | 30 000 |
| Agendar cita | 8 | 0 | 0.0 % | 28 | 1 217 | 1 217 |
| Recepción (llegada) | 3 | 2 | 40.0 % | 19 | 30 001 | 30 001 |
| Iniciar atención | 3 | 2 | 40.0 % | 38 | 30 001 | 30 001 |
| Leer la consulta | 3 | 0 | 0.0 % | 9 | 12 | 12 |
| Registrar diagnóstico | 3 | 0 | 0.0 % | 11 | 36 | 36 |
| Registrar receta | 3 | 0 | 0.0 % | 14 | 39 | 39 |
| Actualizar mascota | 3 | 0 | 0.0 % | 12 | 19 | 19 |
| Cerrar la consulta | 3 | 0 | 0.0 % | 27 | 117 | 117 |
| Caja: crear punto de cobro | 1 | 0 | 0.0 % | 29 | 29 | 29 |
| Caja: vincular equipo | 1 | 0 | 0.0 % | 30 | 30 | 30 |
| Caja: abrir la caja | 1 | 0 | 0.0 % | 58 | 58 | 58 |
| Cobrar | 1 | 0 | 0.0 % | 40 | 40 | 40 |
| Cancelar cita | 3 | 0 | 0.0 % | 22 | 23 672 | 23 672 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 000 | 30 001 | 30 001 | 78 % |
| apoderado | 36 | 27 (75 %) | 30 000 | 30 001 | 30 001 | 5 % |
| cumplimiento | 37 | 13 (35 %) | 14 | 30 001 | 30 001 | 2 % |
| navegacion | 37 | 12 (32 %) | 14 | 30 001 | 30 001 | 2 % |
| privacidad | 37 | 12 (32 %) | 11 | 30 001 | 30 001 | 2 % |
| panel | 35 | 11 (31 %) | 240 | 30 001 | 30 001 | 2 % |
| notificaciones | 35 | 11 (31 %) | 9 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 35 | 11 (31 %) | 13 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 9 | 5 (56 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 9 | 4 (44 %) | 35 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 9 | 4 (44 %) | 31 | 30 001 | 30 001 | 1 % |
| empleados | 9 | 4 (44 %) | 20 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 82 | 341 | 362 | 0 % |
| 0.2 | 25 | 41 | 2 797 | 4 669 | 0 % |
| 0.5 | 39 | 2 | 16 366 | 16 366 | 0 % |
| 0.7 | 52 | 18 | 30 001 | 30 001 | 100 % |
| 1.0 | 66 | 26 | 30 001 | 30 001 | 73 % |
| 1.2 | 79 | 28 | 30 001 | 30 001 | 68 % |
| 1.5 | 93 | 30 | 30 001 | 30 001 | 97 % |
| 1.7 | 100 | 39 | 30 001 | 30 001 | 85 % |
| 2.0 | 100 | 40 | 30 001 | 30 001 | 92 % |
| 2.2 | 100 | 49 | 30 001 | 30 001 | 61 % |
| 2.5 | 100 | 46 | 30 001 | 30 001 | 80 % |
| 2.7 | 100 | 43 | 30 001 | 30 001 | 79 % |
| 3.0 | 100 | 37 | 30 001 | 30 001 | 92 % |
| 3.2 | 100 | 39 | 30 001 | 30 001 | 85 % |
| 3.5 | 100 | 42 | 30 001 | 30 001 | 88 % |
| 3.7 | 100 | 44 | 30 001 | 30 001 | 80 % |
| 4.0 | 100 | 35 | 30 001 | 30 001 | 74 % |
| 4.2 | 100 | 46 | 30 001 | 30 001 | 87 % |
| 4.5 | 98 | 57 | 30 001 | 30 001 | 70 % |
| 4.7 | 83 | 23 | 30 001 | 30 001 | 100 % |
| 5.0 | 49 | 21 | 30 001 | 30 001 | 100 % |
| 5.2 | 26 | 3 | 30 001 | 30 001 | 100 % |
| 5.5 | 4 | 2 | 30 001 | 30 001 | 100 % |
| 5.7 | 3 | 1 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
