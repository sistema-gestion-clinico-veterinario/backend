# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 250 |
| Peticiones hechas | 20 007 |
| Peticiones correctas (2xx) | 19 988 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.6 |
| Tiempo mediano (ms) | 22 |
| Tiempo p95 (ms) | 220 |
| Tiempo p99 (ms) | 799 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 59 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 38 |
| Memoria pico (MB, de 2048) | 1 298 |
| Sentencias SQL | 542 607 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 20 007 | 44.6 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 285 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7303; fallidas: 19.
- Acciones hechas: panel 1001, agenda 818, historia 731, reportes 94, recepcion 981, atencion 473, cobro 323, cancelar 197; sin trabajo disponible: 732.
- CPU del backend: 59 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 102 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 298 MB (63 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 981 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 489 pausas, 45.3 s en total (10 % del tiempo de vida de la JVM); 49 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 950 MB (tope -Xmx1g).
- Clases cargadas: 25567 en total, 181 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4727 |
| 0.1 | 53 | 54 | 11383 |
| 0.2 | 80 | 81 | 16084 |
| 0.4 | 87 | 88 | 17209 |
| 1.2 | 127 | 129 | 24384 |
| 2.4 | 132 | 135 | 25039 |
| 3.2 | 134 | 137 | 25352 |
| 3.7 | 134 | 137 | 25403 |
| 4.3 | 135 | 138 | 25477 |
| 4.9 | 135 | 138 | 25479 |
| 5.5 | 136 | 139 | 25520 |
| 6.2 | 136 | 139 | 25564 |
| 6.8 | 136 | 139 | 25566 |
| 7.7 | 136 | 139 | 25567 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 168 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 176 |
| `com.fasterxml.jackson.databind` | 2 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 71.0 | 377.76 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1251 | 43.0 | 34.4 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1251 | 37.0 | 29.59 | 13 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 24.2 | 33.06 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 20.0 | 106.33 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 818 | 14.1 | 17.26 | 5 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1251 | 12.8 | 10.27 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2069 | 12.3 | 5.96 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.7 | 39.85 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111393 | 3.4 | 0.03 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 981 | 981 | coincide |
| Mascotas | 981 | 981 | coincide |
| Citas agendadas | 967 | 967 | coincide |
| Diagnósticos | 468 | 468 | coincide |
| Recetas | 468 | 468 | coincide |
| Pagos | 323 | 323 | coincide |
| Citas completadas | 468 | 468 | coincide |
| Citas canceladas | 197 | 197 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 981 | 0 | 0.0 % | 41 | 261 | 665 |
| Registrar mascota | 981 | 0 | 0.0 % | 24 | 73 | 193 |
| Agendar cita | 967 | 14 | 1.4 % | 32 | 97 | 215 |
| Recepción (llegada) | 473 | 0 | 0.0 % | 20 | 152 | 562 |
| Iniciar atención | 468 | 5 | 1.1 % | 28 | 97 | 190 |
| Leer la consulta | 468 | 0 | 0.0 % | 15 | 49 | 179 |
| Registrar diagnóstico | 468 | 0 | 0.0 % | 15 | 44 | 113 |
| Registrar receta | 468 | 0 | 0.0 % | 16 | 58 | 193 |
| Actualizar mascota | 468 | 0 | 0.0 % | 16 | 50 | 170 |
| Cerrar la consulta | 468 | 0 | 0.0 % | 29 | 98 | 206 |
| Caja: crear punto de cobro | 191 | 0 | 0.0 % | 15 | 111 | 674 |
| Caja: vincular equipo | 191 | 0 | 0.0 % | 14 | 49 | 200 |
| Caja: abrir la caja | 191 | 0 | 0.0 % | 18 | 56 | 123 |
| Cobrar | 323 | 0 | 0.0 % | 24 | 130 | 404 |
| Cancelar cita | 197 | 0 | 0.0 % | 24 | 328 | 847 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 476 | 6 980 | 8 901 | 21 % |
| panel | 1251 | 0 (0 %) | 86 | 470 | 944 | 14 % |
| login | 250 | 0 (0 %) | 392 | 1 326 | 2 478 | 10 % |
| propietarios | 818 | 0 (0 %) | 72 | 264 | 542 | 6 % |
| apoderado | 981 | 0 (0 %) | 41 | 261 | 665 | 5 % |
| lista_de_historias | 731 | 0 (0 %) | 43 | 333 | 744 | 5 % |
| citas_del_mes | 818 | 0 (0 %) | 38 | 349 | 719 | 5 % |
| cita | 981 | 14 (1 %) | 32 | 97 | 215 | 3 % |
| navegacion | 1251 | 0 (0 %) | 11 | 117 | 528 | 3 % |
| privacidad | 1251 | 0 (0 %) | 14 | 123 | 244 | 3 % |
| mascota | 981 | 0 (0 %) | 24 | 73 | 193 | 2 % |
| historia | 731 | 0 (0 %) | 27 | 88 | 177 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 8 | 56 | 329 | 354 | 0 % |
| 0.4 | 38 | 191 | 317 | 522 | 1 % |
| 0.9 | 68 | 302 | 269 | 499 | 0 % |
| 1.4 | 98 | 438 | 125 | 430 | 0 % |
| 1.9 | 128 | 523 | 98 | 356 | 0 % |
| 2.4 | 158 | 652 | 149 | 670 | 0 % |
| 2.9 | 188 | 769 | 179 | 1 170 | 0 % |
| 3.4 | 218 | 947 | 111 | 463 | 0 % |
| 3.9 | 248 | 995 | 178 | 641 | 0 % |
| 4.4 | 250 | 990 | 216 | 632 | 0 % |
| 4.9 | 250 | 905 | 141 | 376 | 0 % |
| 5.4 | 250 | 1021 | 148 | 367 | 0 % |
| 5.9 | 250 | 873 | 202 | 660 | 0 % |
| 6.4 | 250 | 964 | 82 | 301 | 0 % |
| 6.9 | 194 | 256 | 82 | 214 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
