# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 13 |
| Peticiones hechas | 720 |
| Peticiones correctas (2xx) | 156 |
| Errores (%) | 78.3 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 97 |
| Tiempo con la CPU al tope (%) | 95 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 430 |
| Sentencias SQL | 4 429 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 720 | 2.1 | no aplica (ramping-vus) | 78.3 % | 563 | 0 / 1 / 4 | 0 % | 99 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 22; fallidas: 41.
- Acciones hechas: panel 12, agenda 5, historia 6, reportes 1, recepcion 38, atencion 2, cobro 0, cancelar 2; sin trabajo disponible: 23.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 52 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 6 avisos en el registro.
- CPU del backend: 97 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 95 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 5 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 430 MB (84 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 54 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **13 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 52 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 1.
- Correo: el servidor de pruebas recibió 7 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.3 %).
- GC: 1183 pausas, 328.7 s en total (84 % del tiempo de vida de la JVM); 961 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24338 en total, 241 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.1 | 24 | 25 | 6503 |
| 0.3 | 75 | 76 | 16410 |
| 1.1 | 108 | 112 | 24019 |
| 1.7 | 108 | 112 | 24080 |
| 2.1 | 108 | 112 | 24080 |
| 2.6 | 108 | 112 | 24086 |
| 3.1 | 108 | 112 | 24094 |
| 3.5 | 108 | 112 | 24101 |
| 4.0 | 108 | 112 | 24105 |
| 4.5 | 108 | 112 | 24108 |
| 5.0 | 109 | 112 | 24139 |
| 5.5 | 109 | 112 | 24250 |
| 5.9 | 109 | 112 | 24254 |
| 6.4 | 109 | 112 | 24264 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 195 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 201 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 3 |
| `org.springframework.dao.support` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.springframework.security.web` | 1 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.7 | 371.82 | 46 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 19 | 0.3 | 15.83 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 19 | 0.2 | 10.27 | 12 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 18 | 0.0 | 2.19 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 17 | 0.0 | 1.52 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 18.67 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 18.14 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 18 | 0.0 | 0.99 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 16.97 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 34 | 0.0 | 0.47 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 7 | 7 | coincide |
| Mascotas | 7 | 7 | coincide |
| Citas agendadas | 5 | 5 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 1 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 7 | 31 | 81.6 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 7 | 0 | 0.0 % | 647 | 3 896 | 3 896 |
| Agendar cita | 5 | 2 | 28.6 % | 1 684 | 30 000 | 30 000 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 15 582 | 30 001 | 30 001 |
| Iniciar atención | 1 | 1 | 50.0 % | 16 984 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 7 555 | 7 555 | 7 555 |
| Registrar diagnóstico | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Registrar receta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 441 | 423 (96 %) | 30 000 | 30 001 | 30 001 | 75 % |
| apoderado | 38 | 31 (82 %) | 30 000 | 30 001 | 30 001 | 6 % |
| panel | 30 | 13 (43 %) | 824 | 30 001 | 30 001 | 2 % |
| cumplimiento | 32 | 13 (41 %) | 309 | 30 001 | 30 001 | 2 % |
| navegacion | 32 | 13 (41 %) | 224 | 30 001 | 30 003 | 2 % |
| privacidad | 32 | 13 (41 %) | 307 | 30 001 | 30 001 | 2 % |
| notificaciones | 31 | 12 (39 %) | 426 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 30 | 11 (37 %) | 176 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 7 | 6 (86 %) | 30 000 | 30 003 | 30 003 | 1 % |
| empleados | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 5 | 4 (80 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 434 | 459 | 0 % |
| 0.2 | 27 | 59 | 3 967 | 6 442 | 2 % |
| 0.5 | 40 | 7 | 20 722 | 20 722 | 0 % |
| 0.8 | 54 | 13 | 30 001 | 30 001 | 100 % |
| 1.0 | 68 | 25 | 30 001 | 30 003 | 100 % |
| 1.2 | 81 | 17 | 30 001 | 30 001 | 100 % |
| 1.5 | 95 | 35 | 30 001 | 30 002 | 100 % |
| 1.8 | 100 | 30 | 30 001 | 30 002 | 100 % |
| 2.0 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 30 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 35 | 30 001 | 30 003 | 100 % |
| 3.2 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 36 | 30 001 | 30 003 | 100 % |
| 3.8 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 4.5 | 96 | 34 | 30 001 | 30 001 | 100 % |
| 4.8 | 75 | 25 | 30 001 | 30 001 | 100 % |
| 5.0 | 47 | 20 | 30 001 | 30 001 | 100 % |
| 5.2 | 26 | 2 | 30 001 | 30 001 | 100 % |
| 5.5 | 6 | 5 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
