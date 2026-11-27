# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 192m

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
| Usuarios sin degradación | 22 |
| Peticiones hechas | 1 063 |
| Peticiones correctas (2xx) | 849 |
| Errores (%) | 20.1 % |
| Ritmo medio (peticiones/s) | 3.6 |
| Tiempo mediano (ms) | 182 |
| Tiempo p95 (ms) | 30 001 |
| Tiempo p99 (ms) | 30 001 |
| p99 menor a 5000 ms (criterio experimental) | no |
| CPU del backend, media (% del límite de 1 CPU) | 51 |
| Tiempo con la CPU al tope (%) | 44 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 2 |
| Memoria pico (MB, de 512) | 428 |
| Sentencias SQL | 22 234 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 50 / 50 | 1 063 | 3.6 | no aplica (ramping-vus) | 20.1 % | 207 | 0 / 1 / 4 | 0 % | 78 | 0 | **sí** |


## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 225; fallidas: 54.
- Acciones hechas: panel 27, agenda 28, historia 40, reportes 4, recepcion 80, atencion 16, cobro 5, cancelar 11; sin trabajo disponible: 55.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 22 errores `OutOfMemoryError` en el registro.
- CPU del backend: 51 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 44 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 18 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 428 MB (84 % de 512 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 1 % errores 5xx del servidor, 0 % 401 (sesión perdida), 2 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.4 con unos 36 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **22 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Java agotó el heap: 22 errores `OutOfMemoryError: Java heap space`.
- Otros `OutOfMemoryError` que no son de heap ni de Metaspace: 2.
- Correo: el servidor de pruebas recibió 50 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace estable**: en la segunda mitad pasó de 110 a 110 MB (+0.2 %).
- GC: 714 pausas, 120.8 s en total (61 % del tiempo de vida de la JVM); 473 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 185 MB (tope -Xmx192m).
- Clases cargadas: 24962 en total, 281 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 11 | 11 | 3734 |
| 0.1 | 45 | 46 | 10969 |
| 0.2 | 74 | 75 | 16107 |
| 0.3 | 79 | 80 | 17215 |
| 0.9 | 108 | 112 | 23830 |
| 1.1 | 108 | 112 | 23893 |
| 1.8 | 110 | 112 | 24692 |
| 2.0 | 110 | 112 | 24712 |
| 2.2 | 110 | 112 | 24890 |
| 2.4 | 110 | 112 | 24890 |
| 2.6 | 110 | 112 | 24900 |
| 2.9 | 110 | 112 | 24901 |
| 3.1 | 110 | 112 | 24903 |
| 3.3 | 110 | 112 | 24933 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 251 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 260 |
| `org.springframework.security.core` | 2 |
| `org.springframework.dao.support` | 2 |
| `javax.crypto.BadPaddingException` | 1 |
| `veterinaria.vargasvet.security.IndisponibilidadDeBaseDeDatos` | 1 |
| `java.sql.SQLNonTransientConnectionException` | 1 |
| `java.sql.SQLRecoverableException` | 1 |
| `org.springframework.context.support` | 1 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 6 | 1.7 | 289.72 | 32 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 75 | 1.0 | 13.52 | 18 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 75 | 0.8 | 10.39 | 14 % |
| `SELECT hc.id AS "id", hc.numero_hc AS "numeroHc", hc.activa AS "activa", m.id AS "mascotaId", m.nombre_completo AS "mascotaNombre", COALESCE(m.especie, m.otra_especie) AS "especie", r.nombre AS "raza", CONCAT(u.nombre, $` | 23 | 0.4 | 19.53 | 8 % |
| `select e1_0.id,e1_0.company_id,e1_0.created_at,e1_0.estado,e1_0.estado_modificado_por,e1_0.fecha_ingreso,e1_0.fecha_modificacion_estado,e1_0.fecha_nacimiento,e1_0.fecha_salida,e1_0.foto_url,e1_0.genero,e1_0.numero_colegi` | 50 | 0.3 | 5.41 | 5 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 92 | 0.2 | 1.78 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 75 | 0.1 | 1.17 | 2 % |
| `select count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.eliminada=false and (cast($2 as text) is null or c1_0.` | 93 | 0.1 | 0.75 | 1 % |
| `select count(c1_0.id)>$6 from citas c1_0 where c1_0.empleado_id=$1 and (cast($2 as text) is null or c1_0.id<>$3) and c1_0.estado not in ($7,$8) and c1_0.eliminada=$9 and c1_0.fecha_hora_inicio<$4 and c1_0.fecha_hora_fin>` | 50 | 0.1 | 1.19 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 92 | 0.1 | 0.6 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 50 | 50 | coincide |
| Mascotas | 50 | 50 | coincide |
| Citas agendadas | 47 | 47 | coincide |
| Diagnósticos | 9 | 9 | coincide |
| Recetas | 9 | 9 | coincide |
| Pagos | 2 | 2 | coincide |
| Citas completadas | 9 | 9 | coincide |
| Citas canceladas | 7 | 7 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 50 | 30 | 37.5 % | 10 849 | 30 001 | 30 001 |
| Registrar mascota | 50 | 0 | 0.0 % | 47 | 1 765 | 17 136 |
| Agendar cita | 47 | 3 | 6.0 % | 61 | 1 312 | 17 165 |
| Recepción (llegada) | 9 | 7 | 43.8 % | 13 051 | 30 001 | 30 001 |
| Iniciar atención | 9 | 7 | 43.8 % | 911 | 30 001 | 30 001 |
| Leer la consulta | 9 | 0 | 0.0 % | 10 | 868 | 868 |
| Registrar diagnóstico | 9 | 0 | 0.0 % | 13 | 1 081 | 1 081 |
| Registrar receta | 9 | 0 | 0.0 % | 10 | 878 | 878 |
| Actualizar mascota | 9 | 0 | 0.0 % | 13 | 337 | 337 |
| Cerrar la consulta | 9 | 0 | 0.0 % | 30 | 1 449 | 1 449 |
| Caja: crear punto de cobro | 2 | 3 | 60.0 % | 30 000 | 30 001 | 30 001 |
| Caja: vincular equipo | 2 | 0 | 0.0 % | 263 | 487 | 487 |
| Caja: abrir la caja | 2 | 0 | 0.0 % | 710 | 1 392 | 1 392 |
| Cobrar | 2 | 0 | 0.0 % | 227 | 416 | 416 |
| Cancelar cita | 7 | 4 | 36.4 % | 11 025 | 30 001 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| apoderado | 80 | 30 (38 %) | 10 849 | 30 001 | 30 001 | 15 % |
| privacidad | 97 | 22 (23 %) | 79 | 30 001 | 30 001 | 9 % |
| navegacion | 97 | 22 (23 %) | 114 | 30 001 | 30 001 | 9 % |
| lista_de_historias | 40 | 17 (42 %) | 14 736 | 30 001 | 30 001 | 8 % |
| citas_del_mes | 32 | 15 (47 %) | 16 449 | 30 001 | 30 001 | 8 % |
| cumplimiento | 91 | 16 (18 %) | 58 | 30 001 | 30 001 | 7 % |
| notificaciones | 91 | 16 (18 %) | 48 | 30 001 | 30 001 | 7 % |
| empleados | 32 | 15 (47 %) | 8 870 | 30 001 | 30 001 | 6 % |
| login | 54 | 4 (7 %) | 3 971 | 30 000 | 30 001 | 6 % |
| avisos_de_cumplimiento | 87 | 12 (14 %) | 38 | 30 001 | 30 001 | 5 % |
| propietarios | 28 | 11 (39 %) | 1 113 | 30 001 | 30 001 | 5 % |
| checkin | 16 | 7 (44 %) | 13 051 | 30 001 | 30 001 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 63 | 343 | 359 | 0 % |
| 0.2 | 22 | 59 | 1 351 | 2 570 | 0 % |
| 0.4 | 36 | 39 | 21 132 | 23 925 | 8 % |
| 0.7 | 49 | 299 | 10 002 | 22 463 | 0 % |
| 0.9 | 50 | 191 | 47 | 88 | 0 % |
| 1.2 | 50 | 54 | 2 078 | 2 892 | 2 % |
| 1.4 | 50 | 154 | 16 475 | 17 690 | 4 % |
| 1.9 | 50 | 2 | 30 001 | 30 001 | 100 % |
| 2.2 | 50 | 35 | 30 001 | 30 001 | 100 % |
| 2.4 | 50 | 14 | 30 001 | 30 001 | 100 % |
| 2.7 | 50 | 19 | 30 001 | 30 001 | 100 % |
| 2.9 | 50 | 26 | 30 001 | 30 001 | 100 % |
| 3.2 | 50 | 21 | 30 001 | 30 001 | 100 % |
| 3.4 | 50 | 17 | 30 001 | 30 001 | 100 % |
| 3.7 | 47 | 22 | 30 001 | 30 001 | 100 % |
| 3.9 | 39 | 15 | 30 001 | 30 001 | 100 % |
| 4.2 | 36 | 20 | 30 001 | 30 001 | 100 % |
| 4.4 | 29 | 9 | 30 001 | 30 001 | 100 % |
| 4.7 | 16 | 4 | 30 001 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
