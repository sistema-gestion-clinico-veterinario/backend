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
| Usuarios sin degradación | 52 |
| Peticiones hechas | 3 976 |
| Peticiones correctas (2xx) | 3 619 |
| Errores (%) | 9.0 % |
| Ritmo medio (peticiones/s) | 13.1 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 350 |
| Tiempo p99 (ms) | 1 077 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 41 |
| Tiempo con la CPU al tope (%) | 9 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 424 |
| Sentencias SQL | 102 850 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 976 | 13.1 | no aplica (ramping-vus) | 9.0 % | 0 | 1 / 2 / 3 | 0 % | 120 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1358; fallidas: 0.
- Acciones hechas: panel 179, agenda 149, historia 130, reportes 0, recepcion 177, atencion 89, cobro 55, cancelar 35; sin trabajo disponible: 135.
- CPU del backend: 41 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 9 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 14 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 424 MB (83 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 66 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **52 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.2 s.
- Correo: el servidor de pruebas recibió 177 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.4 %).
- GC: 363 pausas, 5.8 s en total (2 % del tiempo de vida de la JVM); 26 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 180 MB (tope -Xmx192m).
- Clases cargadas: 25145 en total, 343 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1883 |
| 0.0 | 11 | 11 | 3734 |
| 0.0 | 19 | 19 | 5396 |
| 0.1 | 44 | 44 | 10581 |
| 0.1 | 68 | 69 | 15266 |
| 0.2 | 74 | 75 | 16106 |
| 0.2 | 76 | 77 | 16540 |
| 0.3 | 79 | 80 | 17209 |
| 0.3 | 98 | 100 | 20823 |
| 1.3 | 111 | 114 | 24248 |
| 2.1 | 110 | 114 | 24640 |
| 2.8 | 111 | 114 | 24885 |
| 3.5 | 110 | 114 | 24982 |
| 4.5 | 111 | 114 | 25099 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 329 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 337 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.hibernate.event.spi` | 1 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 229 | 3.2 | 13.88 | 26 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.6 | 12.36 | 13 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 229 | 1.6 | 7.01 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 378 | 0.8 | 2.12 | 7 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 607 | 0.4 | 0.65 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 657 | 0.4 | 0.56 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 378 | 0.4 | 0.94 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 378 | 0.3 | 0.9 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 229 | 0.3 | 1.16 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 149 | 0.2 | 1.54 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 177 | 177 | coincide |
| Mascotas | 177 | 177 | coincide |
| Citas agendadas | 177 | 177 | coincide |
| Diagnósticos | 89 | 89 | coincide |
| Recetas | 89 | 89 | coincide |
| Pagos | 55 | 55 | coincide |
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
| Registrar propietario | 177 | 0 | 0.0 % | 27 | 77 | 112 |
| Registrar mascota | 177 | 0 | 0.0 % | 19 | 57 | 290 |
| Agendar cita | 177 | 0 | 0.0 % | 23 | 94 | 136 |
| Recepción (llegada) | 89 | 0 | 0.0 % | 11 | 43 | 90 |
| Iniciar atención | 89 | 0 | 0.0 % | 14 | 62 | 107 |
| Leer la consulta | 89 | 0 | 0.0 % | 8 | 40 | 78 |
| Registrar diagnóstico | 89 | 0 | 0.0 % | 7 | 32 | 63 |
| Registrar receta | 89 | 0 | 0.0 % | 8 | 48 | 58 |
| Actualizar mascota | 89 | 0 | 0.0 % | 9 | 42 | 57 |
| Cerrar la consulta | 89 | 0 | 0.0 % | 15 | 70 | 120 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 13 | 36 | 37 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 15 | 38 | 50 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 13 | 42 | 58 |
| Cobrar | 55 | 0 | 0.0 % | 15 | 46 | 82 |
| Cancelar cita | 35 | 0 | 0.0 % | 21 | 46 | 64 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 407 | 357 (88 %) | 342 | 1 233 | 1 660 | 67 % |
| panel | 229 | 0 (0 %) | 54 | 125 | 283 | 6 % |
| propietarios | 149 | 0 (0 %) | 24 | 135 | 210 | 3 % |
| apoderado | 177 | 0 (0 %) | 27 | 77 | 112 | 3 % |
| cita | 177 | 0 (0 %) | 23 | 94 | 136 | 2 % |
| mascota | 177 | 0 (0 %) | 19 | 57 | 290 | 2 % |
| citas_del_mes | 149 | 0 (0 %) | 21 | 72 | 104 | 2 % |
| lista_de_historias | 130 | 0 (0 %) | 26 | 54 | 89 | 2 % |
| historia | 130 | 0 (0 %) | 17 | 61 | 296 | 1 % |
| navegacion | 229 | 0 (0 %) | 8 | 28 | 36 | 1 % |
| empleados | 149 | 0 (0 %) | 9 | 44 | 68 | 1 % |
| cumplimiento | 229 | 0 (0 %) | 7 | 31 | 38 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 11 | 77 | 342 | 386 | 0 % |
| 0.2 | 25 | 147 | 350 | 579 | 0 % |
| 0.5 | 39 | 194 | 324 | 389 | 0 % |
| 0.7 | 52 | 234 | 320 | 373 | 1 % |
| 1.0 | 66 | 201 | 321 | 366 | 7 % |
| 1.2 | 79 | 189 | 317 | 1 106 | 9 % |
| 1.5 | 93 | 238 | 517 | 1 015 | 11 % |
| 1.7 | 100 | 240 | 897 | 1 553 | 12 % |
| 2.0 | 100 | 245 | 592 | 1 327 | 11 % |
| 2.2 | 100 | 197 | 1 156 | 1 788 | 14 % |
| 2.5 | 100 | 244 | 601 | 1 400 | 11 % |
| 2.7 | 100 | 222 | 644 | 964 | 13 % |
| 3.0 | 100 | 247 | 380 | 711 | 11 % |
| 3.2 | 100 | 215 | 344 | 790 | 10 % |
| 3.5 | 100 | 224 | 301 | 1 134 | 15 % |
| 3.7 | 100 | 232 | 47 | 309 | 10 % |
| 4.0 | 100 | 248 | 35 | 60 | 9 % |
| 4.2 | 100 | 234 | 46 | 64 | 13 % |
| 4.5 | 91 | 124 | 39 | 50 | 2 % |
| 4.7 | 42 | 24 | 49 | 50 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
