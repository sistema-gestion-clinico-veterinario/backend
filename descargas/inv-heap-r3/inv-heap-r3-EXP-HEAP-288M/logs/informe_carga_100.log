# Fase 3: 100 usuarios simultáneos, 1 CPU, 512 MB, heap 288m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 288m; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: EXP-HEAP-288M (experimental: -Xmx288m en un contenedor de 512m; el resto de las opciones es el de producción) (memoria 512m, CPU 1, heap máximo 288m; configuración de la JVM en los niveles: H, en el control: H)
- usadas en esta corrida: H para los niveles y H para el control (perfil EXP-HEAP-288M (experimental: -Xmx288m en un contenedor de 512m; el resto de las opciones es el de producción))
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: b1d22f951f756e136a366262625c79e2e95b7e5b

| | DESPUÉS |
|---|---|
| Veredicto | FALLA |
| Usuarios sin degradación | 9 |
| Peticiones hechas | 213 |
| Peticiones correctas (2xx) | 128 |
| Errores (%) | 39.9 % |
| Ritmo medio (peticiones/s) | 5.1 |
| Tiempo mediano (ms) | 10 |
| Tiempo p95 (ms) | 353 |
| Tiempo p99 (ms) | 1 315 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 22 |
| Tiempo con la CPU al tope (%) | 11 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 2 |
| Memoria pico (MB, de 512) | 492 |
| Sentencias SQL | 2 819 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_100 | 38 / 100 | 213 | 5.1 | no aplica (ramping-vus) | 39.9 % | 85 | 1 / 1 / 2 | 0 % | 89 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_100`:** k6 alcanzó 38 usuarios activos de 100 previstos; k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 6; fallidas: 17.
- Acciones hechas: panel 7, agenda 1, historia 2, reportes 1, recepcion 15, atencion 2, cobro 0, cancelar 0; sin trabajo disponible: 12.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- CPU del backend: 22 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 4 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 492 MB (96 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.2 con unos 22 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.0 s.
- Correo: el servidor de pruebas recibió 2 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace sigue creciendo** en la segunda mitad: de 102 a 110 MB (+8.2 %, unos +21.5 MB por minuto).
- GC: 221 pausas, 5.3 s en total (11 % del tiempo de vida de la JVM); 18 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 265 MB (tope -Xmx288m).
- Clases cargadas: 24071 en total, 2515 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 2 | 2 | 1298 |
| 0.0 | 5 | 5 | 2132 |
| 0.0 | 10 | 11 | 3591 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 18 | 18 | 5038 |
| 0.1 | 26 | 27 | 6882 |
| 0.1 | 45 | 46 | 10966 |
| 0.1 | 61 | 61 | 13679 |
| 0.1 | 73 | 74 | 16051 |
| 0.2 | 74 | 75 | 16103 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17214 |
| 0.3 | 98 | 100 | 20847 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 703 |
| `java.lang.invoke.LambdaForm$DMH/` | 65 |
| `veterinaria.vargasvet.service.reportes.ReporteActividadClinica$$Lambda/` | 42 |
| `veterinaria.vargasvet.service.impl.UsuarioServiceImpl$$Lambda/` | 24 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 21 |
| `jdk.proxy#.$Proxy#` | 18 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `veterinaria.vargasvet.service.impl.DashboardServiceImpl$$Lambda/` | 13 |
| `veterinaria.vargasvet.service.impl.EmpleadoServiceImpl$$Lambda/` | 13 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 13 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 779 |
| `veterinaria.vargasvet.service.impl` | 93 |
| `veterinaria.vargasvet.service.reportes` | 88 |
| `io.jsonwebtoken.impl.security` | 62 |
| `org.thymeleaf.standard.processor` | 59 |
| `org.eclipse.angus.mail` | 58 |
| `io.jsonwebtoken.impl.lang` | 51 |
| `org.thymeleaf.standard.expression` | 50 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.4 | 179.73 | 36 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 17 | 0.2 | 12.42 | 21 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 17 | 0.1 | 7.66 | 13 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 2 | 0.1 | 40.77 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 17 | 0.0 | 1.52 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 17 | 0.0 | 1.17 | 2 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1 | 0.0 | 13.76 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 12.99 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 12.75 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 17 | 0.0 | 0.63 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 2 | 2 | coincide |
| Mascotas | 2 | 2 | coincide |
| Citas agendadas | 2 | 2 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|
| Registrar propietario | 2 | 13 | 86.7 % | 0 | 441 | 441 |
| Registrar mascota | 2 | 0 | 0.0 % | 148 | 203 | 203 |
| Agendar cita | 2 | 0 | 0.0 % | 257 | 386 | 386 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 0 | 0 | 0 |
| Iniciar atención | 0 | 2 | 100.0 % | 0 | 0 | 0 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 38 | 21 (55 %) | 0 | 1 335 | 1 580 | 53 % |
| reporte_clinico | 1 | 0 (0 %) | 4 539 | 4 539 | 4 539 | 23 % |
| panel | 24 | 7 (29 %) | 56 | 145 | 181 | 7 % |
| apoderado | 15 | 13 (87 %) | 0 | 441 | 441 | 3 % |
| notificaciones | 24 | 7 (29 %) | 8 | 28 | 411 | 3 % |
| cita | 2 | 0 (0 %) | 257 | 386 | 386 | 3 % |
| mascota | 2 | 0 (0 %) | 148 | 203 | 203 | 2 % |
| privacidad | 24 | 7 (29 %) | 10 | 28 | 52 | 1 % |
| navegacion | 24 | 7 (29 %) | 11 | 34 | 43 | 1 % |
| avisos_de_cumplimiento | 24 | 7 (29 %) | 11 | 26 | 52 | 1 % |
| cumplimiento | 24 | 7 (29 %) | 8 | 34 | 45 | 1 % |
| reporte_de_ventas | 1 | 0 (0 %) | 155 | 155 | 155 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 63 | 350 | 370 | 0 % |
| 0.2 | 22 | 88 | 1 037 | 4 539 | 26 % |
| 0.4 | 36 | 48 | 0 | 0 | 100 % |
| 0.7 | 38 | 14 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
