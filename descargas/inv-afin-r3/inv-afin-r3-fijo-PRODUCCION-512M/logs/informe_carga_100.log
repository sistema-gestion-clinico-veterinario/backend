# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: PRODUCCION-512M (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: P, en el control: P)
- usadas en esta corrida: P para los niveles y P para el control (perfil PRODUCCION-512M)
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 16 |
| Peticiones hechas | 765 |
| Peticiones correctas (2xx) | 199 |
| Errores (%) | 74.0 % |
| Ritmo medio (peticiones/s) | 2.2 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 97 |
| Tiempo con la CPU al tope (%) | 95 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 429 |
| Sentencias SQL | 5 174 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 765 | 2.2 | no aplica (ramping-vus) | 74.0 % | 566 | 1 / 2 / 7 | 0 % | 98 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 33; fallidas: 45.
- Acciones hechas: panel 13, agenda 7, historia 10, reportes 1, recepcion 45, atencion 4, cobro 0, cancelar 3; sin trabajo disponible: 28.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 38 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 1 avisos en el registro.
- CPU del backend: 97 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 95 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 29 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 429 MB (84 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.6 con unos 43 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **16 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 38 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 9 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.3 %).
- GC: 1204 pausas, 329.6 s en total (83 % del tiempo de vida de la JVM); 984 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24438 en total, 256 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.1 | 28 | 28 | 7111 |
| 0.3 | 78 | 79 | 17011 |
| 1.3 | 109 | 112 | 24076 |
| 1.8 | 109 | 112 | 24179 |
| 2.3 | 109 | 112 | 24180 |
| 2.8 | 109 | 112 | 24181 |
| 3.2 | 109 | 112 | 24182 |
| 3.7 | 109 | 112 | 24201 |
| 4.2 | 109 | 112 | 24225 |
| 4.7 | 109 | 112 | 24252 |
| 5.2 | 109 | 112 | 24367 |
| 5.7 | 109 | 112 | 24416 |
| 6.1 | 109 | 112 | 24438 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 197 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 5 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 202 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 5 |
| `org.springframework.web.servlet` | 5 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.springframework.security.core` | 2 |
| `org.springframework.dao.support` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 22 | 0.3 | 12.54 | 23 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 22 | 0.2 | 10.98 | 20 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 1 | 0.2 | 210.79 | 18 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 23 | 0.1 | 2.19 | 4 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 22 | 0.0 | 1.73 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 24.32 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 36 | 0.0 | 0.63 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 23 | 0.0 | 0.97 | 2 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 22 | 0.0 | 0.91 | 2 % |
| `select a1_0.id from apoderado a1_0 left join usuario u1_0 on u1_0.id=a1_0.user_id left join company c1_0 on c1_0.id=a1_0.company_id where u1_0.id=$1 and c1_0.id=$2 fetch first $3 rows only` | 94 | 0.0 | 0.15 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 9 | 9 | coincide |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 8 | 8 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 9 | 36 | 80.0 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 1 | 11.1 % | 569 | 30 001 | 30 001 |
| Agendar cita | 8 | 0 | 0.0 % | 800 | 1 792 | 1 792 |
| Recepción (llegada) | 2 | 2 | 50.0 % | 22 979 | 30 000 | 30 000 |
| Iniciar atención | 1 | 3 | 75.0 % | 30 000 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 764 | 764 | 764 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 1 342 | 1 342 | 1 342 |
| Registrar receta | 1 | 0 | 0.0 % | 1 524 | 1 524 | 1 524 |
| Actualizar mascota | 1 | 0 | 0.0 % | 1 018 | 1 018 | 1 018 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 420 | 398 (95 %) | 30 000 | 30 001 | 30 001 | 70 % |
| apoderado | 45 | 36 (80 %) | 30 000 | 30 001 | 30 001 | 6 % |
| avisos_de_cumplimiento | 37 | 15 (41 %) | 170 | 30 001 | 30 001 | 3 % |
| panel | 35 | 14 (40 %) | 886 | 30 001 | 30 002 | 3 % |
| cumplimiento | 38 | 14 (37 %) | 150 | 30 001 | 30 001 | 2 % |
| notificaciones | 37 | 14 (38 %) | 427 | 30 001 | 30 001 | 2 % |
| navegacion | 38 | 14 (37 %) | 458 | 30 001 | 30 001 | 2 % |
| privacidad | 38 | 14 (37 %) | 126 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 11 | 10 (91 %) | 30 000 | 30 001 | 30 001 | 2 % |
| empleados | 11 | 10 (91 %) | 30 001 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 10 | 9 (90 %) | 30 000 | 30 001 | 30 001 | 2 % |
| propietarios | 7 | 6 (86 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 452 | 452 | 0 % |
| 0.1 | 16 | 90 | 565 | 1 535 | 0 % |
| 0.3 | 29 | 81 | 3 349 | 8 948 | 0 % |
| 0.6 | 43 | 14 | 30 001 | 30 001 | 7 % |
| 0.8 | 57 | 10 | 30 001 | 30 001 | 90 % |
| 1.1 | 70 | 32 | 30 001 | 30 001 | 100 % |
| 1.3 | 84 | 16 | 30 001 | 30 001 | 100 % |
| 1.6 | 98 | 36 | 30 001 | 30 012 | 100 % |
| 1.8 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 42 | 30 001 | 30 002 | 100 % |
| 4.5 | 92 | 29 | 30 001 | 30 001 | 100 % |
| 4.8 | 70 | 31 | 30 001 | 30 002 | 100 % |
| 5.0 | 43 | 15 | 30 001 | 30 001 | 100 % |
| 5.3 | 21 | 5 | 30 001 | 30 001 | 100 % |
| 5.5 | 8 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
