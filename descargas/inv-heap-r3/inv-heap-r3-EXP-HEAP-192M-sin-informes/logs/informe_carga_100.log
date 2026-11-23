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
| Usuarios sin degradación | 51 |
| Peticiones hechas | 3 964 |
| Peticiones correctas (2xx) | 3 613 |
| Errores (%) | 8.9 % |
| Ritmo medio (peticiones/s) | 13.0 |
| Tiempo mediano (ms) | 24 |
| Tiempo p95 (ms) | 491 |
| Tiempo p99 (ms) | 1 891 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 48 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 7 |
| Memoria pico (MB, de 512) | 427 |
| Sentencias SQL | 102 675 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 100 / 100 | 3 964 | 13.0 | no aplica (ramping-vus) | 8.9 % | 0 | 1 / 3 / 4 | 0 % | 120 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1358; fallidas: 0.
- Acciones hechas: panel 178, agenda 149, historia 130, reportes 0, recepcion 177, atencion 89, cobro 55, cancelar 35; sin trabajo disponible: 135.
- CPU del backend: 48 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 7 % de media y 21 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 427 MB (83 % de 512 MB).
- Los fallos fueron: 100 % 401 (sesión perdida).
- **Los errores superaron el 5 % por primera vez al minuto 0.9 con unos 65 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **51 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 3.2 s.
- Correo: el servidor de pruebas recibió 177 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 111 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 111 MB (+0.5 %).
- GC: 363 pausas, 7.1 s en total (2 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 25146 en total, 342 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 4 | 4 | 1884 |
| 0.0 | 11 | 11 | 3734 |
| 0.0 | 19 | 19 | 5379 |
| 0.1 | 44 | 44 | 10581 |
| 0.2 | 69 | 69 | 15276 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 75 | 76 | 16410 |
| 0.3 | 79 | 80 | 17209 |
| 0.4 | 93 | 94 | 19777 |
| 1.4 | 111 | 113 | 24251 |
| 2.2 | 110 | 114 | 24643 |
| 2.9 | 110 | 114 | 24887 |
| 3.7 | 111 | 114 | 24984 |
| 4.7 | 111 | 114 | 25100 |

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

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 228 | 4.2 | 18.29 | 26 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 130 | 2.3 | 17.52 | 14 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 228 | 2.2 | 9.48 | 13 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 377 | 1.1 | 2.96 | 7 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.ema` | 601 | 0.5 | 0.89 | 3 % |
| `select u1_0.id,u1_0.activo,u1_0.apellido,u1_0.company_id,u1_0.dni,u1_0.email,u1_0.email_verified,u1_0.nombre,u1_0.username,u1_0.verification_token,u1_0.verification_token_expires_at from usuario u1_0 where upper(u1_0.use` | 651 | 0.5 | 0.81 | 3 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 377 | 0.5 | 1.29 | 3 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 377 | 0.5 | 1.24 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 228 | 0.4 | 1.66 | 2 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 149 | 0.3 | 1.9 | 2 % |

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
| Registrar propietario | 177 | 0 | 0.0 % | 39 | 97 | 173 |
| Registrar mascota | 177 | 0 | 0.0 % | 27 | 64 | 103 |
| Agendar cita | 177 | 0 | 0.0 % | 38 | 104 | 164 |
| Recepción (llegada) | 89 | 0 | 0.0 % | 16 | 66 | 109 |
| Iniciar atención | 89 | 0 | 0.0 % | 23 | 89 | 125 |
| Leer la consulta | 89 | 0 | 0.0 % | 10 | 63 | 122 |
| Registrar diagnóstico | 89 | 0 | 0.0 % | 12 | 56 | 89 |
| Registrar receta | 89 | 0 | 0.0 % | 14 | 67 | 92 |
| Actualizar mascota | 89 | 0 | 0.0 % | 16 | 70 | 268 |
| Cerrar la consulta | 89 | 0 | 0.0 % | 24 | 120 | 176 |
| Caja: crear punto de cobro | 38 | 0 | 0.0 % | 21 | 54 | 68 |
| Caja: vincular equipo | 38 | 0 | 0.0 % | 20 | 50 | 78 |
| Caja: abrir la caja | 38 | 0 | 0.0 % | 22 | 77 | 83 |
| Cobrar | 55 | 0 | 0.0 % | 23 | 55 | 108 |
| Cancelar cita | 35 | 0 | 0.0 % | 33 | 115 | 121 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 401 | 351 (88 %) | 487 | 2 637 | 3 862 | 73 % |
| panel | 228 | 0 (0 %) | 74 | 210 | 270 | 5 % |
| propietarios | 149 | 0 (0 %) | 52 | 252 | 291 | 3 % |
| apoderado | 177 | 0 (0 %) | 39 | 97 | 173 | 2 % |
| cita | 177 | 0 (0 %) | 38 | 104 | 164 | 2 % |
| citas_del_mes | 149 | 0 (0 %) | 34 | 113 | 210 | 2 % |
| lista_de_historias | 130 | 0 (0 %) | 41 | 83 | 96 | 1 % |
| mascota | 177 | 0 (0 %) | 27 | 64 | 103 | 1 % |
| historia | 130 | 0 (0 %) | 29 | 85 | 107 | 1 % |
| empleados | 149 | 0 (0 %) | 16 | 74 | 115 | 1 % |
| cumplimiento | 228 | 0 (0 %) | 10 | 47 | 76 | 1 % |
| navegacion | 228 | 0 (0 %) | 12 | 46 | 58 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 10 | 70 | 439 | 507 | 0 % |
| 0.2 | 24 | 147 | 449 | 743 | 0 % |
| 0.5 | 38 | 185 | 444 | 508 | 0 % |
| 0.7 | 51 | 234 | 409 | 506 | 0 % |
| 0.9 | 65 | 197 | 408 | 527 | 7 % |
| 1.2 | 78 | 193 | 440 | 1 724 | 9 % |
| 1.4 | 92 | 235 | 774 | 1 551 | 11 % |
| 1.7 | 100 | 234 | 2 214 | 2 829 | 12 % |
| 1.9 | 100 | 243 | 729 | 1 879 | 8 % |
| 2.2 | 100 | 204 | 3 205 | 3 959 | 17 % |
| 2.5 | 100 | 239 | 971 | 1 569 | 10 % |
| 2.7 | 100 | 228 | 932 | 1 618 | 11 % |
| 3.0 | 100 | 241 | 824 | 1 232 | 11 % |
| 3.2 | 100 | 231 | 1 063 | 1 812 | 11 % |
| 3.5 | 100 | 207 | 378 | 554 | 14 % |
| 3.7 | 100 | 216 | 99 | 431 | 12 % |
| 4.0 | 100 | 265 | 52 | 68 | 9 % |
| 4.2 | 100 | 219 | 55 | 74 | 12 % |
| 4.5 | 92 | 136 | 53 | 60 | 3 % |
| 4.7 | 47 | 40 | 59 | 62 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
