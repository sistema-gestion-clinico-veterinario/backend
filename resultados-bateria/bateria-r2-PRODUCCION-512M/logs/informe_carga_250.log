# Fase 3: 250 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Peticiones hechas | 1 906 |
| Peticiones correctas (2xx) | 140 |
| Errores (%) | 92.7 % |
| Ritmo medio (peticiones/s) | 3.9 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 15 |
| Tiempo con la CPU al tope (%) | 13 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 425 |
| Sentencias SQL | 3 719 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 1 906 | 3.9 | no aplica (ramping-vus) | 92.7 % | 1 766 | 1 / 1 / 4 | 0 % | 178 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 11; fallidas: 55.
- Acciones hechas: panel 15, agenda 11, historia 11, reportes 1, recepcion 53, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 34.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 17 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 41 avisos en el registro.
- CPU del backend: 15 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 13 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 34 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 425 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 56 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **11 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 17 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 7 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.5 %).
- GC: 437 pausas, 63.9 s en total (58 % del tiempo de vida de la JVM); 218 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24150 en total, 274 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 32 | 33 | 8222 |
| 0.1 | 69 | 70 | 15527 |
| 0.2 | 74 | 75 | 16100 |
| 0.3 | 79 | 80 | 17100 |
| 0.3 | 87 | 89 | 18338 |
| 0.9 | 108 | 112 | 23862 |
| 1.1 | 108 | 112 | 23987 |
| 1.2 | 108 | 112 | 23993 |
| 1.4 | 108 | 112 | 23996 |
| 1.5 | 108 | 112 | 23998 |
| 1.7 | 108 | 112 | 23998 |
| 1.8 | 109 | 112 | 24145 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 173 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |
| `org.hibernate.internal.SessionImpl$$Lambda/` | 4 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |
| `org.springframework.security.core.context.ThreadLocalSecurityContextHolderStrategy$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 179 |
| `com.fasterxml.jackson.databind` | 6 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.query.sql` | 5 |
| `org.hibernate.internal.SessionImpl` | 4 |
| `org.springframework.data.web` | 3 |
| `org.hibernate.dialect.ColumnAliasExtractor` | 3 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 18 | 1.5 | 82.02 | 46 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 283.91 | 18 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 18 | 0.4 | 24.23 | 14 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2 and cast(c1_0.fecha_hora_inicio as date)=` | 18 | 0.2 | 10.08 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$2` | 18 | 0.2 | 10.03 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ($4` | 18 | 0.1 | 6.94 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 18 | 0.0 | 1.58 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 18 | 0.0 | 0.95 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//$6//u1_0.apellido) as text)) like lower(cast(($7//replace(cast($3 a` | 18 | 0.0 | 0.77 | 0 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.72 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 7 | la base tiene más (ver nota) |
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
| Registrar propietario | 5 | 48 | 90.6 % | 30 001 | 30 001 | 30 018 |
| Registrar mascota | 3 | 2 | 40.0 % | 779 | 30 001 | 30 001 |
| Agendar cita | 3 | 0 | 0.0 % | 193 | 986 | 986 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 1571 | 1553 (99 %) | 30 000 | 30 001 | 30 001 | 88 % |
| apoderado | 53 | 48 (91 %) | 30 001 | 30 001 | 30 018 | 3 % |
| navegacion | 40 | 21 (52 %) | 30 000 | 30 001 | 30 001 | 1 % |
| privacidad | 40 | 21 (52 %) | 30 000 | 30 001 | 30 001 | 1 % |
| cumplimiento | 37 | 18 (49 %) | 2 400 | 30 001 | 30 001 | 1 % |
| notificaciones | 35 | 17 (49 %) | 1 225 | 30 001 | 30 001 | 1 % |
| panel | 33 | 16 (48 %) | 913 | 30 001 | 30 001 | 1 % |
| avisos_de_cumplimiento | 34 | 16 (47 %) | 806 | 30 001 | 30 001 | 1 % |
| empleados | 12 | 12 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 12 | 11 (92 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 11 | 11 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 11 | 11 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 83 | 345 | 363 | 0 % |
| 0.5 | 41 | 1 | 7 523 | 7 523 | 0 % |
| 0.9 | 71 | 22 | 30 001 | 30 001 | 100 % |
| 1.4 | 101 | 34 | 30 001 | 30 001 | 100 % |
| 1.9 | 131 | 38 | 30 001 | 30 001 | 100 % |
| 2.5 | 161 | 55 | 30 001 | 30 018 | 100 % |
| 3.0 | 191 | 56 | 30 001 | 30 001 | 100 % |
| 3.5 | 221 | 72 | 30 001 | 30 002 | 100 % |
| 4.0 | 250 | 74 | 30 001 | 30 001 | 100 % |
| 4.5 | 250 | 84 | 30 001 | 30 001 | 100 % |
| 5.0 | 250 | 93 | 30 001 | 30 001 | 100 % |
| 5.5 | 250 | 73 | 30 001 | 30 001 | 100 % |
| 6.0 | 250 | 81 | 30 001 | 30 001 | 100 % |
| 6.5 | 250 | 91 | 30 001 | 30 001 | 100 % |
| 7.0 | 214 | 72 | 30 001 | 30 001 | 100 % |
| 7.5 | 71 | 13 | 30 001 | 30 001 | 100 % |
| 8.0 | 8 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
