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
| Usuarios sin degradación | 15 |
| Peticiones hechas | 696 |
| Peticiones correctas (2xx) | 138 |
| Errores (%) | 80.2 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 17 |
| Tiempo con la CPU al tope (%) | 14 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 421 |
| Sentencias SQL | 3 905 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 696 | 2.0 | no aplica (ramping-vus) | 80.2 % | 558 | 0 / 1 / 2 | 0 % | 101 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 16; fallidas: 36.
- Acciones hechas: panel 11, agenda 4, historia 4, reportes 1, recepcion 35, atencion 2, cobro 0, cancelar 2; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 12 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 30 avisos en el registro.
- CPU del backend: 17 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 14 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 18 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 421 MB (82 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 56 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **15 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 12 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 8 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.5 %).
- GC: 377 pausas, 46.1 s en total (35 % del tiempo de vida de la JVM); 156 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24207 en total, 1005 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 5 | 1922 |
| 0.0 | 11 | 11 | 3767 |
| 0.0 | 21 | 21 | 5688 |
| 0.1 | 46 | 47 | 11097 |
| 0.1 | 73 | 74 | 16044 |
| 0.2 | 74 | 75 | 16100 |
| 0.2 | 79 | 80 | 17082 |
| 0.3 | 79 | 80 | 17226 |
| 0.8 | 106 | 108 | 22920 |
| 1.0 | 108 | 112 | 23948 |
| 1.1 | 108 | 112 | 23986 |
| 1.3 | 108 | 112 | 23987 |
| 1.4 | 108 | 112 | 23990 |
| 1.5 | 108 | 112 | 23990 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 364 |
| `java.lang.invoke.LambdaForm$DMH/` | 17 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 15 |
| `veterinaria.vargasvet.service.PetOwnershipService$$Lambda/` | 8 |
| `java.lang.invoke.LambdaForm$BMH/` | 6 |
| `org.thymeleaf.util.ExpressionUtils$$Lambda/` | 6 |
| `org.hibernate.internal.SessionImpl$$Lambda/` | 4 |
| `org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation$$Lambda/` | 3 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 3 |
| `org.springframework.security.core.context.ThreadLocalSecurityContextHolderStrategy$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 387 |
| `org.thymeleaf.standard.expression` | 49 |
| `org.eclipse.angus.mail` | 32 |
| `veterinaria.vargasvet.service.impl` | 20 |
| `org.thymeleaf.spring6.processor` | 19 |
| `org.thymeleaf.spring6.expression` | 19 |
| `org.hibernate.query.sql` | 17 |
| `org.thymeleaf.standard.processor` | 10 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 17 | 1.1 | 65.71 | 39 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.6 | 291.25 | 20 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 17 | 0.4 | 24.67 | 14 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 17 | 0.2 | 10.88 | 6 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 17 | 0.2 | 10.72 | 6 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 17 | 0.1 | 7.06 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 17 | 0.0 | 1.59 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 17 | 0.0 | 0.98 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 14.02 | 0 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 12.79 | 0 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 6 | 8 | la base tiene más (ver nota) |
| Mascotas | 5 | 5 | coincide |
| Citas agendadas | 4 | 5 | la base tiene más (ver nota) |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
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
| Registrar propietario | 6 | 29 | 82.9 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 5 | 1 | 16.7 % | 935 | 30 000 | 30 000 |
| Agendar cita | 4 | 1 | 20.0 % | 985 | 30 001 | 30 001 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 27 751 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 1 | 1 | 50.0 % | 28 753 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 001 | 30 001 | 30 001 | 77 % |
| apoderado | 35 | 29 (83 %) | 30 000 | 30 001 | 30 001 | 5 % |
| privacidad | 31 | 13 (42 %) | 337 | 30 001 | 30 004 | 2 % |
| navegacion | 31 | 13 (42 %) | 48 | 30 001 | 30 001 | 2 % |
| panel | 28 | 12 (43 %) | 574 | 30 001 | 30 001 | 2 % |
| cumplimiento | 30 | 12 (40 %) | 389 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 12 (41 %) | 58 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 12 (41 %) | 42 | 30 001 | 30 001 | 2 % |
| empleados | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| lista_de_historias | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 3 (75 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 7 | 347 | 347 | 0 % |
| 0.0 | 15 | 93 | 363 | 802 | 0 % |
| 0.3 | 29 | 36 | 3 211 | 4 922 | 0 % |
| 0.5 | 42 | 1 | 30 000 | 30 000 | 100 % |
| 0.8 | 56 | 22 | 30 001 | 30 001 | 100 % |
| 1.0 | 69 | 20 | 30 001 | 30 001 | 90 % |
| 1.3 | 83 | 21 | 30 001 | 30 001 | 100 % |
| 1.5 | 97 | 35 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 27 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 40 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 37 | 30 001 | 30 004 | 100 % |
| 2.8 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 4.5 | 95 | 35 | 30 001 | 30 001 | 100 % |
| 4.8 | 71 | 27 | 30 001 | 30 001 | 100 % |
| 5.0 | 40 | 13 | 30 001 | 30 001 | 100 % |
| 5.3 | 17 | 1 | 30 001 | 30 001 | 100 % |
| 5.5 | 3 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
