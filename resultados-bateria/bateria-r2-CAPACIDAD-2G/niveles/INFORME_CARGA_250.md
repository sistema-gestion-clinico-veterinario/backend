# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 152 |
| Peticiones hechas | 7 453 |
| Peticiones correctas (2xx) | 5 898 |
| Errores (%) | 20.9 % |
| Ritmo medio (peticiones/s) | 15.2 |
| Tiempo mediano (ms) | 49 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 22 |
| Memoria pico (MB, de 2048) | 1 058 |
| Sentencias SQL | 163 978 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 7 453 | 15.2 | no aplica (ramping-vus) | 20.9 % | 1 463 | 1 / 4 / 8 | 0 % | 220 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1978; fallidas: 241.
- Acciones hechas: panel 312, agenda 271, historia 288, reportes 26, recepcion 394, atencion 165, cobro 84, cancelar 60; sin trabajo disponible: 326.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2558 avisos en el registro.
- CPU del backend: 32 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 22 % de media y 102 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 058 MB (52 % de 2048 MB).
- Los fallos fueron: 94 % sin respuesta (tiempo agotado), 5 % 401 (sesión perdida), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.0 con unos 197 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **152 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 306 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.2 %).
- GC: 308 pausas, 16.5 s en total (3 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 526 MB (tope -Xmx1g).
- Clases cargadas: 25475 en total, 77 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3837 |
| 0.1 | 24 | 24 | 5762 |
| 0.1 | 49 | 50 | 10930 |
| 0.2 | 73 | 74 | 14905 |
| 0.2 | 80 | 81 | 16100 |
| 0.3 | 82 | 83 | 16410 |
| 0.4 | 87 | 88 | 17207 |
| 0.6 | 112 | 113 | 21704 |
| 1.6 | 129 | 132 | 24590 |
| 2.1 | 131 | 134 | 24918 |
| 2.7 | 132 | 135 | 25143 |
| 3.2 | 133 | 136 | 25323 |
| 3.6 | 134 | 137 | 25381 |
| 4.1 | 134 | 137 | 25400 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 31 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 35 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.apache.tomcat.util` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 418 | 78.1 | 186.77 | 35 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and (cast($2 as text) is null or lower(m1_0.nombre_completo) lik` | 418 | 49.5 | 118.5 | 22 % |
| `select count(p1_0.id) from purchases p1_0 left join citas c1_0 on c1_0.id=p1_0.cita_id left join mascota m1_0 on m1_0.id=c1_0.mascota_id where p1_0.company_id=$1 and p1_0.tipo_purchase=$2 and ($3 is null or p1_0.cliente_` | 418 | 26.0 | 62.21 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 46 | 16.6 | 360.57 | 8 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false` | 418 | 11.3 | 26.94 | 5 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and cast(c1_0.fecha_hora_inicio as dat` | 418 | 10.2 | 24.37 | 5 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 418 | 7.2 | 17.14 | 3 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 211 | 6.9 | 32.78 | 3 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 46 | 5.1 | 109.96 | 2 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 623 | 1.5 | 2.41 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 301 | 306 | la base tiene más (ver nota) |
| Mascotas | 298 | 300 | la base tiene más (ver nota) |
| Citas agendadas | 291 | 291 | coincide |
| Diagnósticos | 118 | 118 | coincide |
| Recetas | 117 | 117 | coincide |
| Pagos | 57 | 57 | coincide |
| Citas completadas | 117 | 118 | la base tiene más (ver nota) |
| Citas canceladas | 46 | 48 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 301 | 93 | 23.6 % | 74 | 30 001 | 30 001 |
| Registrar mascota | 298 | 3 | 1.0 % | 37 | 144 | 687 |
| Agendar cita | 291 | 7 | 2.3 % | 54 | 159 | 29 902 |
| Recepción (llegada) | 121 | 44 | 26.7 % | 50 | 30 001 | 30 001 |
| Iniciar atención | 119 | 46 | 27.9 % | 68 | 30 001 | 30 001 |
| Leer la consulta | 119 | 0 | 0.0 % | 29 | 122 | 219 |
| Registrar diagnóstico | 118 | 1 | 0.8 % | 27 | 215 | 674 |
| Registrar receta | 117 | 2 | 1.7 % | 30 | 127 | 30 000 |
| Actualizar mascota | 117 | 2 | 1.7 % | 29 | 150 | 30 000 |
| Cerrar la consulta | 117 | 2 | 1.7 % | 49 | 181 | 30 000 |
| Caja: crear punto de cobro | 53 | 23 | 30.3 % | 39 | 30 001 | 30 001 |
| Caja: vincular equipo | 52 | 1 | 1.9 % | 30 | 214 | 30 001 |
| Caja: abrir la caja | 52 | 0 | 0.0 % | 31 | 185 | 227 |
| Cobrar | 57 | 3 | 5.0 % | 41 | 30 001 | 30 001 |
| Cancelar cita | 46 | 14 | 23.3 % | 56 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 690 | 540 (78 %) | 30 000 | 30 001 | 30 001 | 31 % |
| apoderado | 394 | 93 (24 %) | 74 | 30 001 | 30 001 | 6 % |
| lista_de_historias | 288 | 86 (30 %) | 108 | 30 001 | 30 001 | 6 % |
| navegacion | 505 | 86 (17 %) | 20 | 30 001 | 30 001 | 6 % |
| privacidad | 505 | 85 (17 %) | 17 | 30 001 | 30 001 | 6 % |
| empleados | 284 | 85 (30 %) | 62 | 30 001 | 30 001 | 6 % |
| citas_del_mes | 284 | 84 (30 %) | 64 | 30 001 | 30 001 | 6 % |
| cumplimiento | 495 | 78 (16 %) | 23 | 30 001 | 30 001 | 5 % |
| propietarios | 271 | 74 (27 %) | 178 | 30 001 | 30 001 | 5 % |
| notificaciones | 486 | 69 (14 %) | 16 | 30 001 | 30 001 | 5 % |
| avisos_de_cumplimiento | 480 | 66 (14 %) | 18 | 30 001 | 30 001 | 4 % |
| panel | 462 | 49 (11 %) | 431 | 30 001 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 2 | 18 | 335 | 335 | 0 % |
| 0.3 | 32 | 188 | 481 | 959 | 0 % |
| 0.8 | 62 | 279 | 328 | 910 | 1 % |
| 1.3 | 92 | 387 | 335 | 651 | 0 % |
| 1.8 | 122 | 561 | 489 | 1 383 | 0 % |
| 2.3 | 152 | 662 | 1 046 | 2 125 | 1 % |
| 2.8 | 182 | 635 | 628 | 1 729 | 3 % |
| 3.3 | 212 | 358 | 1 354 | 2 428 | 6 % |
| 4.0 | 250 | 126 | 30 001 | 30 001 | 100 % |
| 4.5 | 250 | 99 | 30 001 | 30 001 | 100 % |
| 5.0 | 250 | 87 | 30 001 | 30 001 | 100 % |
| 5.5 | 250 | 115 | 30 001 | 30 001 | 100 % |
| 6.0 | 250 | 99 | 30 001 | 30 002 | 100 % |
| 6.5 | 250 | 109 | 30 001 | 30 001 | 100 % |
| 7.0 | 201 | 91 | 30 001 | 30 001 | 100 % |
| 7.5 | 103 | 43 | 30 001 | 30 001 | 100 % |
| 8.1 | 29 | 4 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
