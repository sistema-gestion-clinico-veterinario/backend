# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 1; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-192M (experimental: -Xmx192m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 335 |
| Peticiones correctas (2xx) | 6 323 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.7 |
| Tiempo mediano (ms) | 15 |
| Tiempo p95 (ms) | 81 |
| Tiempo p99 (ms) | 365 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 2 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 11 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 172 741 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 6 335 | 20.7 | no aplica (ramping-vus) | 0.2 % | 0 | 2 / 4 / 5 | 0 % | 134 | 0 | **sí** |


## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2401; fallidas: 12.
- Acciones hechas: panel 295, agenda 252, historia 232, reportes 0, recepcion 318, atencion 157, cobro 96, cancelar 56; sin trabajo disponible: 254.
- CPU del backend: 32 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 11 % de media y 28 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 318 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 111 a 111 MB (+0.4 %).
- GC: 388 pausas, 6.8 s en total (2 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 183 MB (tope -Xmx192m).
- Clases cargadas: 25205 en total, 200 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2041 |
| 0.0 | 11 | 11 | 3772 |
| 0.0 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12299 |
| 0.2 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16115 |
| 0.3 | 79 | 80 | 17106 |
| 0.3 | 82 | 83 | 17647 |
| 1.0 | 111 | 113 | 24075 |
| 1.9 | 110 | 114 | 24581 |
| 2.5 | 110 | 114 | 24813 |
| 3.3 | 111 | 114 | 25081 |
| 4.0 | 111 | 114 | 25130 |
| 4.9 | 111 | 114 | 25204 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 196 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 200 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 395 | 5.0 | 12.64 | 21 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 395 | 4.9 | 12.4 | 21 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 232 | 3.6 | 15.37 | 15 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 647 | 1.6 | 2.53 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 647 | 0.7 | 1.12 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 647 | 0.7 | 1.01 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 395 | 0.5 | 1.33 | 2 % |
| `SELECT COUNT(*) FROM historia_clinica hc JOIN mascota m ON m.id = hc.mascota_id JOIN apoderado a ON a.id = m.apoderado_id JOIN usuario u ON u.id = a.user_id WHERE ($1 = $17 OR a.company_id = $2) AND (CAST($3 AS bigint) I` | 232 | 0.5 | 2.09 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 252 | 0.4 | 1.71 | 2 % |
| `select uec1_0.id,uec1_0.company_id,uec1_0.created_at,uec1_0.direccion,uec1_0.telefono,uec1_0.updated_at,uec1_0.usuario_id from usuario_empresa_contacto uec1_0 where uec1_0.usuario_id=$1 and uec1_0.company_id=$2` | 34539 | 0.4 | 0.01 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 318 | 318 | coincide |
| Mascotas | 318 | 318 | coincide |
| Citas agendadas | 307 | 307 | coincide |
| Diagnósticos | 156 | 156 | coincide |
| Recetas | 156 | 156 | coincide |
| Pagos | 96 | 96 | coincide |
| Citas completadas | 156 | 156 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 318 | 0 | 0.0 % | 25 | 77 | 168 |
| Registrar mascota | 318 | 0 | 0.0 % | 18 | 50 | 156 |
| Agendar cita | 307 | 11 | 3.5 % | 23 | 65 | 127 |
| Recepción (llegada) | 157 | 0 | 0.0 % | 13 | 32 | 70 |
| Iniciar atención | 156 | 1 | 0.6 % | 16 | 46 | 122 |
| Leer la consulta | 156 | 0 | 0.0 % | 9 | 30 | 117 |
| Registrar diagnóstico | 156 | 0 | 0.0 % | 9 | 30 | 54 |
| Registrar receta | 156 | 0 | 0.0 % | 10 | 33 | 104 |
| Actualizar mascota | 156 | 0 | 0.0 % | 10 | 30 | 58 |
| Cerrar la consulta | 156 | 0 | 0.0 % | 17 | 72 | 126 |
| Caja: crear punto de cobro | 71 | 0 | 0.0 % | 9 | 24 | 106 |
| Caja: vincular equipo | 71 | 0 | 0.0 % | 8 | 25 | 36 |
| Caja: abrir la caja | 71 | 0 | 0.0 % | 9 | 33 | 45 |
| Cobrar | 96 | 0 | 0.0 % | 13 | 33 | 43 |
| Cancelar cita | 56 | 0 | 0.0 % | 17 | 136 | 275 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 100 | 0 (0 %) | 378 | 585 | 1 295 | 22 % |
| panel | 395 | 0 (0 %) | 61 | 104 | 155 | 14 % |
| propietarios | 252 | 0 (0 %) | 37 | 126 | 284 | 7 % |
| apoderado | 318 | 0 (0 %) | 25 | 77 | 168 | 6 % |
| cita | 318 | 11 (3 %) | 23 | 65 | 127 | 5 % |
| lista_de_historias | 232 | 0 (0 %) | 27 | 93 | 151 | 5 % |
| citas_del_mes | 252 | 0 (0 %) | 23 | 65 | 302 | 5 % |
| mascota | 318 | 0 (0 %) | 18 | 50 | 156 | 4 % |
| navegacion | 395 | 0 (0 %) | 8 | 24 | 199 | 3 % |
| historia | 232 | 0 (0 %) | 16 | 50 | 108 | 3 % |
| empleados | 252 | 0 (0 %) | 12 | 38 | 85 | 2 % |
| avisos_de_cumplimiento | 395 | 0 (0 %) | 7 | 23 | 70 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 4 | 28 | 365 | 372 | 0 % |
| 0.1 | 18 | 107 | 379 | 750 | 0 % |
| 0.3 | 31 | 165 | 366 | 527 | 0 % |
| 0.6 | 45 | 235 | 339 | 420 | 0 % |
| 0.8 | 58 | 259 | 335 | 403 | 0 % |
| 1.1 | 72 | 309 | 221 | 449 | 1 % |
| 1.3 | 86 | 376 | 142 | 516 | 0 % |
| 1.6 | 99 | 449 | 107 | 442 | 0 % |
| 1.8 | 100 | 398 | 158 | 331 | 0 % |
| 2.1 | 100 | 468 | 52 | 76 | 0 % |
| 2.3 | 100 | 415 | 65 | 100 | 0 % |
| 2.6 | 100 | 388 | 56 | 89 | 0 % |
| 2.8 | 100 | 398 | 59 | 79 | 0 % |
| 3.1 | 100 | 392 | 60 | 80 | 1 % |
| 3.3 | 100 | 388 | 56 | 73 | 0 % |
| 3.6 | 100 | 421 | 75 | 151 | 0 % |
| 3.8 | 100 | 355 | 61 | 102 | 0 % |
| 4.1 | 100 | 447 | 66 | 116 | 0 % |
| 4.3 | 100 | 256 | 66 | 129 | 1 % |
| 4.6 | 71 | 81 | 59 | 74 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
