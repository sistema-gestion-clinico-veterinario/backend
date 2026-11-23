# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Veredicto | FALLA |
| Usuarios sin degradación | 39 |
| Peticiones hechas | 3 988 |
| Peticiones correctas (2xx) | 3 632 |
| Errores (%) | 8.9 % |
| Ritmo medio (peticiones/s) | 13.0 |
| Tiempo mediano (ms) | 14 |
| Tiempo p95 (ms) | 274 |
| Tiempo p99 (ms) | 694 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 33 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 429 |
| Sentencias SQL | 103 172 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 988 | 13.0 | no aplica (ramping-vus) | 8.9 % | 0 | 1 / 2 / 4 | 0 % | 119 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1365; fallidas: 0.
- Acciones hechas: panel 180, agenda 149, historia 130, reportes 0, recepcion 179, atencion 89, cobro 56, cancelar 35; sin trabajo disponible: 135.
- CPU del backend: 33 % de media y 81 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 12 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 429 MB (84 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 67 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **39 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 179 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.4 %).
- GC: 363 pausas, 7.0 s en total (2 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25139 en total, 341 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1883 |
| 0.0 | 11 | 11 | 3734 |
| 0.0 | 19 | 19 | 5398 |
| 0.1 | 44 | 44 | 10582 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 76 | 77 | 16607 |
| 0.3 | 79 | 80 | 17210 |
| 0.4 | 99 | 101 | 21095 |
| 1.3 | 111 | 114 | 24248 |
| 2.1 | 110 | 114 | 24638 |
| 2.8 | 111 | 114 | 24884 |
| 3.6 | 110 | 114 | 24982 |
| 4.6 | 111 | 114 | 25106 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 328 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 336 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 230 | 2.2 | 9.63 | 23 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 230 | 1.3 | 5.66 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.3 | 9.8 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 379 | 0.7 | 1.79 | 7 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 379 | 0.3 | 0.84 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 606 | 0.3 | 0.48 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 379 | 0.3 | 0.69 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 656 | 0.3 | 0.4 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 230 | 0.2 | 0.93 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 149 | 0.2 | 1.26 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 179 | 179 | coincide |
| Mascotas | 179 | 179 | coincide |
| Citas agendadas | 179 | 179 | coincide |
| Diagnósticos | 89 | 89 | coincide |
| Recetas | 89 | 89 | coincide |
| Pagos | 56 | 56 | coincide |
| Citas completadas | 89 | 89 | coincide |
| Citas canceladas | 35 | 35 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 179 | 0 | 0.0 % | 21 | 63 | 185 |
| Registrar mascota | 179 | 0 | 0.0 % | 15 | 51 | 318 |
| Agendar cita | 179 | 0 | 0.0 % | 16 | 60 | 168 |
| Recepción (llegada) | 89 | 0 | 0.0 % | 9 | 37 | 87 |
| Iniciar atención | 89 | 0 | 0.0 % | 11 | 48 | 99 |
| Leer la consulta | 89 | 0 | 0.0 % | 7 | 31 | 77 |
| Registrar diagnóstico | 89 | 0 | 0.0 % | 6 | 31 | 34 |
| Registrar receta | 89 | 0 | 0.0 % | 7 | 30 | 51 |
| Actualizar mascota | 89 | 0 | 0.0 % | 7 | 30 | 40 |
| Cerrar la consulta | 89 | 0 | 0.0 % | 13 | 47 | 71 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 11 | 32 | 34 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 12 | 32 | 55 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 16 | 34 | 44 |
| Cobrar | 56 | 0 | 0.0 % | 12 | 44 | 49 |
| Cancelar cita | 35 | 0 | 0.0 % | 12 | 42 | 44 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 406 | 356 (88 %) | 270 | 800 | 1 453 | 64 % |
| panel | 230 | 0 (0 %) | 41 | 94 | 263 | 6 % |
| propietarios | 149 | 0 (0 %) | 21 | 131 | 142 | 3 % |
| apoderado | 179 | 0 (0 %) | 21 | 63 | 185 | 3 % |
| cita | 179 | 0 (0 %) | 16 | 60 | 168 | 3 % |
| mascota | 179 | 0 (0 %) | 15 | 51 | 318 | 2 % |
| lista_de_historias | 130 | 0 (0 %) | 22 | 44 | 52 | 2 % |
| citas_del_mes | 149 | 0 (0 %) | 17 | 44 | 60 | 2 % |
| historia | 130 | 0 (0 %) | 13 | 41 | 71 | 1 % |
| navegacion | 230 | 0 (0 %) | 7 | 22 | 34 | 1 % |
| avisos_de_cumplimiento | 230 | 0 (0 %) | 5 | 20 | 48 | 1 % |
| empleados | 149 | 0 (0 %) | 8 | 39 | 48 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 12 | 84 | 281 | 326 | 0 % |
| 0.2 | 26 | 150 | 292 | 485 | 0 % |
| 0.5 | 39 | 207 | 261 | 288 | 0 % |
| 0.7 | 53 | 223 | 255 | 294 | 1 % |
| 1.0 | 67 | 199 | 257 | 299 | 7 % |
| 1.2 | 80 | 199 | 249 | 814 | 9 % |
| 1.5 | 94 | 236 | 411 | 796 | 11 % |
| 1.7 | 100 | 237 | 516 | 758 | 12 % |
| 2.0 | 100 | 232 | 389 | 566 | 10 % |
| 2.2 | 100 | 205 | 493 | 1 549 | 15 % |
| 2.5 | 100 | 243 | 391 | 807 | 11 % |
| 2.7 | 100 | 231 | 532 | 929 | 13 % |
| 3.0 | 100 | 235 | 303 | 521 | 11 % |
| 3.2 | 100 | 240 | 260 | 689 | 11 % |
| 3.5 | 100 | 210 | 125 | 311 | 13 % |
| 3.7 | 100 | 236 | 37 | 41 | 11 % |
| 4.0 | 100 | 236 | 32 | 38 | 8 % |
| 4.2 | 100 | 237 | 35 | 42 | 11 % |
| 4.5 | 86 | 132 | 38 | 43 | 3 % |
| 4.7 | 43 | 16 | 38 | 38 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
