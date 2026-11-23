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
| Veredicto | FALLA |
| Usuarios sin degradación | 7 |
| Peticiones hechas | 390 |
| Peticiones correctas (2xx) | 128 |
| Errores (%) | 67.2 % |
| Ritmo medio (peticiones/s) | 1.3 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 89 |
| Tiempo con la CPU al tope (%) | 87 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 3 669 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 390 | 1.3 | no aplica (ramping-vus) | 67.2 % | 261 | 0 / 1 / 2 | 0 % | 72 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 8; fallidas: 31.
- Acciones hechas: panel 6, agenda 3, historia 4, reportes 1, recepcion 30, atencion 1, cobro 0, cancelar 0; sin trabajo disponible: 19.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 25 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 7 avisos en el registro.
- CPU del backend: 89 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 87 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 15 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.6 con unos 48 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **7 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 25 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 5 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.3 %).
- GC: 1102 pausas, 256.0 s en total (84 % del tiempo de vida de la JVM); 880 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24412 en total, 236 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 21 | 22 | 5801 |
| 0.2 | 74 | 75 | 16115 |
| 0.8 | 108 | 112 | 23756 |
| 1.2 | 108 | 112 | 23983 |
| 1.6 | 108 | 112 | 23993 |
| 2.0 | 108 | 112 | 24019 |
| 2.4 | 108 | 112 | 24116 |
| 2.8 | 108 | 112 | 24214 |
| 3.1 | 109 | 112 | 24258 |
| 3.5 | 109 | 112 | 24264 |
| 3.9 | 109 | 112 | 24286 |
| 4.3 | 109 | 112 | 24334 |
| 4.6 | 109 | 112 | 24336 |
| 5.0 | 109 | 112 | 24345 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 200 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 206 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.sql.exec` | 2 |
| `org.hibernate.query.sqm` | 2 |
| `veterinaria.vargasvet.service.impl` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.2 | 122.65 | 35 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.1 | 9.22 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 6.22 | 14 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.36 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 0.97 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.72 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 10.68 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 9.82 | 1 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 28 | 0.0 | 0.3 | 1 % |
| `insert into system_logs (category,company_id,correlation_id,dispositivo,duration_ms,error_type,event,http_method,ip_address,outcome,request_path,status_code,timestamp,user_email) values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$1` | 150 | 0.0 | 0.06 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 5 | la base tiene más (ver nota) |
| Mascotas | 3 | 4 | la base tiene más (ver nota) |
| Citas agendadas | 1 | 2 | la base tiene más (ver nota) |
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
| Registrar propietario | 4 | 26 | 86.7 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 3 | 1 | 25.0 % | 546 | 30 001 | 30 001 |
| Agendar cita | 1 | 2 | 66.7 % | 428 | 30 001 | 30 001 |
| Recepción (llegada) | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Iniciar atención | 0 | 1 | 100.0 % | 30 002 | 30 002 | 30 002 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 164 | 147 (90 %) | 30 000 | 30 001 | 30 001 | 56 % |
| apoderado | 30 | 26 (87 %) | 30 000 | 30 001 | 30 001 | 10 % |
| notificaciones | 29 | 13 (45 %) | 328 | 30 001 | 30 001 | 5 % |
| privacidad | 30 | 12 (40 %) | 51 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 28 | 12 (43 %) | 46 | 30 001 | 30 001 | 5 % |
| navegacion | 30 | 11 (37 %) | 350 | 30 001 | 30 002 | 4 % |
| cumplimiento | 29 | 11 (38 %) | 47 | 30 001 | 30 001 | 4 % |
| panel | 23 | 7 (30 %) | 58 | 30 001 | 30 001 | 3 % |
| empleados | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 2 % |
| citas_del_mes | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 2 % |
| propietarios | 3 | 3 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 7 | 49 | 298 | 500 | 0 % |
| 0.1 | 20 | 74 | 921 | 3 736 | 1 % |
| 0.4 | 34 | 6 | 6 244 | 6 244 | 0 % |
| 0.6 | 48 | 9 | 30 001 | 30 001 | 100 % |
| 0.9 | 50 | 24 | 30 001 | 30 001 | 100 % |
| 1.1 | 50 | 17 | 30 002 | 30 002 | 100 % |
| 1.4 | 50 | 14 | 30 002 | 30 002 | 100 % |
| 1.6 | 50 | 23 | 30 001 | 30 002 | 100 % |
| 1.9 | 50 | 15 | 30 001 | 30 001 | 100 % |
| 2.1 | 50 | 15 | 30 001 | 30 001 | 100 % |
| 2.4 | 50 | 23 | 30 001 | 30 001 | 100 % |
| 2.6 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 2.9 | 50 | 13 | 30 001 | 30 001 | 100 % |
| 3.1 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 3.4 | 50 | 23 | 30 001 | 30 001 | 100 % |
| 3.6 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 3.9 | 38 | 15 | 30 001 | 30 001 | 100 % |
| 4.1 | 21 | 8 | 30 001 | 30 001 | 100 % |
| 4.4 | 12 | 5 | 30 001 | 30 001 | 100 % |
| 4.6 | 8 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
