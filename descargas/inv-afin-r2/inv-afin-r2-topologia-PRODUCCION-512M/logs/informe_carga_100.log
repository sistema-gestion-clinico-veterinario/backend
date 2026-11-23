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
| Usuarios sin degradación | 9 |
| Peticiones hechas | 740 |
| Peticiones correctas (2xx) | 176 |
| Errores (%) | 76.2 % |
| Ritmo medio (peticiones/s) | 2.1 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 97 |
| Tiempo con la CPU al tope (%) | 95 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 434 |
| Sentencias SQL | 4 737 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 740 | 2.1 | no aplica (ramping-vus) | 76.2 % | 562 | 1 / 2 / 8 | 0 % | 99 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 29; fallidas: 43.
- Acciones hechas: panel 11, agenda 6, historia 7, reportes 1, recepcion 41, atencion 3, cobro 0, cancelar 3; sin trabajo disponible: 25.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 7 errores `OutOfMemoryError` en el registro.
- CPU del backend: 97 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 95 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 5 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 434 MB (85 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.7 con unos 50 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 7 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 8 mensajes; 1 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC (tope configurado: 128 MB).
- **Metaspace estable**: en la segunda mitad pasó de 109 a 109 MB (+0.1 %).
- GC: 1123 pausas, 332.1 s en total (84 % del tiempo de vida de la JVM); 901 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24295 en total, 170 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.1 | 22 | 22 | 5866 |
| 0.3 | 74 | 75 | 16117 |
| 1.1 | 108 | 112 | 23877 |
| 1.7 | 109 | 112 | 24097 |
| 2.2 | 109 | 112 | 24098 |
| 2.7 | 109 | 112 | 24108 |
| 3.1 | 109 | 112 | 24122 |
| 3.6 | 109 | 112 | 24142 |
| 4.1 | 109 | 112 | 24208 |
| 4.6 | 109 | 112 | 24248 |
| 5.1 | 109 | 112 | 24256 |
| 5.6 | 109 | 112 | 24278 |
| 6.1 | 109 | 112 | 24281 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 154 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 160 |
| `ch.qos.logback.classic` | 6 |
| `org.springframework.transaction.support` | 1 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |
| `org.springframework.security.web` | 1 |
| `org.apache.tomcat.util` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 20 | 0.3 | 13.98 | 25 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 1 | 0.2 | 216.57 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 20 | 0.2 | 10.3 | 19 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 20 | 0.0 | 2.24 | 4 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 20 | 0.0 | 1.65 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 20 | 0.0 | 1.09 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 39 | 0.0 | 0.5 | 2 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 20 | 0.0 | 0.94 | 2 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 1 | 0.0 | 18.38 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 21 | 0.0 | 0.83 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 9 | 9 | coincide |
| Mascotas | 8 | 8 | coincide |
| Citas agendadas | 6 | 6 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
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
| Registrar propietario | 9 | 32 | 78.0 % | 30 000 | 30 001 | 30 001 |
| Registrar mascota | 8 | 1 | 11.1 % | 724 | 30 000 | 30 000 |
| Agendar cita | 6 | 2 | 25.0 % | 979 | 6 595 | 6 595 |
| Recepción (llegada) | 1 | 2 | 66.7 % | 30 001 | 30 001 | 30 001 |
| Iniciar atención | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 1 691 | 1 691 | 1 691 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 2 503 | 2 503 | 2 503 |
| Registrar receta | 1 | 0 | 0.0 % | 3 856 | 3 856 | 3 856 |
| Actualizar mascota | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cerrar la consulta | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |
| Cancelar cita | 1 | 2 | 66.7 % | 30 000 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 436 | 417 (96 %) | 30 000 | 30 001 | 30 001 | 74 % |
| apoderado | 41 | 32 (78 %) | 30 000 | 30 001 | 30 001 | 6 % |
| privacidad | 35 | 14 (40 %) | 536 | 30 001 | 30 001 | 3 % |
| notificaciones | 34 | 14 (41 %) | 477 | 30 001 | 30 002 | 2 % |
| cumplimiento | 34 | 14 (41 %) | 542 | 30 001 | 30 001 | 2 % |
| navegacion | 35 | 13 (37 %) | 158 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 32 | 12 (38 %) | 373 | 30 001 | 30 001 | 2 % |
| panel | 30 | 11 (37 %) | 988 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 7 | 6 (86 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 7 | 6 (86 %) | 30 000 | 30 002 | 30 002 | 1 % |
| propietarios | 6 | 5 (83 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 63 | 462 | 513 | 0 % |
| 0.2 | 23 | 78 | 1 937 | 5 951 | 1 % |
| 0.4 | 37 | 34 | 10 755 | 19 778 | 3 % |
| 0.7 | 50 | 8 | 30 001 | 30 001 | 62 % |
| 0.9 | 64 | 18 | 30 001 | 30 001 | 100 % |
| 1.2 | 78 | 27 | 30 001 | 30 001 | 100 % |
| 1.4 | 91 | 21 | 30 001 | 30 001 | 100 % |
| 1.7 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 2.2 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 2.4 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.7 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 3.2 | 100 | 40 | 30 001 | 30 002 | 100 % |
| 3.4 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 3.7 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 29 | 30 001 | 30 002 | 100 % |
| 4.2 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 4.4 | 99 | 38 | 30 001 | 30 003 | 100 % |
| 4.7 | 84 | 26 | 30 001 | 30 001 | 100 % |
| 4.9 | 53 | 26 | 30 001 | 30 001 | 100 % |
| 5.2 | 31 | 7 | 30 001 | 30 001 | 100 % |
| 5.4 | 8 | 4 | 30 001 | 30 001 | 100 % |
| 5.7 | 7 | 1 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
