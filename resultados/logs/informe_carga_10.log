# Fase 2b: carga con escrituras, 10 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 10 | 10 |
| Peticiones hechas | 502 | 539 |
| Peticiones correctas (2xx) | 502 | 539 |
| Errores (%) | 0.0 % | 0.0 % |
| Ritmo medio (peticiones/s) | 2.2 | 2.4 |
| Tiempo mediano (ms) | 35 | 33 |
| Tiempo p95 (ms) | 294 | 353 |
| CPU del backend, media (%) | 17 | 22 |
| Tiempo con la CPU al tope (%) | 2 | 2 |
| CPU de PostgreSQL, media (%) | 6 | 3 |
| Memoria pico (MB) | 817 | 653 |
| Sentencias SQL | 36 865 | 14 378 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 180; fallidas: 0.
- Acciones hechas: panel 23, agenda 18, historia 15, reportes 5, recepcion 25, atencion 11, cobro 8, cancelar 5; sin trabajo disponible: 18.
- CPU del backend: 17 % de media y 97 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 42 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 817 MB (40 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.8 s.
- Correo: el servidor de pruebas recibió 25 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 130 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 128 a 130 MB (+1.9 %).
- GC: 184 pausas, 8.0 s en total (3 % del tiempo de vida de la JVM); 15 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 368 MB (tope -Xmx1g).
- Clases cargadas: 24884 en total, 252 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 12 | 13 | 3620 |
| 0.1 | 15 | 15 | 4031 |
| 0.1 | 21 | 22 | 5294 |
| 0.1 | 30 | 30 | 6932 |
| 0.2 | 49 | 50 | 10939 |
| 0.3 | 65 | 66 | 13611 |
| 0.3 | 77 | 78 | 15742 |
| 0.4 | 80 | 81 | 16092 |
| 0.4 | 80 | 81 | 16120 |
| 0.4 | 82 | 83 | 16424 |
| 0.5 | 86 | 87 | 17120 |
| 0.6 | 92 | 93 | 17898 |
| 1.3 | 124 | 126 | 23978 |
| 2.6 | 128 | 131 | 24757 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 198 |
| `veterinaria.vargasvet.service.PuntoCobroService$$Lambda/` | 7 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |
| `veterinaria.vargasvet.service.impl.CajaServiceImpl$$Lambda/` | 5 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 204 |
| `org.hibernate.query.sqm` | 12 |
| `com.fasterxml.jackson.databind` | 11 |
| `veterinaria.vargasvet.service.impl` | 9 |
| `veterinaria.vargasvet.service.PuntoCobroService` | 7 |
| `org.hibernate.action.internal` | 2 |
| `org.hibernate.engine.spi` | 2 |
| `org.hibernate.sql.exec` | 2 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 25 | 25 | coincide |
| Mascotas | 25 | 25 | coincide |
| Citas agendadas | 25 | 25 | coincide |
| Diagnósticos | 11 | 11 | coincide |
| Recetas | 11 | 11 | coincide |
| Pagos | 8 | 8 | coincide |
| Citas completadas | 11 | 11 | coincide |
| Citas canceladas | 5 | 5 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 25 | 0 | 0.0 % | 58 | 118 |
| Registrar mascota | 25 | 0 | 0.0 % | 40 | 74 |
| Agendar cita | 25 | 0 | 0.0 % | 54 | 94 |
| Recepción (llegada) | 11 | 0 | 0.0 % | 28 | 45 |
| Iniciar atención | 11 | 0 | 0.0 % | 36 | 50 |
| Leer la consulta | 11 | 0 | 0.0 % | 23 | 32 |
| Registrar diagnóstico | 11 | 0 | 0.0 % | 25 | 53 |
| Registrar receta | 11 | 0 | 0.0 % | 30 | 70 |
| Actualizar mascota | 11 | 0 | 0.0 % | 30 | 55 |
| Cerrar la consulta | 11 | 0 | 0.0 % | 55 | 223 |
| Caja: crear punto de cobro | 5 | 0 | 0.0 % | 27 | 42 |
| Caja: vincular equipo | 5 | 0 | 0.0 % | 22 | 66 |
| Caja: abrir la caja | 5 | 0 | 0.0 % | 26 | 53 |
| Cobrar | 8 | 0 | 0.0 % | 26 | 62 |
| Cancelar cita | 5 | 0 | 0.0 % | 21 | 48 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| reporte_clinico | 5 | 0 (0 %) | 2 325 | 5 700 | 30 % |
| panel | 33 | 0 (0 %) | 256 | 601 | 17 % |
| login | 10 | 0 (0 %) | 404 | 1 150 | 11 % |
| citas_del_mes | 18 | 0 (0 %) | 97 | 304 | 5 % |
| propietarios | 18 | 0 (0 %) | 101 | 164 | 4 % |
| cita | 25 | 0 (0 %) | 54 | 94 | 4 % |
| apoderado | 25 | 0 (0 %) | 58 | 118 | 3 % |
| mascota | 25 | 0 (0 %) | 40 | 74 | 3 % |
| reporte_de_ventas | 5 | 0 (0 %) | 194 | 347 | 2 % |
| avisos_de_cumplimiento | 33 | 0 (0 %) | 22 | 103 | 2 % |
| navegacion | 33 | 0 (0 %) | 23 | 137 | 2 % |
| notificaciones | 33 | 0 (0 %) | 20 | 60 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 601 | 0 % |
| 0.2 | 8 | 40 | 398 | 0 % |
| 0.4 | 10 | 41 | 733 | 0 % |
| 0.7 | 10 | 32 | 211 | 0 % |
| 0.9 | 10 | 37 | 276 | 0 % |
| 1.2 | 10 | 41 | 186 | 0 % |
| 1.4 | 10 | 56 | 182 | 0 % |
| 1.7 | 10 | 41 | 294 | 0 % |
| 1.9 | 10 | 23 | 82 | 0 % |
| 2.2 | 10 | 50 | 286 | 0 % |
| 2.4 | 10 | 41 | 90 | 0 % |
| 2.7 | 10 | 28 | 314 | 0 % |
| 2.9 | 10 | 29 | 258 | 0 % |
| 3.2 | 8 | 19 | 256 | 0 % |
| 3.4 | 3 | 3 | 1 809 | 0 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 202; fallidas: 0.
- Acciones hechas: panel 23, agenda 19, historia 23, reportes 1, recepcion 21, atencion 14, cobro 10, cancelar 4; sin trabajo disponible: 20.
- CPU del backend: 22 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 33 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 653 MB (32 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.8 s.
- Correo: el servidor de pruebas recibió 21 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 130 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 127 a 130 MB (+2.4 %).
- GC: 184 pausas, 7.0 s en total (3 % del tiempo de vida de la JVM); 17 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 228 MB (tope -Xmx1g).
- Clases cargadas: 24781 en total, 529 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 12 | 13 | 3665 |
| 0.1 | 15 | 15 | 4049 |
| 0.1 | 22 | 22 | 5456 |
| 0.1 | 32 | 32 | 7225 |
| 0.2 | 50 | 50 | 10965 |
| 0.3 | 65 | 66 | 13627 |
| 0.3 | 80 | 81 | 16044 |
| 0.4 | 80 | 81 | 16074 |
| 0.4 | 80 | 81 | 16103 |
| 0.4 | 82 | 83 | 16395 |
| 0.5 | 86 | 87 | 17103 |
| 0.5 | 87 | 88 | 17206 |
| 0.7 | 98 | 99 | 18764 |
| 3.0 | 127 | 130 | 24368 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 360 |
| `veterinaria.vargasvet.service.reportes.ReporteActividadClinica$$Lambda/` | 42 |
| `java.lang.invoke.LambdaForm$DMH/` | 19 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$$Lambda/` | 17 |
| `veterinaria.vargasvet.service.reportes.ReportePreventivos$$Lambda/` | 10 |
| `java.util.stream.Collectors$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.reportes.ReporteIngresos$$Lambda/` | 6 |
| `veterinaria.vargasvet.service.reportes.ReportePacientesInactivos$$Lambda/` | 6 |
| `java.util.stream.LongPipeline$$Lambda/` | 4 |
| `veterinaria.vargasvet.service.reportes.ReporteVentasProductos$PeriodoVentas$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 381 |
| `veterinaria.vargasvet.service.reportes` | 88 |
| `veterinaria.vargasvet.dto.response` | 10 |
| `java.util.stream.Collectors` | 7 |
| `java.util.stream.LongPipeline` | 4 |
| `sun.text.resources.ext` | 4 |
| `sun.util.resources.LocaleData` | 3 |
| `sun.text.resources.cldr` | 3 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 21 | 21 | coincide |
| Mascotas | 21 | 21 | coincide |
| Citas agendadas | 21 | 21 | coincide |
| Diagnósticos | 14 | 14 | coincide |
| Recetas | 14 | 14 | coincide |
| Pagos | 10 | 10 | coincide |
| Citas completadas | 14 | 14 | coincide |
| Citas canceladas | 4 | 4 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 21 | 0 | 0.0 % | 66 | 113 |
| Registrar mascota | 21 | 0 | 0.0 % | 48 | 72 |
| Agendar cita | 21 | 0 | 0.0 % | 60 | 103 |
| Recepción (llegada) | 14 | 0 | 0.0 % | 24 | 51 |
| Iniciar atención | 14 | 0 | 0.0 % | 38 | 67 |
| Leer la consulta | 14 | 0 | 0.0 % | 27 | 43 |
| Registrar diagnóstico | 14 | 0 | 0.0 % | 25 | 70 |
| Registrar receta | 14 | 0 | 0.0 % | 28 | 83 |
| Actualizar mascota | 14 | 0 | 0.0 % | 29 | 47 |
| Cerrar la consulta | 14 | 0 | 0.0 % | 48 | 234 |
| Caja: crear punto de cobro | 9 | 0 | 0.0 % | 15 | 42 |
| Caja: vincular equipo | 9 | 0 | 0.0 % | 20 | 48 |
| Caja: abrir la caja | 9 | 0 | 0.0 % | 22 | 65 |
| Cobrar | 10 | 0 | 0.0 % | 31 | 64 |
| Cancelar cita | 4 | 0 | 0.0 % | 27 | 48 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 33 | 0 (0 %) | 287 | 444 | 24 % |
| login | 10 | 0 (0 %) | 429 | 1 801 | 16 % |
| reporte_clinico | 1 | 0 (0 %) | 3 424 | 3 424 | 9 % |
| propietarios | 19 | 0 (0 %) | 90 | 158 | 4 % |
| apoderado | 21 | 0 (0 %) | 66 | 113 | 4 % |
| citas_del_mes | 19 | 0 (0 %) | 52 | 345 | 4 % |
| cita | 21 | 0 (0 %) | 60 | 103 | 4 % |
| lista_de_historias | 23 | 0 (0 %) | 43 | 76 | 3 % |
| mascota | 21 | 0 (0 %) | 48 | 72 | 3 % |
| historia | 23 | 0 (0 %) | 36 | 65 | 3 % |
| navegacion | 33 | 0 (0 %) | 24 | 58 | 3 % |
| avisos_de_cumplimiento | 33 | 0 (0 %) | 24 | 75 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 28 | 798 | 0 % |
| 0.2 | 9 | 44 | 437 | 0 % |
| 0.5 | 10 | 40 | 158 | 0 % |
| 0.7 | 10 | 42 | 155 | 0 % |
| 1.0 | 10 | 42 | 119 | 0 % |
| 1.2 | 10 | 49 | 390 | 0 % |
| 1.5 | 10 | 46 | 94 | 0 % |
| 1.7 | 10 | 42 | 63 | 0 % |
| 2.0 | 10 | 28 | 52 | 0 % |
| 2.2 | 10 | 49 | 371 | 0 % |
| 2.5 | 10 | 54 | 109 | 0 % |
| 2.7 | 10 | 32 | 353 | 0 % |
| 3.0 | 10 | 23 | 56 | 0 % |
| 3.2 | 7 | 20 | 120 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
