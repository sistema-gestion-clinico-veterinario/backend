# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 288m

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 288m; núcleos de k6: 1; reservas de horario por usuario: 14
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
| Usuarios sin degradación | 15 |
| Peticiones hechas | 215 |
| Peticiones correctas (2xx) | 128 |
| Errores (%) | 40.5 % |
| Ritmo medio (peticiones/s) | 5.1 |
| Tiempo mediano (ms) | 11 |
| Tiempo p95 (ms) | 468 |
| Tiempo p99 (ms) | 1 886 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 6 |
| Memoria pico (MB, de 512) | 502 |
| Sentencias SQL | 2 966 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 38 / 50 | 215 | 5.1 | no aplica (ramping-vus) | 40.5 % | 87 | 0 / 2 / 2 | 0 % | 65 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_50`:** k6 alcanzó 38 usuarios activos de 50 previstos; k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 9; fallidas: 16.
- Acciones hechas: panel 7, agenda 1, historia 2, reportes 1, recepcion 15, atencion 2, cobro 0, cancelar 0; sin trabajo disponible: 12.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- CPU del backend: 29 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 29 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 502 MB (98 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.3 con unos 29 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **15 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.7 s.
- Correo: el servidor de pruebas recibió 3 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace sigue creciendo** en la segunda mitad: de 102 a 110 MB (+8.1 %, unos +19.8 MB por minuto).
- GC: 221 pausas, 5.5 s en total (10 % del tiempo de vida de la JVM); 18 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 264 MB (tope -Xmx288m).
- Clases cargadas: 24018 en total, 2711 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 2 | 2 | 1298 |
| 0.0 | 5 | 5 | 2132 |
| 0.0 | 10 | 11 | 3591 |
| 0.0 | 11 | 12 | 3804 |
| 0.0 | 18 | 18 | 5061 |
| 0.1 | 25 | 26 | 6733 |
| 0.1 | 45 | 46 | 10918 |
| 0.1 | 61 | 61 | 13659 |
| 0.2 | 73 | 74 | 16048 |
| 0.2 | 74 | 75 | 16104 |
| 0.2 | 74 | 75 | 16115 |
| 0.3 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17215 |
| 0.4 | 99 | 101 | 21047 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 703 |
| `java.lang.invoke.LambdaForm$DMH/` | 66 |
| `veterinaria.vargasvet.service.reportes.ReporteActividadClinica$$Lambda/` | 42 |
| `veterinaria.vargasvet.service.impl.UsuarioServiceImpl$$Lambda/` | 28 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 24 |
| `jdk.proxy#.$Proxy#` | 16 |
| `veterinaria.vargasvet.service.impl.DashboardServiceImpl$$Lambda/` | 13 |
| `veterinaria.vargasvet.service.impl.EmpleadoServiceImpl$$Lambda/` | 13 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 13 |
| `org.springframework.web.util.HierarchicalUriComponents$Type$#` | 12 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 781 |
| `veterinaria.vargasvet.service.impl` | 97 |
| `com.fasterxml.jackson.databind` | 91 |
| `veterinaria.vargasvet.service.reportes` | 63 |
| `io.jsonwebtoken.impl.security` | 62 |
| `org.thymeleaf.standard.processor` | 59 |
| `org.eclipse.angus.mail` | 58 |
| `io.jsonwebtoken.impl.lang` | 51 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.4 | 199.21 | 33 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 17 | 0.3 | 15.57 | 22 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 17 | 0.2 | 9.4 | 13 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 2 | 0.1 | 45.98 | 8 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 17 | 0.0 | 2.0 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 17 | 0.0 | 1.53 | 2 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 16.78 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 15.93 | 1 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 17 | 0.0 | 0.93 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 17 | 0.0 | 0.67 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 3 | 3 | coincide |
| Mascotas | 3 | 3 | coincide |
| Citas agendadas | 3 | 3 | coincide |
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
| Registrar propietario | 3 | 12 | 80.0 % | 0 | 510 | 510 |
| Registrar mascota | 3 | 0 | 0.0 % | 101 | 202 | 202 |
| Agendar cita | 3 | 0 | 0.0 % | 138 | 458 | 458 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 0 | 0 | 0 |
| Iniciar atención | 0 | 2 | 100.0 % | 0 | 0 | 0 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 38 | 21 (55 %) | 209 | 1 914 | 2 044 | 57 % |
| reporte_clinico | 1 | 1 (100 %) | 5 783 | 5 783 | 5 783 | 21 % |
| panel | 24 | 7 (29 %) | 68 | 166 | 564 | 7 % |
| apoderado | 15 | 12 (80 %) | 0 | 510 | 510 | 4 % |
| cita | 3 | 0 (0 %) | 138 | 458 | 458 | 3 % |
| mascota | 3 | 0 (0 %) | 101 | 202 | 202 | 1 % |
| navegacion | 24 | 7 (29 %) | 12 | 37 | 45 | 1 % |
| privacidad | 24 | 7 (29 %) | 12 | 40 | 44 | 1 % |
| avisos_de_cumplimiento | 24 | 7 (29 %) | 12 | 33 | 46 | 1 % |
| cumplimiento | 24 | 7 (29 %) | 11 | 35 | 59 | 1 % |
| notificaciones | 24 | 7 (29 %) | 9 | 26 | 42 | 1 % |
| reporte_de_ventas | 1 | 1 (100 %) | 0 | 0 | 0 | 0 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 1 | 9 | 475 | 475 | 0 % |
| 0.0 | 15 | 95 | 457 | 1 446 | 0 % |
| 0.3 | 29 | 69 | 1 717 | 5 783 | 65 % |
| 0.5 | 38 | 42 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
