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
| Usuarios sin degradación | 14 |
| Peticiones hechas | 703 |
| Peticiones correctas (2xx) | 143 |
| Errores (%) | 79.7 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 25 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 1 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 4 377 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 703 | 2.0 | no aplica (ramping-vus) | 79.7 % | 560 | 0 / 1 / 2 | 0 % | 99 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 15; fallidas: 39.
- Acciones hechas: panel 12, agenda 4, historia 5, reportes 1, recepcion 38, atencion 2, cobro 0, cancelar 2; sin trabajo disponible: 24.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 18 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 3 avisos en el registro.
- CPU del backend: 29 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 25 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 18 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 55 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **14 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 18 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 7 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 109 MB (+0.6 %).
- GC: 515 pausas, 86.0 s en total (29 % del tiempo de vida de la JVM); 292 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24262 en total, 4 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 7 | 7 | 2731 |
| 0.0 | 18 | 18 | 5064 |
| 0.1 | 53 | 54 | 12515 |
| 0.2 | 74 | 75 | 16103 |
| 0.2 | 79 | 80 | 17088 |
| 0.3 | 85 | 86 | 18044 |
| 0.9 | 108 | 112 | 23917 |
| 1.1 | 108 | 112 | 24013 |
| 1.3 | 108 | 112 | 24018 |
| 1.5 | 108 | 112 | 24018 |
| 1.6 | 108 | 112 | 24021 |
| 1.8 | 108 | 112 | 24021 |
| 2.0 | 108 | 112 | 24021 |
| 2.2 | 108 | 112 | 24021 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 2 |
| `org.springframework.jdbc.core` | 1 |
| `com.zaxxer.hikari.pool` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.5 | 263.14 | 44 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 18 | 0.3 | 13.98 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 18 | 0.1 | 7.02 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 18 | 0.0 | 1.59 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 18 | 0.0 | 1.11 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 18.01 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 18 | 0.0 | 0.84 | 1 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 14.86 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.12 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 19 | 0.0 | 0.64 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 8 | la base tiene más (ver nota) |
| Mascotas | 5 | 5 | coincide |
| Citas agendadas | 4 | 4 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
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
| Registrar propietario | 5 | 33 | 86.8 % | 30 001 | 30 001 | 30 001 |
| Registrar mascota | 5 | 0 | 0.0 % | 494 | 2 247 | 2 247 |
| Agendar cita | 4 | 1 | 20.0 % | 765 | 30 001 | 30 001 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 16 112 | 30 001 | 30 001 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 001 | 30 001 | 30 001 | 76 % |
| apoderado | 38 | 33 (87 %) | 30 001 | 30 001 | 30 001 | 6 % |
| cumplimiento | 31 | 13 (42 %) | 376 | 30 001 | 30 001 | 2 % |
| panel | 29 | 13 (45 %) | 471 | 30 001 | 30 001 | 2 % |
| privacidad | 31 | 12 (39 %) | 42 | 30 001 | 30 001 | 2 % |
| navegacion | 31 | 12 (39 %) | 49 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 11 (38 %) | 54 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 11 (38 %) | 48 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 5 | 4 (80 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 5 | 4 (80 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 5 | 4 (80 %) | 30 000 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 14 | 92 | 346 | 387 | 0 % |
| 0.3 | 28 | 51 | 3 727 | 6 059 | 0 % |
| 0.5 | 41 | 1 | 30 001 | 30 001 | 100 % |
| 0.8 | 55 | 21 | 30 001 | 30 001 | 100 % |
| 1.0 | 69 | 20 | 30 001 | 30 001 | 100 % |
| 1.3 | 82 | 16 | 30 001 | 30 001 | 100 % |
| 1.5 | 96 | 36 | 30 001 | 30 001 | 100 % |
| 1.8 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 2.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 2.8 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.3 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 3.8 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 27 | 30 001 | 30 001 | 100 % |
| 4.3 | 100 | 41 | 30 001 | 30 001 | 100 % |
| 4.5 | 96 | 35 | 30 001 | 30 001 | 100 % |
| 4.8 | 72 | 25 | 30 001 | 30 001 | 100 % |
| 5.0 | 44 | 16 | 30 001 | 30 001 | 100 % |
| 5.3 | 21 | 2 | 30 001 | 30 001 | 100 % |
| 5.5 | 5 | 3 | 30 001 | 30 001 | 100 % |
| 5.8 | 3 | 1 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
