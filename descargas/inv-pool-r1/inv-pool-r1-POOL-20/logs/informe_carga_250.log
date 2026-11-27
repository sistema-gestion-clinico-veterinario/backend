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
| Peticiones hechas | 19 965 |
| Peticiones correctas (2xx) | 19 944 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 25 |
| Tiempo p95 (ms) | 224 |
| Tiempo p99 (ms) | 821 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 65 |
| Tiempo con la CPU al tope (%) | 23 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 263 |
| Sentencias SQL | 540 892 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 965 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 10 / 12 | 0 % | 282 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7301; fallidas: 21.
- Acciones hechas: panel 997, agenda 812, historia 731, reportes 94, recepcion 980, atencion 475, cobro 321, cancelar 198; sin trabajo disponible: 735.
- CPU del backend: 65 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 23 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 96 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 263 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.7 s.
- Correo: el servidor de pruebas recibió 980 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 476 pausas, 46.3 s en total (10 % del tiempo de vida de la JVM); 46 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 901 MB (tope -Xmx1g).
- Clases cargadas: 25563 en total, 166 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4700 |
| 0.2 | 50 | 50 | 11014 |
| 0.3 | 80 | 81 | 16082 |
| 0.3 | 82 | 83 | 16434 |
| 0.5 | 101 | 103 | 19711 |
| 1.9 | 131 | 133 | 24803 |
| 3.0 | 133 | 136 | 25314 |
| 3.6 | 134 | 137 | 25383 |
| 4.2 | 135 | 138 | 25451 |
| 4.9 | 135 | 138 | 25475 |
| 5.5 | 136 | 139 | 25515 |
| 6.1 | 136 | 139 | 25560 |
| 6.7 | 136 | 139 | 25562 |
| 7.5 | 136 | 139 | 25563 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 154 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 161 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 63.7 | 338.58 | 23 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1247 | 45.2 | 36.27 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1247 | 38.3 | 30.7 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 27.1 | 37.03 | 10 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 20.7 | 110.16 | 7 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=$10 and (cast($2 as text) is null or c1_0.fe` | 812 | 16.9 | 20.76 | 6 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1247 | 11.9 | 9.55 | 4 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2059 | 11.4 | 5.53 | 4 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 110690 | 3.4 | 0.03 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.4 | 35.64 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 980 | 980 | coincide |
| Mascotas | 980 | 980 | coincide |
| Citas agendadas | 966 | 966 | coincide |
| Diagnósticos | 468 | 468 | coincide |
| Recetas | 468 | 468 | coincide |
| Pagos | 321 | 321 | coincide |
| Citas completadas | 468 | 468 | coincide |
| Citas canceladas | 198 | 198 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 980 | 0 | 0.0 % | 44 | 246 | 588 |
| Registrar mascota | 980 | 0 | 0.0 % | 25 | 82 | 219 |
| Agendar cita | 966 | 14 | 1.4 % | 36 | 123 | 267 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 22 | 221 | 579 |
| Iniciar atención | 468 | 7 | 1.5 % | 32 | 102 | 201 |
| Leer la consulta | 468 | 0 | 0.0 % | 18 | 63 | 168 |
| Registrar diagnóstico | 468 | 0 | 0.0 % | 18 | 55 | 153 |
| Registrar receta | 468 | 0 | 0.0 % | 19 | 58 | 173 |
| Actualizar mascota | 468 | 0 | 0.0 % | 19 | 63 | 153 |
| Cerrar la consulta | 468 | 0 | 0.0 % | 32 | 94 | 234 |
| Caja: crear punto de cobro | 191 | 0 | 0.0 % | 17 | 227 | 621 |
| Caja: vincular equipo | 191 | 0 | 0.0 % | 16 | 51 | 165 |
| Caja: abrir la caja | 191 | 0 | 0.0 % | 20 | 55 | 122 |
| Cobrar | 321 | 0 | 0.0 % | 25 | 130 | 470 |
| Cancelar cita | 198 | 0 | 0.0 % | 25 | 187 | 660 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 619 | 6 315 | 7 585 | 20 % |
| panel | 1247 | 0 (0 %) | 101 | 478 | 827 | 14 % |
| login | 250 | 0 (0 %) | 424 | 2 284 | 3 432 | 13 % |
| propietarios | 812 | 0 (0 %) | 80 | 267 | 545 | 6 % |
| apoderado | 980 | 0 (0 %) | 44 | 246 | 588 | 5 % |
| citas_del_mes | 812 | 0 (0 %) | 44 | 283 | 787 | 5 % |
| lista_de_historias | 731 | 0 (0 %) | 51 | 321 | 690 | 5 % |
| cita | 980 | 14 (1 %) | 36 | 123 | 267 | 4 % |
| navegacion | 1247 | 0 (0 %) | 13 | 148 | 529 | 3 % |
| privacidad | 1247 | 0 (0 %) | 18 | 88 | 192 | 3 % |
| mascota | 980 | 0 (0 %) | 25 | 82 | 219 | 2 % |
| historia | 731 | 0 (0 %) | 31 | 96 | 201 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 8 | 368 | 368 | 0 % |
| 0.3 | 31 | 177 | 704 | 1 695 | 1 % |
| 0.8 | 61 | 276 | 316 | 896 | 0 % |
| 1.3 | 91 | 391 | 152 | 456 | 0 % |
| 1.8 | 121 | 570 | 142 | 766 | 0 % |
| 2.3 | 151 | 658 | 251 | 1 057 | 0 % |
| 2.8 | 181 | 782 | 121 | 827 | 0 % |
| 3.3 | 211 | 877 | 137 | 785 | 0 % |
| 3.8 | 241 | 1031 | 305 | 2 284 | 0 % |
| 4.3 | 250 | 992 | 427 | 998 | 0 % |
| 4.8 | 250 | 914 | 193 | 630 | 0 % |
| 5.3 | 250 | 972 | 118 | 328 | 0 % |
| 5.8 | 250 | 940 | 244 | 855 | 0 % |
| 6.3 | 250 | 981 | 92 | 180 | 0 % |
| 6.8 | 238 | 447 | 95 | 343 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
