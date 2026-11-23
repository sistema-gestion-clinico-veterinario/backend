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
| Peticiones hechas | 19 962 |
| Peticiones correctas (2xx) | 19 937 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 44.4 |
| Tiempo mediano (ms) | 24 |
| Tiempo p95 (ms) | 231 |
| Tiempo p99 (ms) | 800 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 65 |
| Tiempo con la CPU al tope (%) | 26 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 40 |
| Memoria pico (MB, de 2048) | 1 258 |
| Sentencias SQL | 543 567 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_250 | 250 / 250 | 19 962 | 44.4 | no aplica (ramping-vus) | 0.1 % | 0 | 3 / 9 / 14 | 0 % | 277 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 7282; fallidas: 25.
- Acciones hechas: panel 999, agenda 814, historia 729, reportes 94, recepcion 981, atencion 474, cobro 322, cancelar 197; sin trabajo disponible: 732.
- CPU del backend: 65 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 26 % del tiempo.
- CPU de PostgreSQL: 40 % de media y 90 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 258 MB (61 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **250 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 981 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 496 pausas, 49.5 s en total (11 % del tiempo de vida de la JVM); 52 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 917 MB (tope -Xmx1g).
- Clases cargadas: 25560 en total, 163 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 19 | 19 | 4728 |
| 0.2 | 53 | 54 | 11404 |
| 0.2 | 80 | 81 | 16084 |
| 0.4 | 86 | 87 | 17090 |
| 0.5 | 101 | 102 | 19656 |
| 1.9 | 131 | 133 | 24828 |
| 3.0 | 133 | 136 | 25311 |
| 3.5 | 134 | 137 | 25380 |
| 4.2 | 135 | 138 | 25449 |
| 4.9 | 135 | 138 | 25472 |
| 5.4 | 136 | 139 | 25512 |
| 6.0 | 136 | 139 | 25557 |
| 6.7 | 136 | 139 | 25559 |
| 7.5 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 151 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 158 |
| `com.fasterxml.jackson.databind` | 2 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 188 | 75.5 | 401.33 | 26 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 1249 | 48.0 | 38.42 | 17 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 1249 | 42.5 | 34.03 | 15 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 729 | 26.3 | 36.13 | 9 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 188 | 22.0 | 117.23 | 8 % |
| `select cd1_0.id,cd1_0.aviso_id,cd1_0.aviso_version,cd1_0.canal,cd1_0.company_id,cd1_0.contenido_hash,cd1_0.estado,cd1_0.fecha,cd1_0.finalidad,cd1_0.ip_address,cd1_0.motivo,cd1_0.registrado_por,cd1_0.user_agent,cd1_0.usua` | 1249 | 14.3 | 11.47 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 2063 | 12.4 | 6.02 | 4 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 94 | 4.0 | 43.08 | 1 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 2063 | 3.4 | 1.64 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 2063 | 3.3 | 1.61 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 981 | 981 | coincide |
| Mascotas | 981 | 981 | coincide |
| Citas agendadas | 964 | 964 | coincide |
| Diagnósticos | 466 | 466 | coincide |
| Recetas | 466 | 466 | coincide |
| Pagos | 322 | 322 | coincide |
| Citas completadas | 466 | 466 | coincide |
| Citas canceladas | 197 | 197 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 981 | 0 | 0.0 % | 44 | 307 | 683 |
| Registrar mascota | 981 | 0 | 0.0 % | 26 | 75 | 203 |
| Agendar cita | 964 | 17 | 1.7 % | 37 | 134 | 450 |
| Recepción (llegada) | 474 | 0 | 0.0 % | 22 | 228 | 733 |
| Iniciar atención | 466 | 8 | 1.7 % | 30 | 91 | 199 |
| Leer la consulta | 466 | 0 | 0.0 % | 19 | 55 | 188 |
| Registrar diagnóstico | 466 | 0 | 0.0 % | 18 | 55 | 145 |
| Registrar receta | 466 | 0 | 0.0 % | 20 | 56 | 152 |
| Actualizar mascota | 466 | 0 | 0.0 % | 19 | 57 | 158 |
| Cerrar la consulta | 466 | 0 | 0.0 % | 32 | 97 | 283 |
| Caja: crear punto de cobro | 189 | 0 | 0.0 % | 19 | 409 | 633 |
| Caja: vincular equipo | 189 | 0 | 0.0 % | 19 | 53 | 175 |
| Caja: abrir la caja | 189 | 0 | 0.0 % | 21 | 69 | 208 |
| Cobrar | 322 | 0 | 0.0 % | 24 | 129 | 450 |
| Cancelar cita | 197 | 0 | 0.0 % | 26 | 261 | 589 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| reporte_clinico | 94 | 0 (0 %) | 2 987 | 5 833 | 7 805 | 21 % |
| panel | 1249 | 0 (0 %) | 107 | 494 | 799 | 14 % |
| login | 250 | 0 (0 %) | 426 | 2 050 | 2 556 | 12 % |
| propietarios | 814 | 0 (0 %) | 83 | 252 | 471 | 6 % |
| apoderado | 981 | 0 (0 %) | 44 | 307 | 683 | 5 % |
| lista_de_historias | 729 | 0 (0 %) | 47 | 352 | 790 | 5 % |
| citas_del_mes | 814 | 0 (0 %) | 36 | 230 | 685 | 4 % |
| cita | 981 | 17 (2 %) | 37 | 134 | 450 | 4 % |
| navegacion | 1249 | 0 (0 %) | 13 | 194 | 562 | 3 % |
| privacidad | 1249 | 0 (0 %) | 16 | 106 | 207 | 3 % |
| mascota | 981 | 0 (0 %) | 26 | 75 | 203 | 2 % |
| historia | 729 | 0 (0 %) | 30 | 85 | 205 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 3 | 611 | 611 | 0 % |
| 0.3 | 30 | 164 | 608 | 1 272 | 1 % |
| 0.8 | 60 | 274 | 276 | 787 | 0 % |
| 1.3 | 90 | 384 | 164 | 529 | 0 % |
| 1.8 | 120 | 549 | 144 | 615 | 0 % |
| 2.3 | 150 | 666 | 172 | 971 | 0 % |
| 2.8 | 180 | 764 | 145 | 491 | 0 % |
| 3.3 | 210 | 853 | 127 | 704 | 0 % |
| 3.8 | 240 | 1034 | 298 | 1 201 | 0 % |
| 4.3 | 250 | 1024 | 221 | 899 | 0 % |
| 4.8 | 250 | 944 | 165 | 539 | 0 % |
| 5.3 | 250 | 940 | 147 | 544 | 0 % |
| 5.8 | 250 | 974 | 259 | 600 | 0 % |
| 6.3 | 250 | 990 | 103 | 234 | 0 % |
| 6.8 | 241 | 489 | 357 | 2 617 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
