# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Usuarios sin degradación | 14 |
| Peticiones hechas | 385 |
| Peticiones correctas (2xx) | 125 |
| Errores (%) | 67.5 % |
| Ritmo medio (peticiones/s) | 1.3 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 18 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 430 |
| Sentencias SQL | 3 496 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 385 | 1.3 | no aplica (ramping-vus) | 67.5 % | 259 | 0 / 1 / 2 | 0 % | 72 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 12; fallidas: 29.
- Acciones hechas: panel 6, agenda 2, historia 2, reportes 1, recepcion 25, atencion 1, cobro 0, cancelar 0; sin trabajo disponible: 17.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 8 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 4 avisos en el registro.
- CPU del backend: 18 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 6 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 430 MB (84 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **14 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 8 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 6 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.4 %).
- GC: 396 pausas, 42.0 s en total (24 % del tiempo de vida de la JVM); 174 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24197 en total, 2 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2098 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 23 | 23 | 6147 |
| 0.1 | 60 | 61 | 13568 |
| 0.1 | 74 | 74 | 16077 |
| 0.2 | 75 | 76 | 16396 |
| 0.2 | 79 | 80 | 17210 |
| 0.3 | 100 | 102 | 21197 |
| 0.8 | 108 | 112 | 23838 |
| 1.0 | 108 | 112 | 23921 |
| 1.1 | 108 | 112 | 23932 |
| 1.2 | 108 | 112 | 23933 |
| 1.3 | 108 | 112 | 23935 |
| 1.5 | 109 | 112 | 24195 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `org.springframework.jdbc.core` | 1 |
| `com.zaxxer.hikari.pool` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.5 | 272.29 | 50 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.2 | 12.48 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 7.01 | 10 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.53 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 1.09 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 14.19 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.61 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.72 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 10.68 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 17 | 0.0 | 0.58 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 7 | la base tiene más (ver nota) |
| Mascotas | 4 | 4 | coincide |
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
| Registrar propietario | 5 | 20 | 80.0 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 4 | 1 | 20.0 % | 1 061 | 30 001 | 30 001 |
| Agendar cita | 1 | 3 | 75.0 % | 15 107 | 30 000 | 30 000 |
| Recepción (llegada) | 1 | 0 | 0.0 % | 4 959 | 4 959 | 4 959 |
| Iniciar atención | 1 | 0 | 0.0 % | 29 709 | 29 709 | 29 709 |
| Leer la consulta | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Registrar diagnóstico | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Registrar receta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 168 | 152 (90 %) | 30 000 | 30 001 | 30 001 | 58 % |
| apoderado | 25 | 20 (80 %) | 30 000 | 30 001 | 30 001 | 8 % |
| privacidad | 29 | 13 (45 %) | 34 | 30 001 | 30 001 | 5 % |
| navegacion | 29 | 12 (41 %) | 36 | 30 001 | 30 001 | 5 % |
| notificaciones | 28 | 12 (43 %) | 23 | 30 001 | 30 001 | 5 % |
| cumplimiento | 28 | 12 (43 %) | 29 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 27 | 11 (41 %) | 30 | 30 001 | 30 001 | 4 % |
| panel | 22 | 6 (27 %) | 61 | 30 001 | 30 001 | 2 % |
| empleados | 3 | 3 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 3 | 3 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| mascota | 5 | 1 (20 %) | 1 061 | 30 001 | 30 001 | 1 % |
| cita | 4 | 3 (75 %) | 15 107 | 30 000 | 30 000 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 14 | 100 | 342 | 400 | 0 % |
| 0.3 | 28 | 23 | 4 959 | 6 634 | 4 % |
| 0.5 | 41 | 1 | 30 001 | 30 001 | 100 % |
| 0.8 | 50 | 23 | 30 001 | 30 001 | 87 % |
| 1.0 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 1.3 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 1.5 | 50 | 20 | 30 001 | 30 001 | 100 % |
| 1.8 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 2.0 | 50 | 16 | 30 001 | 30 001 | 100 % |
| 2.3 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 2.5 | 50 | 21 | 30 001 | 30 001 | 100 % |
| 2.8 | 50 | 18 | 30 001 | 30 001 | 100 % |
| 3.0 | 50 | 19 | 30 001 | 30 001 | 100 % |
| 3.3 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 3.5 | 50 | 22 | 30 001 | 30 001 | 100 % |
| 3.8 | 41 | 16 | 30 001 | 30 001 | 100 % |
| 4.0 | 23 | 10 | 30 001 | 30 001 | 100 % |
| 4.3 | 12 | 3 | 30 001 | 30 001 | 100 % |
| 4.5 | 9 | 6 | 30 001 | 30 001 | 100 % |
| 4.8 | 8 | 1 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
