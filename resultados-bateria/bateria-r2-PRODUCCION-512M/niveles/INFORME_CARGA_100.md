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
| Usuarios sin degradación | 13 |
| Peticiones hechas | 688 |
| Peticiones correctas (2xx) | 120 |
| Errores (%) | 82.6 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 34 |
| Tiempo con la CPU al tope (%) | 31 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 422 |
| Sentencias SQL | 3 229 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 688 | 2.0 | no aplica (ramping-vus) | 82.6 % | 567 | 0 / 1 / 2 | 0 % | 102 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 8; fallidas: 35.
- Acciones hechas: panel 12, agenda 4, historia 3, reportes 1, recepcion 34, atencion 1, cobro 0, cancelar 0; sin trabajo disponible: 23.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 29 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 4 avisos en el registro.
- CPU del backend: 34 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 31 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 21 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 422 MB (82 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 54 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **13 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 29 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 1.
- Correo: el servidor de pruebas recibió 4 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 108 MB (+0.4 %).
- GC: 595 pausas, 107.9 s en total (69 % del tiempo de vida de la JVM); 371 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24139 en total, 256 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 10 | 11 | 3549 |
| 0.0 | 23 | 24 | 6226 |
| 0.1 | 73 | 73 | 15935 |
| 0.2 | 74 | 75 | 16239 |
| 0.3 | 80 | 81 | 17306 |
| 0.9 | 108 | 112 | 23842 |
| 1.1 | 108 | 112 | 23879 |
| 1.3 | 108 | 112 | 23883 |
| 1.5 | 108 | 112 | 23887 |
| 1.7 | 108 | 112 | 23888 |
| 1.9 | 108 | 112 | 23895 |
| 2.2 | 108 | 112 | 23960 |
| 2.4 | 108 | 112 | 23962 |
| 2.6 | 108 | 112 | 23963 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 179 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 186 |
| `org.hibernate.query.sqm` | 12 |
| `org.apache.catalina.core` | 8 |
| `com.fasterxml.jackson.databind` | 6 |
| `ch.qos.logback.classic` | 4 |
| `org.springframework.data.web` | 3 |
| `org.springframework.transaction.support` | 2 |
| `org.springframework.security.core` | 2 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 16 | 1.3 | 83.93 | 43 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.5 | 258.42 | 17 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 15 | 0.5 | 30.64 | 15 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 16 | 0.2 | 13.04 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 16 | 0.2 | 12.84 | 7 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 7.62 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 15 | 0.0 | 1.65 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 17.55 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 16 | 0.0 | 0.86 | 0 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.64 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 4 | coincide |
| Mascotas | 3 | 3 | coincide |
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
| Registrar propietario | 4 | 30 | 88.2 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 3 | 1 | 25.0 % | 503 | 30 001 | 30 001 |
| Agendar cita | 1 | 2 | 66.7 % | 252 | 30 001 | 30 001 |
| Recepción (llegada) | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Iniciar atención | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 452 | 436 (96 %) | 30 000 | 30 001 | 30 001 | 77 % |
| apoderado | 34 | 30 (88 %) | 30 000 | 30 001 | 30 001 | 5 % |
| cumplimiento | 30 | 14 (47 %) | 278 | 30 001 | 30 001 | 2 % |
| privacidad | 30 | 14 (47 %) | 227 | 30 001 | 30 001 | 2 % |
| panel | 28 | 13 (46 %) | 446 | 30 001 | 30 001 | 2 % |
| navegacion | 30 | 13 (43 %) | 141 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 13 (45 %) | 40 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 28 | 12 (43 %) | 49 | 30 001 | 30 001 | 2 % |
| empleados | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 3 | 3 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 345 | 364 | 0 % |
| 0.2 | 27 | 30 | 3 194 | 3 292 | 3 % |
| 0.8 | 54 | 24 | 30 001 | 30 001 | 100 % |
| 1.0 | 68 | 17 | 30 001 | 30 001 | 100 % |
| 1.2 | 81 | 18 | 30 001 | 30 001 | 100 % |
| 1.5 | 95 | 34 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 38 | 30 001 | 30 002 | 100 % |
| 2.2 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 28 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 4.5 | 96 | 37 | 30 001 | 30 001 | 100 % |
| 4.8 | 75 | 26 | 30 001 | 30 001 | 100 % |
| 5.0 | 48 | 20 | 30 001 | 30 001 | 100 % |
| 5.2 | 25 | 2 | 30 001 | 30 001 | 100 % |
| 5.5 | 4 | 2 | 30 001 | 30 001 | 100 % |
| 5.8 | 3 | 1 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
