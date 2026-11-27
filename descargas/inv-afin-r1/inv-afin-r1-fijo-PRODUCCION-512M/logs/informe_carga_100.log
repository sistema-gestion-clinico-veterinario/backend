# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 11 |
| Peticiones hechas | 1 577 |
| Peticiones correctas (2xx) | 1 022 |
| Errores (%) | 35.2 % |
| Ritmo medio (peticiones/s) | 4.5 |
| Tiempo mediano (ms) | 421 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 53 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 3 |
| Memoria pico (MB, de 512) | 435 |
| Sentencias SQL | 26 099 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 1 577 | 4.5 | no aplica (ramping-vus) | 35.2 % | 554 | 1 / 2 / 4 | 0 % | 106 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 225; fallidas: 108.
- Acciones hechas: panel 51, agenda 39, historia 53, reportes 5, recepcion 131, atencion 15, cobro 6, cancelar 12; sin trabajo disponible: 96.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 45 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 31 avisos en el registro.
- CPU del backend: 58 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 53 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 41 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 435 MB (85 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.7 con unos 100 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **11 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 45 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 57 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 110 a 110 MB (+0.2 %).
- GC: 865 pausas, 164.2 s en total (65 % del tiempo de vida de la JVM); 618 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25068 en total, 216 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 12 | 13 | 3987 |
| 0.1 | 67 | 68 | 15027 |
| 0.2 | 79 | 80 | 17106 |
| 0.9 | 108 | 112 | 23814 |
| 1.2 | 108 | 112 | 24018 |
| 2.0 | 110 | 112 | 24824 |
| 2.3 | 110 | 112 | 24880 |
| 2.6 | 110 | 112 | 24882 |
| 2.9 | 110 | 112 | 24882 |
| 3.1 | 110 | 112 | 24882 |
| 3.4 | 110 | 112 | 24882 |
| 3.6 | 110 | 112 | 24882 |
| 3.9 | 110 | 112 | 24882 |
| 4.2 | 110 | 112 | 24882 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 183 |
| `org.springframework.transaction.support.AbstractPlatformTransactionManager$$Lambda/` | 8 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 186 |
| `org.springframework.transaction.support` | 8 |
| `java.lang.Throwable` | 2 |
| `org.springframework.http.ReadOnlyHttpHeaders` | 2 |
| `org.springframework.dao.support` | 2 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 101 | 1.4 | 13.85 | 26 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 4 | 1.3 | 315.34 | 23 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 101 | 0.9 | 8.92 | 17 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 25 | 0.3 | 13.66 | 6 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 114 | 0.2 | 1.75 | 4 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 101 | 0.1 | 1.25 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 114 | 0.1 | 0.67 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 116 | 0.1 | 0.63 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 48 | 0.1 | 1.29 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 101 | 0.1 | 0.59 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 56 | 57 | la base tiene más (ver nota) |
| Mascotas | 50 | 52 | la base tiene más (ver nota) |
| Citas agendadas | 47 | 47 | coincide |
| Diagnósticos | 9 | 9 | coincide |
| Recetas | 8 | 8 | coincide |
| Pagos | 1 | 1 | coincide |
| Citas completadas | 8 | 8 | coincide |
| Citas canceladas | 6 | 6 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 56 | 75 | 57.3 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 50 | 6 | 10.7 % | 95 | 30 001 | 30 001 |
| Agendar cita | 47 | 3 | 6.0 % | 59 | 7 568 | 30 001 |
| Recepción (llegada) | 11 | 4 | 26.7 % | 2 001 | 30 001 | 30 001 |
| Iniciar atención | 9 | 6 | 40.0 % | 823 | 30 001 | 30 001 |
| Leer la consulta | 9 | 0 | 0.0 % | 19 | 762 | 762 |
| Registrar diagnóstico | 9 | 0 | 0.0 % | 14 | 2 529 | 2 529 |
| Registrar receta | 8 | 1 | 11.1 % | 11 | 30 000 | 30 000 |
| Actualizar mascota | 8 | 1 | 11.1 % | 13 | 30 001 | 30 001 |
| Cerrar la consulta | 8 | 1 | 11.1 % | 38 | 30 000 | 30 000 |
| Caja: crear punto de cobro | 1 | 5 | 83.3 % | 30 000 | 30 001 | 30 001 |
| Caja: vincular equipo | 1 | 0 | 0.0 % | 30 | 30 | 30 |
| Caja: abrir la caja | 1 | 0 | 0.0 % | 31 | 31 | 31 |
| Cobrar | 1 | 0 | 0.0 % | 39 | 39 | 39 |
| Cancelar cita | 6 | 6 | 50.0 % | 17 720 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 186 | 112 (60 %) | 30 000 | 30 001 | 30 001 | 23 % |
| apoderado | 131 | 75 (57 %) | 30 000 | 30 001 | 30 001 | 13 % |
| privacidad | 145 | 42 (29 %) | 159 | 30 001 | 30 001 | 7 % |
| navegacion | 145 | 39 (27 %) | 173 | 30 001 | 30 001 | 7 % |
| cumplimiento | 140 | 37 (26 %) | 128 | 30 001 | 30 001 | 6 % |
| notificaciones | 136 | 35 (26 %) | 92 | 30 001 | 30 001 | 6 % |
| lista_de_historias | 53 | 29 (55 %) | 30 000 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 130 | 29 (22 %) | 65 | 30 001 | 30 001 | 5 % |
| empleados | 42 | 28 (67 %) | 30 000 | 30 001 | 30 001 | 5 % |
| citas_del_mes | 42 | 28 (67 %) | 30 000 | 30 001 | 30 001 | 5 % |
| propietarios | 39 | 27 (69 %) | 30 000 | 30 001 | 30 001 | 5 % |
| panel | 125 | 24 (19 %) | 417 | 30 001 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 77 | 347 | 392 | 0 % |
| 0.2 | 25 | 59 | 3 316 | 5 386 | 0 % |
| 0.5 | 39 | 5 | 7 568 | 7 568 | 0 % |
| 0.7 | 52 | 246 | 23 982 | 29 443 | 0 % |
| 1.0 | 66 | 306 | 417 | 5 267 | 0 % |
| 1.2 | 79 | 283 | 696 | 1 033 | 0 % |
| 1.5 | 93 | 48 | 4 603 | 7 485 | 2 % |
| 1.7 | 100 | 7 | 30 001 | 30 001 | 100 % |
| 2.0 | 100 | 73 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 21 | 30 001 | 30 001 | 100 % |
| 2.5 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 53 | 30 001 | 30 001 | 100 % |
| 3.0 | 100 | 35 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 3.5 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 47 | 30 001 | 30 001 | 100 % |
| 4.0 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.2 | 100 | 42 | 30 001 | 30 001 | 100 % |
| 4.5 | 99 | 39 | 30 001 | 30 001 | 100 % |
| 4.7 | 87 | 47 | 30 001 | 30 001 | 100 % |
| 5.0 | 67 | 18 | 30 001 | 30 001 | 100 % |
| 5.2 | 37 | 21 | 30 001 | 30 001 | 100 % |
| 5.5 | 33 | 8 | 30 001 | 30 001 | 100 % |
| 5.7 | 9 | 3 | 30 000 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
