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
| Usuarios sin degradación | 19 |
| Peticiones hechas | 692 |
| Peticiones correctas (2xx) | 129 |
| Errores (%) | 81.4 % |
| Ritmo medio (peticiones/s) | 2.0 |
| Tiempo mediano (ms) | 30 000 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 98 |
| Tiempo con la CPU al tope (%) | 95 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 0 |
| Memoria pico (MB, de 512) | 433 |
| Sentencias SQL | 3 251 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 692 | 2.0 | no aplica (ramping-vus) | 81.4 % | 563 | 0 / 1 / 2 | 0 % | 100 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 12; fallidas: 34.
- Acciones hechas: panel 12, agenda 4, historia 5, reportes 1, recepcion 33, atencion 2, cobro 0, cancelar 1; sin trabajo disponible: 22.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 19 errores `OutOfMemoryError` en el registro.
- CPU del backend: 98 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 95 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 7 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 433 MB (85 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 59 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **19 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 19 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 3.
- Correo: el servidor de pruebas recibió 4 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 109 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 108 a 108 MB (+0.1 %).
- GC: 1326 pausas, 338.2 s en total (88 % del tiempo de vida de la JVM); 1106 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24087 en total, 92 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.1 | 37 | 38 | 9251 |
| 0.3 | 79 | 80 | 17208 |
| 1.1 | 108 | 112 | 23970 |
| 1.6 | 108 | 112 | 23979 |
| 2.1 | 108 | 112 | 23981 |
| 2.5 | 108 | 112 | 23985 |
| 3.0 | 108 | 112 | 23990 |
| 3.5 | 108 | 112 | 24007 |
| 4.0 | 108 | 112 | 24030 |
| 4.4 | 108 | 112 | 24044 |
| 4.9 | 108 | 112 | 24056 |
| 5.4 | 108 | 112 | 24064 |
| 5.9 | 108 | 112 | 24068 |
| 6.4 | 108 | 112 | 24085 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 75 |
| `org.hibernate.internal.SessionImpl$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 76 |
| `org.hibernate.internal.SessionImpl` | 4 |
| `org.springframework.transaction.support` | 2 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.web.servlet` | 1 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |
| `ch.qos.logback.classic` | 1 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 16 | 0.2 | 13.82 | 32 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 1 | 0.2 | 170.9 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 16 | 0.1 | 7.12 | 16 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 16 | 0.0 | 1.62 | 4 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 16 | 0.0 | 1.17 | 3 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 14.44 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 16 | 0.0 | 0.78 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 12.54 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 27 | 0.0 | 0.39 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 16 | 0.0 | 0.6 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 4 | coincide |
| Mascotas | 4 | 4 | coincide |
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
| Registrar propietario | 4 | 29 | 87.9 % | 30 001 | 30 001 | 30 001 |
| Registrar mascota | 4 | 0 | 0.0 % | 333 | 5 095 | 5 095 |
| Agendar cita | 3 | 1 | 25.0 % | 858 | 30 001 | 30 001 |
| Recepción (llegada) | 1 | 1 | 50.0 % | 20 258 | 30 000 | 30 000 |
| Iniciar atención | 0 | 2 | 100.0 % | 30 001 | 30 001 | 30 001 |
| Cancelar cita | 0 | 1 | 100.0 % | 30 000 | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 446 | 429 (96 %) | 30 001 | 30 001 | 30 001 | 76 % |
| apoderado | 33 | 29 (88 %) | 30 001 | 30 001 | 30 001 | 5 % |
| cumplimiento | 31 | 14 (45 %) | 72 | 30 001 | 30 001 | 3 % |
| privacidad | 31 | 14 (45 %) | 56 | 30 001 | 30 001 | 2 % |
| navegacion | 31 | 13 (42 %) | 382 | 30 001 | 30 001 | 2 % |
| panel | 29 | 13 (45 %) | 496 | 30 001 | 30 001 | 2 % |
| notificaciones | 29 | 13 (45 %) | 82 | 30 001 | 30 001 | 2 % |
| avisos_de_cumplimiento | 29 | 13 (45 %) | 55 | 30 001 | 30 001 | 2 % |
| lista_de_historias | 5 | 5 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| citas_del_mes | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |
| empleados | 4 | 4 (100 %) | 30 001 | 30 001 | 30 001 | 1 % |
| propietarios | 4 | 4 (100 %) | 30 000 | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 5 | 35 | 354 | 368 | 0 % |
| 0.1 | 19 | 84 | 483 | 3 119 | 0 % |
| 0.3 | 32 | 10 | 10 516 | 10 516 | 0 % |
| 0.6 | 46 | 3 | 30 001 | 30 001 | 100 % |
| 0.8 | 59 | 26 | 30 001 | 30 001 | 100 % |
| 1.1 | 73 | 19 | 30 001 | 30 001 | 100 % |
| 1.4 | 87 | 22 | 30 001 | 30 001 | 100 % |
| 1.6 | 100 | 36 | 30 001 | 30 001 | 100 % |
| 1.9 | 100 | 32 | 30 001 | 30 001 | 100 % |
| 2.1 | 100 | 39 | 30 001 | 30 001 | 100 % |
| 2.4 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 2.6 | 100 | 31 | 30 001 | 30 001 | 100 % |
| 2.9 | 100 | 37 | 30 001 | 30 001 | 100 % |
| 3.1 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.4 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 3.6 | 100 | 44 | 30 001 | 30 001 | 100 % |
| 3.9 | 100 | 29 | 30 001 | 30 001 | 100 % |
| 4.1 | 100 | 37 | 30 001 | 30 003 | 100 % |
| 4.3 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.6 | 90 | 34 | 30 001 | 30 001 | 100 % |
| 4.8 | 61 | 27 | 30 001 | 30 002 | 100 % |
| 5.1 | 37 | 8 | 30 001 | 30 001 | 100 % |
| 5.6 | 3 | 3 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
