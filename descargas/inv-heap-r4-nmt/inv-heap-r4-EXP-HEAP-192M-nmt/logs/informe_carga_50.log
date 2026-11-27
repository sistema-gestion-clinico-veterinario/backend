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
| Veredicto | FALLA |
| Usuarios sin degradación | 11 |
| Peticiones hechas | 721 |
| Peticiones correctas (2xx) | 493 |
| Errores (%) | 31.6 % |
| Ritmo medio (peticiones/s) | 2.4 |
| Tiempo mediano (ms) | 320 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 90 |
| Tiempo con la CPU al tope (%) | 87 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 438 |
| Sentencias SQL | 12 555 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 721 | 2.4 | no aplica (ramping-vus) | 31.6 % | 226 | 0 / 1 / 2 | 0 % | 73 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 120; fallidas: 45.
- Acciones hechas: panel 19, agenda 17, historia 19, reportes 4, recepcion 57, atencion 7, cobro 3, cancelar 7; sin trabajo disponible: 37.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 16 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 7 avisos en el registro.
- CPU del backend: 90 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 87 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 10 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 438 MB (86 % de 512 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **11 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 16 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 23 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 110 MB (+0.1 %).
- GC: 1045 pausas, 249.8 s en total (76 % del tiempo de vida de la JVM); 815 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25076 en total, 348 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 20 | 20 | 5512 |
| 0.2 | 74 | 75 | 16107 |
| 0.5 | 105 | 107 | 22603 |
| 1.2 | 108 | 112 | 24048 |
| 2.1 | 110 | 112 | 24663 |
| 2.5 | 110 | 112 | 24710 |
| 2.8 | 110 | 112 | 24728 |
| 3.2 | 110 | 112 | 24778 |
| 3.6 | 110 | 112 | 24784 |
| 4.0 | 110 | 112 | 24799 |
| 4.4 | 110 | 112 | 24953 |
| 4.7 | 110 | 112 | 24982 |
| 5.1 | 110 | 112 | 25018 |
| 5.5 | 110 | 112 | 25076 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 327 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 331 |
| `org.springframework.dao.support` | 2 |
| `org.postgresql.util.GT` | 1 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |
| `java.sql.SQLNonTransientConnectionException` | 1 |
| `org.springframework.transaction.support` | 1 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 49 | 1.1 | 23.19 | 32 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 49 | 0.8 | 16.85 | 23 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 3 | 0.8 | 260.82 | 22 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 10 | 0.1 | 13.19 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 54 | 0.1 | 2.39 | 4 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 49 | 0.1 | 1.3 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 54 | 0.0 | 0.63 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 49 | 0.0 | 0.63 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 23 | 0.0 | 1.31 | 1 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 49 | 0.0 | 0.5 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 23 | 23 | coincide |
| Mascotas | 23 | 23 | coincide |
| Citas agendadas | 21 | 21 | coincide |
| Diagnósticos | 6 | 6 | coincide |
| Recetas | 6 | 6 | coincide |
| Pagos | 1 | 1 | coincide |
| Citas completadas | 6 | 6 | coincide |
| Citas canceladas | 4 | 4 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 23 | 34 | 59.6 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 23 | 0 | 0.0 % | 35 | 6 917 | 19 436 |
| Agendar cita | 21 | 2 | 8.7 % | 320 | 2 155 | 19 168 |
| Recepción (llegada) | 7 | 0 | 0.0 % | 17 | 14 429 | 14 429 |
| Iniciar atención | 7 | 0 | 0.0 % | 23 | 24 609 | 24 609 |
| Leer la consulta | 7 | 0 | 0.0 % | 10 | 4 298 | 4 298 |
| Registrar diagnóstico | 6 | 1 | 14.3 % | 12 | 15 571 | 15 571 |
| Registrar receta | 6 | 1 | 14.3 % | 12 | 30 001 | 30 001 |
| Actualizar mascota | 6 | 1 | 14.3 % | 21 | 30 000 | 30 000 |
| Cerrar la consulta | 6 | 1 | 14.3 % | 37 | 30 000 | 30 000 |
| Caja: crear punto de cobro | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Caja: vincular equipo | 1 | 0 | 0.0 % | 35 | 35 | 35 |
| Caja: abrir la caja | 1 | 0 | 0.0 % | 37 | 37 | 37 |
| Cobrar | 1 | 0 | 0.0 % | 34 | 34 | 34 |
| Cancelar cita | 4 | 3 | 42.9 % | 17 545 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 107 | 74 (69 %) | 30 000 | 30 001 | 30 001 | 34 % |
| apoderado | 57 | 34 (60 %) | 30 000 | 30 001 | 30 001 | 15 % |
| empleados | 19 | 14 (74 %) | 30 000 | 30 001 | 30 001 | 6 % |
| citas_del_mes | 19 | 14 (74 %) | 30 000 | 30 001 | 30 001 | 6 % |
| propietarios | 17 | 12 (71 %) | 30 000 | 30 001 | 30 001 | 5 % |
| navegacion | 61 | 11 (18 %) | 86 | 30 001 | 30 001 | 5 % |
| privacidad | 61 | 11 (18 %) | 70 | 30 001 | 30 001 | 5 % |
| cumplimiento | 59 | 10 (17 %) | 61 | 30 001 | 30 001 | 4 % |
| lista_de_historias | 19 | 9 (47 %) | 9 033 | 30 001 | 30 001 | 4 % |
| notificaciones | 56 | 7 (12 %) | 59 | 30 001 | 30 001 | 3 % |
| avisos_de_cumplimiento | 54 | 5 (9 %) | 35 | 30 000 | 30 001 | 2 % |
| panel | 52 | 3 (6 %) | 186 | 30 000 | 30 001 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 77 | 349 | 365 | 0 % |
| 0.2 | 25 | 62 | 2 735 | 3 920 | 0 % |
| 0.5 | 39 | 5 | 9 033 | 9 033 | 0 % |
| 0.7 | 50 | 123 | 30 000 | 30 001 | 7 % |
| 1.0 | 50 | 133 | 330 | 374 | 1 % |
| 1.2 | 50 | 111 | 30 000 | 30 001 | 10 % |
| 1.5 | 50 | 10 | 30 001 | 30 001 | 70 % |
| 1.7 | 50 | 8 | 30 001 | 30 001 | 100 % |
| 2.0 | 50 | 35 | 30 001 | 30 001 | 100 % |
| 2.2 | 50 | 11 | 30 001 | 30 001 | 100 % |
| 2.5 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 2.7 | 50 | 29 | 30 001 | 30 001 | 100 % |
| 3.0 | 50 | 14 | 30 001 | 30 001 | 100 % |
| 3.2 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 3.5 | 50 | 21 | 30 001 | 30 001 | 100 % |
| 3.7 | 49 | 23 | 30 001 | 30 001 | 100 % |
| 4.0 | 33 | 11 | 30 001 | 30 001 | 100 % |
| 4.2 | 20 | 9 | 30 001 | 30 001 | 100 % |
| 4.5 | 17 | 7 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
