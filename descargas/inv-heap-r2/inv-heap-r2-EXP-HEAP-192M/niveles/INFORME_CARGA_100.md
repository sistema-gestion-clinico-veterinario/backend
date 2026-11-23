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
| Veredicto | FALLA |
| Usuarios sin degradación | 19 |
| Peticiones hechas | 695 |
| Peticiones correctas (2xx) | 135 |
| Errores (%) | 80.6 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 41 |
| Tiempo con la CPU al tope (%) | 38 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 426 |
| Sentencias SQL | 3 684 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 695 | 2.0 | no aplica (ramping-vus) | 80.6 % | 560 | 0 / 1 / 2 | 0 % | 99 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 13; fallidas: 37.
- Acciones hechas: panel 12, agenda 4, historia 5, reportes 1, recepcion 36, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 37 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 1 avisos en el registro.
- CPU del backend: 41 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 38 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 7 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 426 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.6 con unos 47 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **19 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 37 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 1.
- Correo: el servidor de pruebas recibió 7 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.3 %).
- GC: 702 pausas, 132.3 s en total (41 % del tiempo de vida de la JVM); 481 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24214 en total, 134 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 11 | 11 | 3734 |
| 0.1 | 44 | 44 | 10582 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17210 |
| 0.9 | 108 | 112 | 23832 |
| 1.2 | 108 | 112 | 24033 |
| 1.4 | 108 | 112 | 24034 |
| 1.6 | 108 | 112 | 24038 |
| 1.8 | 108 | 112 | 24040 |
| 2.0 | 108 | 112 | 24040 |
| 2.2 | 108 | 112 | 24041 |
| 2.5 | 108 | 112 | 24066 |
| 2.7 | 108 | 112 | 24080 |
| 2.9 | 108 | 112 | 24088 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 94 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 99 |
| `org.apache.catalina.core` | 6 |
| `ch.qos.logback.classic` | 4 |
| `org.springframework.dao.support` | 2 |
| `veterinaria.vargasvet.service.impl` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `java.lang.invoke.InvokerBytecodeGenerator` | 1 |
| `org.springframework.security.web` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.5 | 254.79 | 46 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.2 | 14.02 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 6.77 | 10 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.71 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 1.14 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.9 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.53 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 12.54 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 12.1 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 28 | 0.0 | 0.38 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 7 | la base tiene más (ver nota) |
| Mascotas | 4 | 5 | la base tiene más (ver nota) |
| Citas agendadas | 3 | 3 | coincide |
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
| Registrar propietario | 5 | 31 | 86.1 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 4 | 1 | 20.0 % | 398 | 30 000 | 30 000 |
| Agendar cita | 3 | 1 | 25.0 % | 375 | 30 001 | 30 001 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 21 922 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 001 | 30 001 | 30 001 | 76 % |
| apoderado | 36 | 31 (86 %) | 30 000 | 30 001 | 30 001 | 6 % |
| privacidad | 31 | 13 (42 %) | 401 | 30 001 | 30 001 | 2 % |
| panel | 29 | 13 (45 %) | 505 | 30 001 | 30 001 | 2 % |
| cumplimiento | 30 | 13 (43 %) | 258 | 30 001 | 30 001 | 2 % |
| navegacion | 31 | 12 (39 %) | 57 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 12 (41 %) | 49 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 12 (41 %) | 50 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 5 | 5 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| empleados | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 3 (75 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 352 | 384 | 0 % |
| 0.1 | 19 | 78 | 508 | 3 097 | 0 % |
| 0.4 | 33 | 14 | 11 698 | 11 698 | 0 % |
| 0.6 | 47 | 5 | 30 001 | 30 001 | 80 % |
| 0.9 | 60 | 26 | 30 001 | 30 001 | 100 % |
| 1.1 | 74 | 18 | 30 001 | 30 001 | 100 % |
| 1.4 | 88 | 23 | 30 001 | 30 001 | 100 % |
| 1.6 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 2.1 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 2.4 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.6 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 3.1 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.4 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.6 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 4.1 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 4.4 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 4.6 | 90 | 32 | 30 001 | 30 001 | 100 % |
| 4.9 | 60 | 28 | 30 001 | 30 001 | 100 % |
| 5.1 | 36 | 5 | 30 001 | 30 001 | 100 % |
| 5.4 | 8 | 1 | 30 000 | 30 000 | 100 % |
| 5.6 | 4 | 1 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
