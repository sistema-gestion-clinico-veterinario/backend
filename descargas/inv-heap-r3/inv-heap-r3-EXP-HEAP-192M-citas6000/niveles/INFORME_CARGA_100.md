# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 41 |
| Peticiones hechas | 3 905 |
| Peticiones correctas (2xx) | 3 550 |
| Errores (%) | 9.1 % |
| Ritmo medio (peticiones/s) | 12.8 |
| Tiempo mediano (ms) | 30 |
| Tiempo p95 (ms) | 785 |
| Tiempo p99 (ms) | 3 075 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 58 |
| Tiempo con la CPU al tope (%) | 31 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 427 |
| Sentencias SQL | 99 850 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 905 | 12.8 | no aplica (ramping-vus) | 9.1 % | 0 | 1 / 3 / 4 | 0 % | 121 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1336; fallidas: 1.
- Acciones hechas: panel 174, agenda 141, historia 130, reportes 12, recepcion 176, atencion 88, cobro 54, cancelar 33; sin trabajo disponible: 138.
- CPU del backend: 58 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 31 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 23 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 427 MB (83 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.0 con unos 68 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **41 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 3.5 s.
- Correo: el servidor de pruebas recibió 176 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 112 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.7 %).
- GC: 413 pausas, 18.1 s en total (5 % del tiempo de vida de la JVM); 68 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25342 en total, 330 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 5 | 5 | 2041 |
| 0.0 | 11 | 11 | 3772 |
| 0.1 | 21 | 22 | 5801 |
| 0.1 | 53 | 53 | 12297 |
| 0.2 | 74 | 74 | 16074 |
| 0.2 | 74 | 75 | 16117 |
| 0.3 | 79 | 80 | 17107 |
| 0.4 | 83 | 85 | 17858 |
| 1.1 | 111 | 114 | 24342 |
| 1.9 | 110 | 114 | 24748 |
| 2.6 | 111 | 114 | 24937 |
| 3.2 | 111 | 114 | 25140 |
| 3.9 | 111 | 114 | 25276 |
| 5.0 | 111 | 114 | 25311 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 321 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 329 |
| `java.util.concurrent.locks` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 224 | 2.0 | 8.74 | 13 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 224 | 1.8 | 8.0 | 12 % |
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 24 | 1.7 | 70.86 | 11 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 1.1 | 8.42 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 365 | 1.1 | 2.94 | 7 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 141 | 1.0 | 7.22 | 7 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 604 | 0.5 | 0.88 | 4 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 654 | 0.5 | 0.74 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 365 | 0.4 | 1.18 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 224 | 0.4 | 1.64 | 2 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 176 | 176 | coincide |
| Mascotas | 176 | 176 | coincide |
| Citas agendadas | 176 | 176 | coincide |
| Diagnósticos | 87 | 87 | coincide |
| Recetas | 87 | 87 | coincide |
| Pagos | 54 | 54 | coincide |
| Citas completadas | 87 | 87 | coincide |
| Citas canceladas | 33 | 33 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 176 | 0 | 0.0 % | 54 | 275 | 403 |
| Registrar mascota | 176 | 0 | 0.0 % | 34 | 123 | 393 |
| Agendar cita | 176 | 0 | 0.0 % | 48 | 195 | 533 |
| Recepción (llegada) | 88 | 0 | 0.0 % | 26 | 101 | 412 |
| Iniciar atención | 87 | 1 | 1.1 % | 37 | 121 | 168 |
| Leer la consulta | 87 | 0 | 0.0 % | 23 | 92 | 130 |
| Registrar diagnóstico | 87 | 0 | 0.0 % | 23 | 79 | 117 |
| Registrar receta | 87 | 0 | 0.0 % | 25 | 93 | 403 |
| Actualizar mascota | 87 | 0 | 0.0 % | 22 | 88 | 131 |
| Cerrar la consulta | 87 | 0 | 0.0 % | 42 | 144 | 523 |
| Caja: crear punto de cobro | 37 | 0 | 0.0 % | 22 | 110 | 326 |
| Caja: vincular equipo | 37 | 0 | 0.0 % | 24 | 95 | 105 |
| Caja: abrir la caja | 37 | 0 | 0.0 % | 27 | 99 | 117 |
| Cobrar | 54 | 0 | 0.0 % | 32 | 116 | 129 |
| Cancelar cita | 33 | 0 | 0.0 % | 33 | 239 | 316 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 404 | 354 (88 %) | 534 | 3 566 | 5 144 | 71 % |
| reporte_clinico | 12 | 0 (0 %) | 1 841 | 5 773 | 5 773 | 4 % |
| panel | 224 | 0 (0 %) | 64 | 237 | 381 | 3 % |
| propietarios | 141 | 0 (0 %) | 96 | 369 | 467 | 3 % |
| apoderado | 176 | 0 (0 %) | 54 | 275 | 403 | 2 % |
| cita | 176 | 0 (0 %) | 48 | 195 | 533 | 2 % |
| citas_del_mes | 141 | 0 (0 %) | 47 | 194 | 494 | 2 % |
| mascota | 176 | 0 (0 %) | 34 | 123 | 393 | 1 % |
| lista_de_historias | 130 | 0 (0 %) | 35 | 110 | 186 | 1 % |
| empleados | 141 | 0 (0 %) | 28 | 119 | 154 | 1 % |
| historia | 130 | 0 (0 %) | 31 | 103 | 150 | 1 % |
| navegacion | 224 | 0 (0 %) | 14 | 72 | 176 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 0 | 7 | 444 | 444 | 0 % |
| 0.0 | 14 | 95 | 444 | 1 110 | 0 % |
| 0.3 | 28 | 150 | 502 | 2 191 | 0 % |
| 0.5 | 41 | 238 | 418 | 534 | 0 % |
| 0.8 | 55 | 212 | 407 | 494 | 2 % |
| 1.0 | 68 | 184 | 433 | 1 656 | 7 % |
| 1.3 | 82 | 197 | 1 178 | 2 247 | 9 % |
| 1.5 | 96 | 234 | 2 530 | 3 365 | 12 % |
| 1.8 | 100 | 234 | 2 144 | 2 543 | 14 % |
| 2.0 | 100 | 220 | 1 117 | 2 613 | 10 % |
| 2.3 | 100 | 228 | 3 506 | 5 144 | 11 % |
| 2.5 | 100 | 213 | 2 754 | 5 206 | 12 % |
| 2.8 | 100 | 229 | 2 892 | 3 690 | 13 % |
| 3.0 | 100 | 218 | 1 745 | 3 162 | 13 % |
| 3.3 | 100 | 243 | 685 | 2 729 | 11 % |
| 3.5 | 100 | 211 | 60 | 519 | 14 % |
| 3.8 | 100 | 222 | 47 | 85 | 11 % |
| 4.0 | 100 | 243 | 43 | 53 | 9 % |
| 4.3 | 100 | 214 | 272 | 467 | 11 % |
| 4.5 | 85 | 113 | 245 | 1 399 | 1 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
