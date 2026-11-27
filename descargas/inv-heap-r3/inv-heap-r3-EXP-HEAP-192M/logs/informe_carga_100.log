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
| Usuarios sin degradación | 17 |
| Peticiones hechas | 694 |
| Peticiones correctas (2xx) | 131 |
| Errores (%) | 81.1 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 52 |
| Tiempo con la CPU al tope (%) | 49 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 422 |
| Sentencias SQL | 3 636 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 694 | 2.0 | no aplica (ramping-vus) | 81.1 % | 563 | 0 / 1 / 2 | 0 % | 98 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 10; fallidas: 38.
- Acciones hechas: panel 12, agenda 4, historia 5, reportes 1, recepcion 36, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 35 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 3 avisos en el registro.
- CPU del backend: 52 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 49 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 7 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 422 MB (82 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 58 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **17 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 35 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 6 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.3 %).
- GC: 829 pausas, 166.5 s en total (78 % del tiempo de vida de la JVM); 606 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24213 en total, 202 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 12 | 12 | 3903 |
| 0.1 | 63 | 64 | 14060 |
| 0.2 | 79 | 80 | 17088 |
| 0.8 | 108 | 112 | 23740 |
| 1.1 | 108 | 112 | 23993 |
| 1.4 | 108 | 112 | 24003 |
| 1.7 | 108 | 112 | 24010 |
| 2.0 | 108 | 112 | 24011 |
| 2.2 | 108 | 112 | 24014 |
| 2.5 | 108 | 112 | 24014 |
| 2.8 | 108 | 112 | 24020 |
| 3.0 | 108 | 112 | 24029 |
| 3.3 | 108 | 112 | 24056 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 134 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 4 |
| `org.hibernate.internal.SessionImpl$$Lambda/` | 4 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 139 |
| `org.hibernate.query.sqm` | 12 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.transaction.support` | 4 |
| `org.hibernate.internal.SessionImpl` | 4 |
| `ch.qos.logback.classic` | 3 |
| `org.springframework.web.servlet` | 2 |
| `org.springframework.security.web` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 296.58 | 49 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.2 | 13.88 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 6.66 | 9 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.62 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 1.13 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 16.91 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.6 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.81 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 12.57 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 27 | 0.0 | 0.36 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 7 | la base tiene más (ver nota) |
| Mascotas | 3 | 3 | coincide |
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
| Registrar propietario | 4 | 32 | 88.9 % | 30 001 | 30 001 | 30 001 |
| Registrar mascota | 3 | 1 | 25.0 % | 277 | 30 001 | 30 001 |
| Agendar cita | 3 | 0 | 0.0 % | 455 | 795 | 795 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 30 000 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 000 | 30 001 | 30 001 | 76 % |
| apoderado | 36 | 32 (89 %) | 30 001 | 30 001 | 30 001 | 6 % |
| cumplimiento | 31 | 14 (45 %) | 47 | 30 001 | 30 001 | 2 % |
| privacidad | 31 | 13 (42 %) | 35 | 30 001 | 30 001 | 2 % |
| panel | 29 | 13 (45 %) | 427 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 13 (45 %) | 33 | 30 001 | 30 001 | 2 % |
| navegacion | 31 | 12 (39 %) | 355 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 12 (41 %) | 36 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 5 | 5 (100 %) | 30 000 | 30 000 | 30 000 | 1 % |
| empleados | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 3 (75 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 3 | 28 | 352 | 352 | 0 % |
| 0.1 | 17 | 82 | 344 | 2 825 | 0 % |
| 0.3 | 30 | 20 | 6 025 | 6 025 | 0 % |
| 0.6 | 44 | 3 | 30 001 | 30 001 | 67 % |
| 0.8 | 58 | 24 | 30 001 | 30 001 | 100 % |
| 1.1 | 71 | 20 | 30 001 | 30 001 | 100 % |
| 1.3 | 85 | 23 | 30 001 | 30 001 | 100 % |
| 1.6 | 99 | 35 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 2.1 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.6 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 3.1 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 3.6 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 4.1 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.6 | 92 | 35 | 30 001 | 30 001 | 100 % |
| 4.8 | 67 | 30 | 30 001 | 30 001 | 100 % |
| 5.1 | 39 | 10 | 30 001 | 30 001 | 100 % |
| 5.6 | 4 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
