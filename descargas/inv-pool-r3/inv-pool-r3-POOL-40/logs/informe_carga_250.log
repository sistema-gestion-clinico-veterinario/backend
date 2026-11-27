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
| Peticiones hechas | 19 999 |
| Peticiones correctas (2xx) | 19 978 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.6 |
| Tiempo mediano (ms) | 22 |
| Tiempo p95 (ms) | 195 |
| Tiempo p99 (ms) | 743 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 60 |
| Tiempo con la CPU al tope (%) | 16 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 36 |
| Memoria pico (MB, de 2048) | 1 319 |
| Sentencias SQL | 545 404 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 999 | 44.6 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 8 / 12 | 0 % | 282 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7302; fallidas: 21.
- Acciones hechas: panel 998, agenda 821, historia 731, reportes 94, recepcion 984, atencion 475, cobro 320, cancelar 197; sin trabajo disponible: 735.
- CPU del backend: 60 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 16 % del tiempo.
- CPU de PostgreSQL: 36 % de media y 82 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 319 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.8 s.
- Correo: el servidor de pruebas recibió 984 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 461 pausas, 43.9 s en total (10 % del tiempo de vida de la JVM); 46 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 952 MB (tope -Xmx1g).
- Clases cargadas: 25560 en total, 167 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4660 |
| 0.1 | 49 | 50 | 10941 |
| 0.2 | 80 | 81 | 16056 |
| 0.3 | 86 | 87 | 17089 |
| 0.9 | 123 | 126 | 23759 |
| 2.2 | 132 | 134 | 24953 |
| 3.1 | 133 | 136 | 25312 |
| 3.7 | 134 | 137 | 25397 |
| 4.2 | 135 | 138 | 25455 |
| 4.8 | 135 | 138 | 25473 |
| 5.4 | 136 | 139 | 25512 |
| 6.0 | 136 | 139 | 25557 |
| 6.7 | 136 | 139 | 25559 |
| 7.5 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 155 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 162 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 62.8 | 334.16 | 25 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1248 | 40.9 | 32.74 | 16 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1248 | 35.5 | 28.43 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 731 | 22.4 | 30.7 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 19.9 | 105.63 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1248 | 13.2 | 10.62 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2069 | 11.2 | 5.4 | 5 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 3.5 | 37.11 | 1 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 111705 | 3.3 | 0.03 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2069 | 3.2 | 1.56 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 984 | 984 | coincide |
| Mascotas | 984 | 984 | coincide |
| Citas agendadas | 970 | 970 | coincide |
| Diagnósticos | 468 | 468 | coincide |
| Recetas | 468 | 468 | coincide |
| Pagos | 320 | 320 | coincide |
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
| Registrar propietario | 984 | 0 | 0.0 % | 41 | 235 | 690 |
| Registrar mascota | 984 | 0 | 0.0 % | 23 | 68 | 131 |
| Agendar cita | 970 | 14 | 1.4 % | 34 | 117 | 281 |
| Recepción (llegada) | 475 | 0 | 0.0 % | 19 | 221 | 624 |
| Iniciar atención | 468 | 7 | 1.5 % | 28 | 84 | 163 |
| Leer la consulta | 468 | 0 | 0.0 % | 15 | 50 | 120 |
| Registrar diagnóstico | 468 | 0 | 0.0 % | 16 | 48 | 190 |
| Registrar receta | 468 | 0 | 0.0 % | 17 | 49 | 83 |
| Actualizar mascota | 468 | 0 | 0.0 % | 18 | 47 | 65 |
| Cerrar la consulta | 468 | 0 | 0.0 % | 28 | 84 | 246 |
| Caja: crear punto de cobro | 188 | 0 | 0.0 % | 15 | 186 | 692 |
| Caja: vincular equipo | 188 | 0 | 0.0 % | 14 | 38 | 98 |
| Caja: abrir la caja | 188 | 0 | 0.0 % | 16 | 56 | 140 |
| Cobrar | 320 | 0 | 0.0 % | 20 | 88 | 215 |
| Cancelar cita | 197 | 0 | 0.0 % | 24 | 152 | 794 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 364 | 5 668 | 6 479 | 21 % |
| panel | 1248 | 0 (0 %) | 91 | 408 | 695 | 14 % |
| login | 250 | 0 (0 %) | 390 | 1 602 | 2 527 | 12 % |
| propietarios | 821 | 0 (0 %) | 77 | 243 | 444 | 6 % |
| apoderado | 984 | 0 (0 %) | 41 | 235 | 690 | 6 % |
| lista_de_historias | 731 | 0 (0 %) | 43 | 249 | 595 | 5 % |
| citas_del_mes | 821 | 0 (0 %) | 30 | 214 | 594 | 4 % |
| cita | 984 | 14 (1 %) | 34 | 117 | 281 | 4 % |
| navegacion | 1248 | 0 (0 %) | 12 | 116 | 511 | 3 % |
| privacidad | 1248 | 0 (0 %) | 15 | 104 | 163 | 3 % |
| mascota | 984 | 0 (0 %) | 23 | 68 | 131 | 2 % |
| historia | 731 | 0 (0 %) | 27 | 81 | 210 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 13 | 91 | 321 | 397 | 0 % |
| 0.5 | 43 | 245 | 295 | 473 | 0 % |
| 1.0 | 73 | 315 | 275 | 677 | 0 % |
| 1.5 | 103 | 466 | 82 | 419 | 0 % |
| 2.0 | 133 | 630 | 92 | 405 | 0 % |
| 2.5 | 163 | 631 | 190 | 796 | 0 % |
| 3.0 | 193 | 767 | 292 | 1 640 | 0 % |
| 3.5 | 223 | 930 | 96 | 396 | 0 % |
| 4.0 | 250 | 1002 | 258 | 841 | 0 % |
| 4.5 | 250 | 973 | 235 | 870 | 0 % |
| 5.0 | 250 | 1015 | 190 | 780 | 0 % |
| 5.5 | 250 | 950 | 129 | 375 | 0 % |
| 6.0 | 250 | 916 | 141 | 282 | 0 % |
| 6.5 | 250 | 886 | 202 | 743 | 0 % |
| 7.0 | 160 | 119 | 51 | 208 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
