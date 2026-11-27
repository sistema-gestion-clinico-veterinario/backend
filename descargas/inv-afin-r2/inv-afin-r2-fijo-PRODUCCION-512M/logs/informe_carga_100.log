# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 734 |
| Peticiones correctas (2xx) | 172 |
| Errores (%) | 76.6 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 23 |
| Tiempo con la CPU al tope (%) | 20 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 5 590 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 734 | 2.1 | no aplica (ramping-vus) | 76.6 % | 561 | 1 / 1 / 6 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 27; fallidas: 42.
- Acciones hechas: panel 12, agenda 6, historia 7, reportes 1, recepcion 41, atencion 2, cobro 0, cancelar 3; sin trabajo disponible: 26.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 5 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 6 avisos en el registro.
- CPU del backend: 23 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 20 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 10 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.6 con unos 43 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **16 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 5 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 11 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.7 %).
- GC: 394 pausas, 58.4 s en total (47 % del tiempo de vida de la JVM); 171 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24370 en total, 496 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2041 |
| 0.0 | 11 | 11 | 3772 |
| 0.1 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12294 |
| 0.2 | 74 | 74 | 16075 |
| 0.2 | 74 | 75 | 16116 |
| 0.3 | 79 | 80 | 17106 |
| 0.4 | 83 | 84 | 17756 |
| 1.0 | 108 | 112 | 23826 |
| 1.2 | 109 | 112 | 24063 |
| 1.4 | 109 | 112 | 24107 |
| 1.6 | 109 | 112 | 24109 |
| 1.7 | 109 | 112 | 24110 |
| 1.9 | 109 | 112 | 24110 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 272 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `java.lang.invoke.LambdaForm$DMH/` | 14 |
| `veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl$$Lambda/` | 11 |
| `java.lang.invoke.LambdaForm$BMH/` | 7 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.impl.ControlPreventivoServiceImpl$$Lambda/` | 5 |
| `jdk.proxy#.$Proxy#` | 4 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 293 |
| `veterinaria.vargasvet.service.impl` | 27 |
| `veterinaria.vargasvet.service.reportes` | 21 |
| `org.hibernate.query.sql` | 16 |
| `org.hibernate.query.sqm` | 12 |
| `org.springframework.data.repository` | 10 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 7 |
| `org.apache.catalina.core` | 6 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.8 | 390.38 | 43 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 20 | 0.4 | 18.76 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 20 | 0.2 | 11.49 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 21 | 0.0 | 2.0 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 20 | 0.0 | 1.56 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 42 | 0.0 | 0.56 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 22.82 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 22.14 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 21 | 0.0 | 0.95 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 18.2 | 1 % |

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
| Citas canceladas | 0 | 2 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 8 | 33 | 80.5 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 0 | 0.0 % | 547 | 3 794 | 3 794 |
| Agendar cita | 6 | 2 | 25.0 % | 1 192 | 30 001 | 30 001 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 15 253 | 30 001 | 30 001 |
| Iniciar atención | 1 | 1 | 50.0 % | 15 811 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 1 855 | 1 855 | 1 855 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 867 | 2 867 | 2 867 |
| Registrar receta | 1 | 0 | 0.0 % | 10 220 | 10 220 | 10 220 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 023 | 30 023 | 30 023 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 0 | 3 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 436 | 417 (96 %) | 30 000 | 30 001 | 30 001 | 74 % |
| apoderado | 41 | 33 (80 %) | 30 000 | 30 001 | 30 001 | 6 % |
| cumplimiento | 34 | 14 (41 %) | 520 | 30 001 | 30 001 | 2 % |
| privacidad | 34 | 14 (41 %) | 107 | 30 001 | 30 001 | 2 % |
| navegacion | 34 | 13 (38 %) | 380 | 30 001 | 30 001 | 2 % |
| notificaciones | 33 | 13 (39 %) | 110 | 30 001 | 30 001 | 2 % |
| panel | 31 | 12 (39 %) | 659 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 31 | 11 (35 %) | 392 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 7 | 6 (86 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 001 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 14 | 504 | 504 | 0 % |
| 0.1 | 16 | 90 | 462 | 941 | 0 % |
| 0.3 | 29 | 63 | 3 522 | 10 287 | 2 % |
| 0.6 | 43 | 7 | 30 001 | 30 001 | 14 % |
| 0.8 | 57 | 11 | 30 001 | 30 001 | 100 % |
| 1.1 | 70 | 31 | 30 001 | 30 023 | 100 % |
| 1.3 | 84 | 15 | 30 001 | 30 001 | 100 % |
| 1.6 | 98 | 38 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 33 | 30 001 | 30 002 | 100 % |
| 2.3 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 38 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 4.5 | 92 | 33 | 30 001 | 30 001 | 100 % |
| 4.8 | 69 | 26 | 30 001 | 30 001 | 100 % |
| 5.0 | 39 | 15 | 30 001 | 30 001 | 100 % |
| 5.3 | 17 | 4 | 30 001 | 30 001 | 100 % |
| 5.5 | 6 | 2 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
