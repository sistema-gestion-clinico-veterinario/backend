# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 11 |
| Peticiones hechas | 740 |
| Peticiones correctas (2xx) | 175 |
| Errores (%) | 76.4 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 57 |
| Tiempo con la CPU al tope (%) | 54 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 5 208 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 740 | 2.1 | no aplica (ramping-vus) | 76.4 % | 564 | 1 / 1 / 7 | 0 % | 97 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 27; fallidas: 42.
- Acciones hechas: panel 12, agenda 6, historia 7, reportes 1, recepcion 41, atencion 2, cobro 0, cancelar 3; sin trabajo disponible: 26.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 26 errores `OutOfMemoryError` en el registro.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 54 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 28 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.5 con unos 38 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **11 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 26 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 11 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.3 %).
- GC: 743 pausas, 177.9 s en total (74 % del tiempo de vida de la JVM); 519 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24218 en total, 127 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 11 | 11 | 3772 |
| 0.1 | 50 | 50 | 11564 |
| 0.2 | 74 | 75 | 16107 |
| 0.4 | 80 | 81 | 17285 |
| 1.2 | 109 | 112 | 24041 |
| 1.5 | 109 | 112 | 24088 |
| 1.8 | 109 | 112 | 24091 |
| 2.1 | 109 | 112 | 24092 |
| 2.4 | 109 | 112 | 24093 |
| 2.7 | 109 | 112 | 24093 |
| 3.0 | 109 | 112 | 24094 |
| 3.3 | 109 | 112 | 24096 |
| 3.6 | 109 | 112 | 24096 |
| 3.9 | 109 | 112 | 24096 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 64 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 64 |
| `org.hibernate.query.sqm` | 12 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.springframework.security.web` | 2 |
| `org.springframework.dao.support` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.8 | 382.29 | 42 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 20 | 0.3 | 17.0 | 19 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 20 | 0.2 | 11.36 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 21 | 0.0 | 2.05 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 2 | 0.0 | 16.97 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 20 | 0.0 | 1.64 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 21 | 0.0 | 0.91 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 37 | 0.0 | 0.5 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 18.42 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 17.71 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 8 | 11 | la base tiene más (ver nota) |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 6 | 7 | la base tiene más (ver nota) |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
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
| Registrar propietario | 8 | 33 | 80.5 % | 30 001 | 30 001 | 30 001 |
| Registrar mascota | 8 | 0 | 0.0 % | 647 | 2 262 | 2 262 |
| Agendar cita | 6 | 2 | 25.0 % | 1 010 | 30 000 | 30 000 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 15 312 | 30 000 | 30 000 |
| Iniciar atención | 1 | 1 | 50.0 % | 15 725 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 1 669 | 1 669 | 1 669 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 403 | 2 403 | 2 403 |
| Registrar receta | 1 | 0 | 0.0 % | 9 079 | 9 079 | 9 079 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 3 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 436 | 417 (96 %) | 30 000 | 30 001 | 30 001 | 74 % |
| apoderado | 41 | 33 (80 %) | 30 001 | 30 001 | 30 001 | 6 % |
| privacidad | 35 | 14 (40 %) | 413 | 30 001 | 30 001 | 2 % |
| cumplimiento | 34 | 14 (41 %) | 463 | 30 001 | 30 001 | 2 % |
| navegacion | 35 | 13 (37 %) | 81 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 33 | 13 (39 %) | 152 | 30 001 | 30 001 | 2 % |
| notificaciones | 33 | 13 (39 %) | 69 | 30 001 | 30 001 | 2 % |
| panel | 31 | 11 (35 %) | 614 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 8 | 7 (88 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 8 | 7 (88 %) | 30 001 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 77 | 462 | 537 | 0 % |
| 0.2 | 25 | 80 | 3 777 | 8 133 | 0 % |
| 0.5 | 38 | 19 | 14 673 | 14 673 | 5 % |
| 0.7 | 52 | 7 | 30 001 | 30 001 | 100 % |
| 1.0 | 66 | 26 | 30 001 | 30 001 | 100 % |
| 1.2 | 79 | 20 | 30 001 | 30 001 | 100 % |
| 1.5 | 93 | 25 | 30 001 | 30 001 | 100 % |
| 1.7 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 27 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 4.5 | 98 | 40 | 30 001 | 30 001 | 100 % |
| 4.7 | 83 | 25 | 30 001 | 30 001 | 100 % |
| 5.0 | 50 | 22 | 30 001 | 30 001 | 100 % |
| 5.2 | 28 | 6 | 30 001 | 30 001 | 100 % |
| 5.5 | 9 | 4 | 30 001 | 30 001 | 100 % |
| 5.7 | 8 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
