# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 256m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 256m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 256m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-256M (experimental: -Xmx256m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 47 |
| Peticiones hechas | 2 557 |
| Peticiones correctas (2xx) | 2 069 |
| Errores (%) | 19.1 % |
| Ritmo medio (peticiones/s) | 7.3 |
| Tiempo mediano (ms) | 163 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 91 |
| Tiempo con la CPU al tope (%) | 83 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 506 |
| Sentencias SQL | 58 157 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 2 557 | 7.3 | no aplica (ramping-vus) | 19.1 % | 319 | 1 / 3 / 4 | 0 % | 114 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 730; fallidas: 63.
- Acciones hechas: panel 94, agenda 83, historia 81, reportes 7, recepcion 129, atencion 52, cobro 29, cancelar 24; sin trabajo disponible: 101.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 14 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 81 avisos en el registro.
- CPU del backend: 91 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 83 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 28 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 506 MB (99 % de 512 MB).
- Los fallos fueron: 65 % sin respuesta (tiempo agotado), 34 % 401 (sesión perdida), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.1 con unos 74 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **47 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 14 errores `OutOfMemoryError: Java heap space`.
- Correo: el servidor de pruebas recibió 104 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.2 %).
- GC: 742 pausas, 197.0 s en total (50 % del tiempo de vida de la JVM); 490 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 247 MB (tope -Xmx256m).
- Clases cargadas: 25647 en total, 487 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 11 | 11 | 3772 |
| 0.1 | 50 | 51 | 11671 |
| 0.2 | 74 | 75 | 16115 |
| 0.7 | 106 | 108 | 22807 |
| 2.2 | 110 | 115 | 24850 |
| 3.5 | 111 | 115 | 25160 |
| 4.0 | 111 | 115 | 25191 |
| 4.3 | 111 | 115 | 25192 |
| 4.7 | 111 | 115 | 25254 |
| 5.0 | 111 | 115 | 25469 |
| 5.4 | 111 | 115 | 25577 |
| 5.8 | 111 | 115 | 25615 |
| 6.1 | 111 | 115 | 25641 |
| 6.5 | 111 | 115 | 25644 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 426 |
| `org.springframework.dao.support.PersistenceExceptionTranslationInterceptor$$Lambda/` | 17 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 431 |
| `org.springframework.dao.support` | 18 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `java.lang.Throwable` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 11 | 2.8 | 258.81 | 22 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 139 | 2.4 | 17.35 | 18 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 73 | 1.4 | 19.86 | 11 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 139 | 1.4 | 10.18 | 11 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 213 | 0.6 | 2.81 | 5 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 10 | 0.5 | 51.71 | 4 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 455 | 0.3 | 0.67 | 2 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 404 | 0.3 | 0.67 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 215 | 0.3 | 1.18 | 2 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 139 | 0.2 | 1.64 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 104 | 104 | coincide |
| Mascotas | 103 | 103 | coincide |
| Citas agendadas | 100 | 100 | coincide |
| Diagnósticos | 45 | 45 | coincide |
| Recetas | 42 | 44 | la base tiene más (ver nota) |
| Pagos | 22 | 22 | coincide |
| Citas completadas | 42 | 42 | coincide |
| Citas canceladas | 21 | 22 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 104 | 25 | 19.4 % | 333 | 30 001 | 30 001 |
| Registrar mascota | 103 | 1 | 1.0 % | 86 | 947 | 9 837 |
| Agendar cita | 100 | 3 | 2.9 % | 137 | 1 131 | 30 001 |
| Recepción (llegada) | 48 | 4 | 7.7 % | 181 | 30 001 | 30 001 |
| Iniciar atención | 46 | 6 | 11.5 % | 257 | 30 001 | 30 001 |
| Leer la consulta | 45 | 1 | 2.2 % | 153 | 1 484 | 30 001 |
| Registrar diagnóstico | 45 | 1 | 2.2 % | 159 | 1 766 | 30 000 |
| Registrar receta | 42 | 4 | 8.7 % | 165 | 30 000 | 30 001 |
| Actualizar mascota | 42 | 4 | 8.7 % | 172 | 30 000 | 30 001 |
| Cerrar la consulta | 42 | 4 | 8.7 % | 328 | 30 001 | 30 001 |
| Caja: crear punto de cobro | 25 | 2 | 7.4 % | 215 | 30 000 | 30 001 |
| Caja: vincular equipo | 24 | 1 | 4.0 % | 204 | 6 123 | 30 000 |
| Caja: abrir la caja | 21 | 3 | 12.5 % | 212 | 30 001 | 30 001 |
| Cobrar | 22 | 1 | 4.3 % | 242 | 1 510 | 30 001 |
| Cancelar cita | 21 | 3 | 12.5 % | 124 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 343 | 293 (85 %) | 9 702 | 30 001 | 30 001 | 44 % |
| apoderado | 129 | 25 (19 %) | 333 | 30 001 | 30 001 | 7 % |
| privacidad | 161 | 21 (13 %) | 43 | 30 001 | 30 001 | 6 % |
| navegacion | 161 | 17 (11 %) | 81 | 30 000 | 30 001 | 5 % |
| cumplimiento | 156 | 16 (10 %) | 49 | 30 000 | 30 001 | 4 % |
| notificaciones | 151 | 12 (8 %) | 29 | 30 000 | 30 001 | 3 % |
| citas_del_mes | 85 | 11 (13 %) | 361 | 30 001 | 30 001 | 3 % |
| propietarios | 83 | 10 (12 %) | 451 | 30 001 | 30 001 | 3 % |
| empleados | 85 | 11 (13 %) | 161 | 30 001 | 30 001 | 3 % |
| avisos_de_cumplimiento | 148 | 9 (6 %) | 30 | 30 000 | 30 001 | 2 % |
| lista_de_historias | 81 | 8 (10 %) | 171 | 30 001 | 30 001 | 2 % |
| panel | 144 | 5 (3 %) | 159 | 2 887 | 30 001 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 445 | 469 | 0 % |
| 0.1 | 19 | 91 | 1 316 | 2 510 | 0 % |
| 0.4 | 33 | 190 | 560 | 2 900 | 0 % |
| 0.6 | 47 | 233 | 426 | 463 | 0 % |
| 0.9 | 60 | 197 | 445 | 2 408 | 5 % |
| 1.1 | 74 | 187 | 938 | 6 418 | 7 % |
| 1.4 | 88 | 191 | 1 959 | 5 687 | 9 % |
| 1.6 | 100 | 234 | 9 628 | 10 162 | 9 % |
| 1.9 | 100 | 221 | 7 856 | 8 649 | 17 % |
| 2.1 | 100 | 236 | 8 564 | 10 615 | 8 % |
| 2.4 | 100 | 198 | 9 255 | 10 345 | 13 % |
| 2.6 | 100 | 150 | 12 614 | 16 464 | 10 % |
| 2.9 | 100 | 67 | 22 842 | 30 001 | 18 % |
| 3.1 | 100 | 23 | 30 001 | 30 001 | 87 % |
| 3.4 | 100 | 56 | 30 001 | 30 001 | 100 % |
| 3.6 | 100 | 27 | 30 001 | 30 002 | 100 % |
| 3.9 | 100 | 34 | 30 001 | 30 001 | 100 % |
| 4.1 | 100 | 43 | 30 001 | 30 001 | 100 % |
| 4.4 | 100 | 33 | 30 001 | 30 001 | 100 % |
| 4.6 | 86 | 39 | 30 001 | 30 001 | 100 % |
| 4.9 | 71 | 27 | 30 001 | 30 001 | 100 % |
| 5.1 | 48 | 17 | 30 001 | 30 001 | 100 % |
| 5.4 | 31 | 14 | 30 001 | 30 001 | 100 % |
| 5.6 | 19 | 7 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
