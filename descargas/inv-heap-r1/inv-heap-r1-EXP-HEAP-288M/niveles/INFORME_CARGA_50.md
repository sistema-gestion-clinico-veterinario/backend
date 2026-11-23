# Fase 3: 50 usuarios simultáneos, 1 CPU, 512 MB, heap 288m

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 6 |
| Peticiones hechas | 220 |
| Peticiones correctas (2xx) | 135 |
| Errores (%) | 38.6 % |
| Ritmo medio (peticiones/s) | 5.2 |
| Tiempo mediano (ms) | 10 |
| Tiempo p95 (ms) | 297 |
| Tiempo p99 (ms) | 1 252 |
| p99 menor a 5000 ms (criterio experimental) | sí |
| CPU del backend, media (% del límite de 1 CPU) | 27 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 3 |
| Memoria pico (MB, de 512) | 504 |
| Sentencias SQL | 3 030 |
| Reinicios del contenedor | 0 |

## Salud del generador de carga (criterio experimental)

| Etiqueta | Usuarios activos / previstos | Peticiones | Pet/s | Descartadas | No 2xx | Sin respuesta | CPU de k6 (% de su capacidad): media / p95 / máx | Muestras ≥ umbral | RSS pico (MB) | Cód. k6 | ¿Interpretable? |
|---|---|---|---|---|---|---|---|---|---|---|---|
| despues_50 | 38 / 50 | 220 | 5.2 | no aplica (ramping-vus) | 38.6 % | 84 | 0 / 1 / 1 | 0 % | 66 | 105 | **potencialmente no** |

> **Resultado potencialmente no interpretable en `despues_50`:** k6 alcanzó 38 usuarios activos de 50 previstos; k6 terminó con código 105

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 6; fallidas: 18.
- Acciones hechas: panel 7, agenda 1, historia 2, reportes 1, recepcion 17, atencion 1, cobro 0, cancelar 0; sin trabajo disponible: 13.
- **El contenedor se quedó sin memoria y el sistema lo mató (OOMKilled).** Reinicios: 0.
- Al terminar el backend **ya no estaba corriendo** (código de salida 137).
- CPU del backend: 27 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 10 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 504 MB (98 % de 512 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.1 con unos 19 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **6 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 3 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 110 MB tras un GC.
- **Metaspace sigue creciendo** en la segunda mitad: de 102 a 110 MB (+8.3 %, unos +21.9 MB por minuto).
- GC: 224 pausas, 5.7 s en total (12 % del tiempo de vida de la JVM); 18 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 263 MB (tope -Xmx288m).
- Clases cargadas: 24078 en total, 2772 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 0 | 1 | 844 |
| 0.0 | 2 | 2 | 1316 |
| 0.0 | 6 | 6 | 2323 |
| 0.0 | 11 | 11 | 3703 |
| 0.0 | 12 | 13 | 3987 |
| 0.0 | 21 | 21 | 5680 |
| 0.1 | 33 | 34 | 8293 |
| 0.1 | 53 | 54 | 12515 |
| 0.1 | 68 | 69 | 15266 |
| 0.1 | 74 | 74 | 16078 |
| 0.2 | 74 | 75 | 16115 |
| 0.2 | 79 | 80 | 17088 |
| 0.3 | 79 | 80 | 17210 |
| 0.3 | 87 | 89 | 18341 |
| 0.8 | 110 | 112 | 23904 |

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
| `select distinct c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre` | 2 | 0.3 | 152.5 | 38 % |
| `select count(c1_0.id),coalesce(sum(case when cast(c1_0.fecha_hora_inicio as date)=current_date then 1 else 0 end),0) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoder` | 18 | 0.2 | 9.1 | 20 % |
| `select cast(c1_0.fecha_hora_inicio as date),count(c1_0.id) from citas c1_0 join mascota m1_0 on m1_0.id=c1_0.mascota_id join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1 and c1_0.estado not in ('C` | 18 | 0.1 | 6.05 | 14 % |
| `select p1_0.id,p1_0.business_trace_id,c1_0.id,c1_0.business_trace_id,c1_0.creado_por_user_id,c1_0.created_at,c1_0.duracion_minutos,c1_0.eliminada,c1_0.eliminado_at,c1_0.eliminado_por_user_id,c1_0.empleado_id,c1_0.es_emer` | 2 | 0.1 | 29.09 | 7 % |
| `select a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_0.fecha_ingreso,a1_0.fecha_modificacio` | 18 | 0.0 | 1.33 | 3 % |
| `select m1_0.id,m1_0.nombre_completo,m1_0.especie,r1_0.nombre from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id left join raza r1_0 on r1_0.id=m1_0.raza_id where a1_0.company_id=$1 order by m1_0.nombre_co` | 18 | 0.0 | 0.95 | 2 % |
| `select count(a1_0.id) from apoderado a1_0 join usuario u1_0 on u1_0.id=a1_0.user_id where a1_0.company_id=$1 and ($2 is null or lower(cast((u1_0.nombre//' '//u1_0.apellido) as text)) like lower(cast(('%'//replace(cast($3` | 18 | 0.0 | 0.72 | 2 % |
| `select m1_0.id,m1_0.activo,m1_0.apoderado_id,a1_0.id,a1_0.acceso_portal_modificado_en,a1_0.acceso_portal_modificado_por,a1_0.company_id,a1_0.created_at,a1_0.estado,a1_0.estado_acceso_portal,a1_0.estado_modificado_por,a1_` | 1 | 0.0 | 10.32 | 1 % |
| `select count(m1_0.id) from mascota m1_0 join apoderado a1_0 on a1_0.id=m1_0.apoderado_id where a1_0.company_id=$1` | 18 | 0.0 | 0.52 | 1 % |
| `select c1_0.id,c1_0.business_trace_id,c2_0.id,c2_0.anamnesis,c2_0.cerrado_por,c2_0.cita_id,c2_0.clinical_template_id,c2_0.created_at,c2_0.desparasitacion_aplicada,c2_0.estado,c2_0.examen_fisico,c2_0.fecha_cierre,c2_0.fec` | 18 | 0.0 | 0.43 | 1 % |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 3 | 3 | coincide |
| Mascotas | 2 | 3 | la base tiene más (ver nota) |
| Citas agendadas | 1 | 1 | coincide |
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
| Registrar propietario | 3 | 14 | 82.4 % | 0 | 370 | 370 |
| Registrar mascota | 2 | 1 | 33.3 % | 124 | 174 | 174 |
| Agendar cita | 1 | 1 | 50.0 % | 237 | 358 | 358 |
| Recepción (llegada) | 0 | 1 | 100.0 % | 0 | 0 | 0 |
| Iniciar atención | 0 | 1 | 100.0 % | 0 | 0 | 0 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | p99 (ms) | % del tiempo total |
|---|---|---|---|---|---|---|
| login | 38 | 20 (53 %) | 0 | 1 277 | 1 306 | 51 % |
| reporte_clinico | 1 | 0 (0 %) | 4 229 | 4 229 | 4 229 | 25 % |
| panel | 25 | 7 (28 %) | 48 | 132 | 165 | 7 % |
| apoderado | 17 | 14 (82 %) | 0 | 370 | 370 | 3 % |
| cita | 2 | 1 (50 %) | 237 | 358 | 358 | 3 % |
| mascota | 3 | 1 (33 %) | 124 | 174 | 174 | 2 % |
| avisos_de_cumplimiento | 25 | 7 (28 %) | 11 | 32 | 50 | 2 % |
| privacidad | 25 | 7 (28 %) | 11 | 27 | 45 | 2 % |
| navegacion | 25 | 7 (28 %) | 10 | 33 | 40 | 2 % |
| cumplimiento | 25 | 7 (28 %) | 9 | 39 | 44 | 2 % |
| notificaciones | 25 | 7 (28 %) | 8 | 24 | 25 | 1 % |
| reporte_de_ventas | 1 | 0 (0 %) | 157 | 157 | 157 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | p99 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | 6 | 42 | 281 | 299 | 0 % |
| 0.1 | 19 | 108 | 559 | 1 306 | 14 % |
| 0.4 | 33 | 51 | 0 | 0 | 100 % |
| 0.6 | 38 | 19 | 0 | 0 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción. Con menos de 100 peticiones el p99 coincide con el máximo.
