# Fase 2b: carga con escrituras, 3 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 3 | 3 |
| Peticiones hechas | 86 | 73 |
| Peticiones correctas (2xx) | 86 | 73 |
| Errores (%) | 0.0 % | 0.0 % |
| Ritmo medio (peticiones/s) | 0.8 | 0.6 |
| Tiempo mediano (ms) | 62 | 62 |
| Tiempo p95 (ms) | 412 | 460 |
| CPU del backend, media (%) | 12 | 11 |
| Tiempo con la CPU al tope (%) | 5 | 0 |
| CPU de PostgreSQL, media (%) | 1 | 1 |
| Memoria pico (MB) | 632 | 560 |
| Sentencias SQL | 4 723 | 1 987 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 29; fallidas: 0.
- Acciones hechas: panel 3, agenda 4, historia 2, reportes 0, recepcion 5, atencion 2, cobro 0, cancelar 0; sin trabajo disponible: 2.
- CPU del backend: 12 % de media y 98 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 11 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 632 MB (31 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **3 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.0 s.
- Correo: el servidor de pruebas recibió 5 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 125 MB tras un GC (tope configurado: 256 MB).
- **Metaspace sigue creciendo** en la segunda mitad: de 118 a 125 MB (+5.6 %, unos +6.5 MB por minuto).
- GC: 148 pausas, 4.1 s en total (3 % del tiempo de vida de la JVM); 11 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 241 MB (tope -Xmx1g).
- Clases cargadas: 24288 en total, 515 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 10 | 11 | 3215 |
| 0.1 | 13 | 14 | 3809 |
| 0.1 | 19 | 19 | 4649 |
| 0.1 | 24 | 24 | 5759 |
| 0.1 | 32 | 32 | 7276 |
| 0.2 | 47 | 48 | 10551 |
| 0.2 | 58 | 58 | 12459 |
| 0.3 | 71 | 72 | 14618 |
| 0.3 | 80 | 81 | 16085 |
| 0.4 | 80 | 81 | 16117 |
| 0.4 | 80 | 81 | 16128 |
| 0.5 | 86 | 87 | 17103 |
| 0.5 | 88 | 88 | 17311 |
| 1.1 | 118 | 120 | 22945 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 301 |
| `java.lang.invoke.LambdaForm$DMH/` | 16 |
| `org.postgresql.jdbc.ArrayEncoding$#` | 15 |
| `veterinaria.vargasvet.service.ClinicalTemplateService$$Lambda/` | 15 |
| `veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl$$Lambda/` | 13 |
| `org.hibernate.persister.entity.mutation.UpdateCoordinatorStandard$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.impl.ControlPreventivoServiceImpl$$Lambda/` | 5 |
| `org.hibernate.query.results.ResultSetMappingImpl$$Lambda/` | 4 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 318 |
| `veterinaria.vargasvet.service.impl` | 28 |
| `org.hibernate.query.results` | 21 |
| `org.postgresql.jdbc.ArrayEncoding` | 20 |
| `org.hibernate.query.sql` | 18 |
| `veterinaria.vargasvet.service.ClinicalTemplateService` | 15 |
| `org.hibernate.query.sqm` | 12 |
| `org.hibernate.validator.internal` | 11 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 5 | 5 | coincide |
| Mascotas | 5 | 5 | coincide |
| Citas agendadas | 5 | 5 | coincide |
| Diagnósticos | 2 | 2 | coincide |
| Recetas | 2 | 2 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 2 | 2 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 5 | 0 | 0.0 % | 86 | 245 |
| Registrar mascota | 5 | 0 | 0.0 % | 60 | 197 |
| Agendar cita | 5 | 0 | 0.0 % | 97 | 308 |
| Recepción (llegada) | 2 | 0 | 0.0 % | 45 | 52 |
| Iniciar atención | 2 | 0 | 0.0 % | 59 | 72 |
| Leer la consulta | 2 | 0 | 0.0 % | 163 | 294 |
| Registrar diagnóstico | 2 | 0 | 0.0 % | 54 | 70 |
| Registrar receta | 2 | 0 | 0.0 % | 58 | 77 |
| Actualizar mascota | 2 | 0 | 0.0 % | 47 | 57 |
| Cerrar la consulta | 2 | 0 | 0.0 % | 127 | 189 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 3 | 0 (0 %) | 449 | 1 049 | 19 % |
| panel | 6 | 0 (0 %) | 255 | 579 | 18 % |
| citas_del_mes | 4 | 0 (0 %) | 204 | 354 | 9 % |
| propietarios | 4 | 0 (0 %) | 149 | 454 | 9 % |
| cita | 5 | 0 (0 %) | 97 | 308 | 7 % |
| apoderado | 5 | 0 (0 %) | 86 | 245 | 6 % |
| mascota | 5 | 0 (0 %) | 60 | 197 | 4 % |
| consulta | 2 | 0 (0 %) | 163 | 294 | 3 % |
| navegacion | 6 | 0 (0 %) | 33 | 164 | 3 % |
| empleados | 4 | 0 (0 %) | 61 | 77 | 2 % |
| cerrar_consulta | 2 | 0 (0 %) | 127 | 189 | 2 % |
| avisos_de_cumplimiento | 6 | 0 (0 %) | 35 | 77 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 2 | 14 | 1 049 | 0 % |
| 0.1 | 3 | 13 | 454 | 0 % |
| 0.4 | 3 | 9 | 135 | 0 % |
| 0.7 | 3 | 12 | 233 | 0 % |
| 0.9 | 3 | 9 | 176 | 0 % |
| 1.1 | 3 | 16 | 294 | 0 % |
| 1.4 | 3 | 13 | 240 | 0 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 13; fallidas: 0.
- Acciones hechas: panel 3, agenda 2, historia 5, reportes 0, recepcion 4, atencion 0, cobro 0, cancelar 1; sin trabajo disponible: 1.
- CPU del backend: 11 % de media y 82 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 6 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 560 MB (27 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **3 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 4 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 122 MB tras un GC (tope configurado: 256 MB).
- **Metaspace sigue creciendo** en la segunda mitad: de 115 a 122 MB (+6.2 %, unos +10.0 MB por minuto).
- GC: 161 pausas, 4.8 s en total (3 % del tiempo de vida de la JVM); 11 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 211 MB (tope -Xmx1g).
- Clases cargadas: 23811 en total, 1171 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 12 | 12 | 3594 |
| 0.1 | 14 | 15 | 3954 |
| 0.1 | 20 | 20 | 4881 |
| 0.1 | 26 | 27 | 6223 |
| 0.2 | 41 | 41 | 9193 |
| 0.2 | 57 | 57 | 12138 |
| 0.3 | 71 | 72 | 14596 |
| 0.4 | 80 | 81 | 16064 |
| 0.4 | 80 | 81 | 16099 |
| 0.4 | 80 | 81 | 16102 |
| 0.5 | 85 | 86 | 17008 |
| 0.6 | 87 | 88 | 17204 |
| 0.6 | 89 | 90 | 17446 |
| 1.6 | 122 | 124 | 23657 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 263 |
| `java.lang.invoke.LambdaForm$DMH/` | 18 |
| `veterinaria.vargasvet.service.impl.CitaServiceImpl$$Lambda/` | 14 |
| `veterinaria.vargasvet.service.impl.HistoriaClinicaServiceImpl$$Lambda/` | 11 |
| `jdk.proxy#.$Proxy#` | 10 |
| `jakarta.mail.Session$#` | 8 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 8 |
| `veterinaria.vargasvet.service.PetOwnershipService$$Lambda/` | 8 |
| `org.thymeleaf.util.ExpressionUtils$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.impl.AuditLogServiceImpl$$Lambda/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 284 |
| `org.thymeleaf.standard.processor` | 59 |
| `org.eclipse.angus.mail` | 58 |
| `veterinaria.vargasvet.service.impl` | 50 |
| `org.thymeleaf.standard.expression` | 50 |
| `org.thymeleaf.spring6.expression` | 20 |
| `org.thymeleaf.spring6.processor` | 19 |
| `org.hibernate.query.sql` | 17 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 4 | 4 | coincide |
| Mascotas | 4 | 4 | coincide |
| Citas agendadas | 4 | 4 | coincide |
| Diagnósticos | 0 | 0 | coincide |
| Recetas | 0 | 0 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 1 | 1 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 4 | 0 | 0.0 % | 137 | 512 |
| Registrar mascota | 4 | 0 | 0.0 % | 108 | 234 |
| Agendar cita | 4 | 0 | 0.0 % | 134 | 430 |
| Cancelar cita | 1 | 0 | 0.0 % | 65 | 65 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 3 | 0 (0 %) | 470 | 1 292 | 25 % |
| panel | 6 | 0 (0 %) | 203 | 601 | 17 % |
| apoderado | 4 | 0 (0 %) | 137 | 512 | 10 % |
| cita | 4 | 0 (0 %) | 134 | 430 | 9 % |
| mascota | 4 | 0 (0 %) | 108 | 234 | 6 % |
| navegacion | 6 | 0 (0 %) | 44 | 207 | 5 % |
| historia | 5 | 0 (0 %) | 63 | 138 | 4 % |
| lista_de_historias | 5 | 0 (0 %) | 54 | 132 | 4 % |
| privacidad | 6 | 0 (0 %) | 42 | 85 | 3 % |
| avisos_de_cumplimiento | 6 | 0 (0 %) | 37 | 99 | 3 % |
| propietarios | 2 | 0 (0 %) | 135 | 137 | 3 % |
| cumplimiento | 6 | 0 (0 %) | 36 | 81 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 601 | 0 % |
| 0.2 | 3 | 12 | 193 | 0 % |
| 0.5 | 3 | 9 | 512 | 0 % |
| 0.7 | 3 | 9 | 145 | 0 % |
| 0.9 | 3 | 7 | 133 | 0 % |
| 1.2 | 3 | 12 | 151 | 0 % |
| 1.4 | 3 | 3 | 54 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
