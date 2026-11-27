# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 288m

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
| Peticiones hechas | 220 |
| Peticiones correctas (2xx) | 135 |
| Errores (%) | 38.6 % |
| Ritmo medio (peticiones/s) | 5.2 |
| Tiempo mediano (ms) | 9 |
| Tiempo p95 (ms) | 349 |
| Tiempo p99 (ms) | 1 310 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 3 |
| Memoria pico (MB, de 512) | 504 |
| Sentencias SQL | 2 989 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 38 / 50 | 220 | 5.2 | no aplica (ramping-vus) | 38.6 % | 85 | 1 / 1 / 1 | 0 % | 65 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_50`:** k6 alcanzó 38 usuarios activos de 50 previstos; k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 6; fallidas: 18.
- Acciones hechas: panel 7, agenda 1, historia 2, reportes 1, recepcion 16, atencion 2, cobro 0, cancelar 0; sin trabajo disponible: 13.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- CPU del backend: 29 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 8 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 504 MB (98 % de 512 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.2 con unos 23 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **9 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.0 s.
- Correo: el servidor de pruebas recibió 2 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace sigue creciendo** en la segunda mitad: de 102 a 110 MB (+8.3 %, unos +21.7 MB por minuto).
- GC: 230 pausas, 5.3 s en total (10 % del tiempo de vida de la JVM); 19 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 263 MB (tope -Xmx288m).
- Clases cargadas: 24070 en total, 2765 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 2 | 2 | 1313 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 11 | 11 | 3703 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 21 | 21 | 5675 |
| 0.1 | 33 | 34 | 8319 |
| 0.1 | 53 | 54 | 12507 |
| 0.1 | 67 | 68 | 15027 |
| 0.1 | 74 | 74 | 16078 |
| 0.2 | 74 | 75 | 16107 |
| 0.2 | 75 | 76 | 16410 |
| 0.2 | 79 | 80 | 17202 |
| 0.3 | 81 | 82 | 17416 |
| 0.8 | 106 | 108 | 22942 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 717 |
| `java.lang.invoke.LambdaForm$DMH/` | 69 |
| `veterinaria.vargasvet.service.reportes.ReporteActividadClinica$$Lambda/` | 42 |
| `veterinaria.vargasvet.service.impl.UsuarioServiceImpl$$Lambda/` | 28 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 24 |
| `jdk.proxy#.$Proxy#` | 18 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `java.lang.invoke.LambdaForm$BMH/` | 13 |
| `veterinaria.vargasvet.service.impl.DashboardServiceImpl$$Lambda/` | 13 |
| `veterinaria.vargasvet.service.impl.EmpleadoServiceImpl$$Lambda/` | 13 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 799 |
| `veterinaria.vargasvet.service.impl` | 97 |
| `com.fasterxml.jackson.databind` | 91 |
| `veterinaria.vargasvet.service.reportes` | 88 |
| `io.jsonwebtoken.impl.security` | 62 |
| `org.thymeleaf.standard.processor` | 59 |
| `org.eclipse.angus.mail` | 58 |
| `io.jsonwebtoken.impl.lang` | 51 |

**Consultas que más tiempo de PostgreSQL consumen (pg_stat_statements, solo este nivel):**

| Consulta (recortada) | Veces | Tiempo total (s) | Media (ms) | % del tiempo SQL |
|---|---|---|---|---|
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.4 | 175.52 | 32 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 18 | 0.3 | 14.88 | 24 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 18 | 0.1 | 6.75 | 11 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 2 | 0.1 | 36.69 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 18 | 0.0 | 1.73 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 18 | 0.0 | 1.12 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 18 | 0.0 | 0.87 | 1 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1 | 0.0 | 13.13 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM apoderado), $2,(SELECT count(*) FROM mascota), $3,(SELECT count(*) FROM citas), $4,(SELECT count(*) FROM citas WHERE estado=$5), $6,(SELECT count(*) FROM citas WHERE est` | 1 | 0.0 | 13.08 | 1 % |
| `SELECT json_build_object( $1,(SELECT count(*) FROM mascota m LEFT JOIN apoderado a ON a.id=m.apoderado_id WHERE a.id IS NULL), $2,(SELECT count(*) FROM citas c LEFT JOIN mascota m ON m.id=c.mascota_id WHERE m.id IS NULL)` | 1 | 0.0 | 12.27 | 1 % |

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
| Registrar propietario | 2 | 14 | 87.5 % | 0 | 445 | 445 |
| Registrar mascota | 2 | 0 | 0.0 % | 144 | 199 | 199 |
| Agendar cita | 2 | 0 | 0.0 % | 246 | 361 | 361 |
| Recepción (llegada) | 0 | 2 | 100.0 % | 0 | 0 | 0 |
| Iniciar atención | 0 | 2 | 100.0 % | 0 | 0 | 0 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 38 | 20 (53 %) | 0 | 1 356 | 1 550 | 53 % |
| reporte_clinico | 1 | 0 (0 %) | 4 341 | 4 341 | 4 341 | 22 % |
| panel | 25 | 7 (28 %) | 57 | 110 | 179 | 7 % |
| apoderado | 16 | 14 (88 %) | 0 | 445 | 445 | 4 % |
| cumplimiento | 25 | 7 (28 %) | 8 | 37 | 407 | 3 % |
| cita | 2 | 0 (0 %) | 246 | 361 | 361 | 3 % |
| navegacion | 25 | 7 (28 %) | 10 | 43 | 83 | 2 % |
| mascota | 2 | 0 (0 %) | 144 | 199 | 199 | 1 % |
| privacidad | 25 | 7 (28 %) | 10 | 27 | 30 | 1 % |
| avisos_de_cumplimiento | 25 | 7 (28 %) | 10 | 19 | 38 | 1 % |
| notificaciones | 25 | 7 (28 %) | 8 | 25 | 36 | 1 % |
| reporte_de_ventas | 1 | 0 (0 %) | 185 | 185 | 185 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 9 | 70 | 348 | 391 | 0 % |
| 0.2 | 23 | 94 | 961 | 4 341 | 31 % |
| 0.4 | 37 | 55 | 0 | 0 | 100 % |
| 0.7 | 38 | 1 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
