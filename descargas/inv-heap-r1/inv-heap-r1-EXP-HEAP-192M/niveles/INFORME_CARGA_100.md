# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Veredicto | FALLA |
| Usuarios sin degradación | 15 |
| Peticiones hechas | 704 |
| Peticiones correctas (2xx) | 144 |
| Errores (%) | 79.5 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 90 |
| Tiempo con la CPU al tope (%) | 87 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 4 202 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 704 | 2.0 | no aplica (ramping-vus) | 79.5 % | 560 | 0 / 1 / 2 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 16; fallidas: 39.
- Acciones hechas: panel 12, agenda 5, historia 4, reportes 1, recepcion 38, atencion 2, cobro 0, cancelar 2; sin trabajo disponible: 24.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 35 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 5 avisos en el registro.
- CPU del backend: 90 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 87 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 18 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 56 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **15 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 35 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 9 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.3 %).
- GC: 1189 pausas, 298.9 s en total (85 % del tiempo de vida de la JVM); 966 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24490 en total, 377 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.1 | 24 | 25 | 6503 |
| 0.2 | 75 | 76 | 16410 |
| 1.0 | 108 | 112 | 23954 |
| 1.5 | 108 | 112 | 24066 |
| 1.9 | 108 | 112 | 24080 |
| 2.3 | 108 | 112 | 24088 |
| 2.8 | 108 | 112 | 24095 |
| 3.2 | 108 | 112 | 24117 |
| 3.6 | 108 | 112 | 24190 |
| 4.1 | 109 | 112 | 24278 |
| 4.5 | 109 | 112 | 24360 |
| 5.0 | 109 | 112 | 24406 |
| 5.4 | 109 | 112 | 24424 |
| 5.8 | 109 | 112 | 24428 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 316 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 6 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |
| `org.hibernate.internal.SessionImpl$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 320 |
| `org.springframework.transaction.support` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.internal.SessionImpl` | 4 |
| `org.springframework.web.servlet` | 3 |
| `org.springframework.dao.support` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 313.07 | 45 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 17 | 0.3 | 16.67 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 17 | 0.1 | 8.75 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 17 | 0.0 | 1.77 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 22.83 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 17 | 0.0 | 1.22 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 17 | 0.0 | 0.96 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 16.13 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 14.96 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 17 | 0.0 | 0.78 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 7 | 9 | la base tiene más (ver nota) |
| Mascotas | 4 | 6 | la base tiene más (ver nota) |
| Citas agendadas | 4 | 4 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 7 | 31 | 81.6 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 4 | 3 | 42.9 % | 1 525 | 30 001 | 30 001 |
| Agendar cita | 4 | 0 | 0.0 % | 681 | 3 057 | 3 057 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 15 948 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 2 | 100.0 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 000 | 30 001 | 30 001 | 76 % |
| apoderado | 38 | 31 (82 %) | 30 000 | 30 001 | 30 001 | 6 % |
| panel | 29 | 13 (45 %) | 305 | 30 001 | 30 002 | 2 % |
| cumplimiento | 31 | 12 (39 %) | 111 | 30 001 | 30 001 | 2 % |
| navegacion | 31 | 12 (39 %) | 163 | 30 001 | 30 001 | 2 % |
| privacidad | 31 | 12 (39 %) | 382 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 11 (38 %) | 52 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 10 (34 %) | 56 | 30 001 | 30 001 | 2 % |
| empleados | 5 | 5 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 5 | 5 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 5 | 4 (80 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 7 | 360 | 360 | 0 % |
| 0.0 | 15 | 97 | 385 | 513 | 0 % |
| 0.3 | 29 | 39 | 4 365 | 4 365 | 0 % |
| 0.5 | 42 | 2 | 30 001 | 30 001 | 50 % |
| 0.8 | 56 | 21 | 30 001 | 30 001 | 100 % |
| 1.0 | 69 | 22 | 30 001 | 30 001 | 100 % |
| 1.3 | 83 | 17 | 30 001 | 30 001 | 100 % |
| 1.5 | 97 | 38 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 4.5 | 95 | 36 | 30 001 | 30 001 | 100 % |
| 4.8 | 71 | 25 | 30 001 | 30 002 | 100 % |
| 5.0 | 40 | 16 | 30 001 | 30 001 | 100 % |
| 5.5 | 5 | 4 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
