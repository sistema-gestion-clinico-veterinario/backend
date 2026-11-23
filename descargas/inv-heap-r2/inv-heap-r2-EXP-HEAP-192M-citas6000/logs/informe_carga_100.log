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
| Usuarios sin degradación | 44 |
| Peticiones hechas | 3 923 |
| Peticiones correctas (2xx) | 3 566 |
| Errores (%) | 9.1 % |
| Ritmo medio (peticiones/s) | 12.9 |
| Tiempo mediano (ms) | 20 |
| Tiempo p95 (ms) | 392 |
| Tiempo p99 (ms) | 1 243 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 46 |
| Tiempo con la CPU al tope (%) | 11 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 5 |
| Memoria pico (MB, de 512) | 423 |
| Sentencias SQL | 100 166 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 923 | 12.9 | no aplica (ramping-vus) | 9.1 % | 0 | 1 / 2 / 4 | 0 % | 121 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1340; fallidas: 0.
- Acciones hechas: panel 175, agenda 142, historia 131, reportes 12, recepcion 176, atencion 87, cobro 55, cancelar 34; sin trabajo disponible: 137.
- CPU del backend: 46 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 14 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 423 MB (83 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 1.1 con unos 71 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **44 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.5 s.
- Correo: el servidor de pruebas recibió 176 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.6 %).
- GC: 418 pausas, 16.2 s en total (5 % del tiempo de vida de la JVM); 68 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25337 en total, 334 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2099 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 23 | 23 | 6112 |
| 0.1 | 59 | 60 | 13442 |
| 0.1 | 74 | 74 | 16078 |
| 0.2 | 74 | 75 | 16117 |
| 0.3 | 79 | 80 | 17207 |
| 0.3 | 90 | 92 | 19038 |
| 1.2 | 111 | 114 | 24465 |
| 2.0 | 110 | 114 | 24781 |
| 2.7 | 111 | 114 | 25003 |
| 3.3 | 111 | 114 | 25185 |
| 4.2 | 111 | 114 | 25274 |
| 5.1 | 111 | 114 | 25336 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 321 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 329 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.hibernate.event.spi` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 225 | 1.5 | 6.74 | 13 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 24 | 1.4 | 58.99 | 12 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 225 | 1.4 | 6.2 | 12 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 142 | 0.9 | 6.64 | 8 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 131 | 0.8 | 6.42 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 367 | 0.8 | 2.18 | 7 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 657 | 0.4 | 0.57 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 607 | 0.4 | 0.59 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 367 | 0.3 | 0.92 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 225 | 0.3 | 1.28 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 176 | 176 | coincide |
| Mascotas | 176 | 176 | coincide |
| Citas agendadas | 176 | 176 | coincide |
| Diagnósticos | 87 | 87 | coincide |
| Recetas | 87 | 87 | coincide |
| Pagos | 55 | 55 | coincide |
| Citas completadas | 87 | 87 | coincide |
| Citas canceladas | 34 | 34 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 176 | 0 | 0.0 % | 31 | 116 | 272 |
| Registrar mascota | 176 | 0 | 0.0 % | 23 | 62 | 262 |
| Agendar cita | 176 | 0 | 0.0 % | 31 | 114 | 296 |
| Recepción (llegada) | 87 | 0 | 0.0 % | 14 | 63 | 303 |
| Iniciar atención | 87 | 0 | 0.0 % | 18 | 63 | 237 |
| Leer la consulta | 87 | 0 | 0.0 % | 11 | 36 | 56 |
| Registrar diagnóstico | 87 | 0 | 0.0 % | 9 | 37 | 48 |
| Registrar receta | 87 | 0 | 0.0 % | 9 | 39 | 62 |
| Actualizar mascota | 87 | 0 | 0.0 % | 10 | 45 | 66 |
| Cerrar la consulta | 87 | 0 | 0.0 % | 18 | 76 | 339 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 21 | 45 | 213 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 18 | 44 | 54 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 22 | 61 | 76 |
| Cobrar | 55 | 0 | 0.0 % | 16 | 67 | 279 |
| Cancelar cita | 34 | 0 | 0.0 % | 32 | 56 | 191 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 407 | 357 (88 %) | 362 | 1 417 | 1 705 | 62 % |
| reporte_clinico | 12 | 0 (0 %) | 1 287 | 2 169 | 2 169 | 5 % |
| panel | 225 | 0 (0 %) | 47 | 138 | 356 | 5 % |
| propietarios | 142 | 0 (0 %) | 34 | 137 | 176 | 3 % |
| apoderado | 176 | 0 (0 %) | 31 | 116 | 272 | 3 % |
| cita | 176 | 0 (0 %) | 31 | 114 | 296 | 2 % |
| citas_del_mes | 142 | 0 (0 %) | 28 | 98 | 317 | 2 % |
| mascota | 176 | 0 (0 %) | 23 | 62 | 262 | 2 % |
| lista_de_historias | 131 | 0 (0 %) | 26 | 79 | 346 | 2 % |
| navegacion | 225 | 0 (0 %) | 10 | 38 | 276 | 1 % |
| historia | 131 | 0 (0 %) | 23 | 72 | 113 | 1 % |
| empleados | 142 | 0 (0 %) | 13 | 48 | 291 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 3 | 28 | 349 | 577 | 0 % |
| 0.1 | 17 | 100 | 547 | 2 169 | 0 % |
| 0.3 | 30 | 172 | 358 | 399 | 0 % |
| 0.6 | 44 | 223 | 326 | 396 | 0 % |
| 0.8 | 58 | 209 | 333 | 460 | 4 % |
| 1.1 | 71 | 194 | 326 | 1 049 | 7 % |
| 1.3 | 85 | 208 | 882 | 1 257 | 11 % |
| 1.6 | 99 | 248 | 540 | 1 206 | 12 % |
| 1.8 | 100 | 208 | 589 | 1 009 | 12 % |
| 2.1 | 100 | 257 | 756 | 1 394 | 10 % |
| 2.3 | 100 | 232 | 1 495 | 1 736 | 12 % |
| 2.6 | 100 | 207 | 572 | 1 495 | 12 % |
| 2.8 | 100 | 228 | 980 | 1 279 | 12 % |
| 3.1 | 100 | 223 | 453 | 619 | 14 % |
| 3.3 | 100 | 235 | 332 | 586 | 11 % |
| 3.6 | 100 | 210 | 55 | 369 | 14 % |
| 3.8 | 100 | 216 | 38 | 58 | 12 % |
| 4.1 | 100 | 241 | 37 | 54 | 10 % |
| 4.3 | 100 | 214 | 231 | 817 | 8 % |
| 4.6 | 76 | 70 | 42 | 1 136 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
