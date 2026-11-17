# Fase 2b: carga con escrituras, 20 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 20 | 20 |
| Peticiones hechas | 1 038 | 1 069 |
| Peticiones correctas (2xx) | 1 038 | 1 069 |
| Errores (%) | 0.0 % | 0.0 % |
| Ritmo medio (peticiones/s) | 4.5 | 4.7 |
| Tiempo mediano (ms) | 30 | 27 |
| Tiempo p95 (ms) | 302 | 352 |
| CPU del backend, media (%) | 24 | 18 |
| Tiempo con la CPU al tope (%) | 2 | 2 |
| CPU de PostgreSQL, media (%) | 10 | 10 |
| Memoria pico (MB) | 753 | 832 |
| Sentencias SQL | 57 400 | 29 931 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 433; fallidas: 0.
- Acciones hechas: panel 42, agenda 36, historia 32, reportes 3, recepcion 55, atencion 29, cobro 18, cancelar 8; sin trabajo disponible: 47.
- CPU del backend: 24 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 51 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 753 MB (37 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **20 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.9 s.
- Correo: el servidor de pruebas recibió 55 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 132 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 129 a 132 MB (+1.8 %).
- GC: 184 pausas, 7.8 s en total (3 % del tiempo de vida de la JVM); 16 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 315 MB (tope -Xmx1g).
- Clases cargadas: 25087 en total, 288 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 12 | 12 | 3616 |
| 0.1 | 15 | 15 | 4027 |
| 0.1 | 21 | 22 | 5292 |
| 0.1 | 30 | 30 | 6934 |
| 0.2 | 49 | 50 | 10934 |
| 0.3 | 65 | 66 | 13610 |
| 0.3 | 77 | 78 | 15734 |
| 0.4 | 80 | 81 | 16094 |
| 0.4 | 80 | 81 | 16110 |
| 0.5 | 83 | 83 | 16442 |
| 0.6 | 87 | 88 | 17214 |
| 0.7 | 96 | 97 | 18343 |
| 1.4 | 125 | 127 | 24068 |
| 2.2 | 129 | 132 | 24836 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 255 |
| `java.lang.invoke.LambdaForm$DMH/` | 10 |
| `org.hibernate.query.sqm.sql.BaseSqmToSqlAstConverter$$Lambda/` | 4 |
| `org.hibernate.query.sqm.internal.SimpleUpdateQueryPlan$$Lambda/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 265 |
| `org.hibernate.query.sqm` | 12 |
| `org.hibernate.action.internal` | 2 |
| `org.hibernate.engine.spi` | 2 |
| `org.hibernate.sql.exec` | 2 |
| `org.hibernate.sql.ast` | 1 |
| `org.springframework.data.repository` | 1 |
| `org.springframework.jdbc.core` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 55 | 55 | coincide |
| Mascotas | 55 | 55 | coincide |
| Citas agendadas | 55 | 55 | coincide |
| Diagnósticos | 29 | 29 | coincide |
| Recetas | 29 | 29 | coincide |
| Pagos | 18 | 18 | coincide |
| Citas completadas | 29 | 29 | coincide |
| Citas canceladas | 8 | 8 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 55 | 0 | 0.0 % | 52 | 249 |
| Registrar mascota | 55 | 0 | 0.0 % | 36 | 100 |
| Agendar cita | 55 | 0 | 0.0 % | 46 | 162 |
| Recepción (llegada) | 29 | 0 | 0.0 % | 19 | 111 |
| Iniciar atención | 29 | 0 | 0.0 % | 25 | 83 |
| Leer la consulta | 29 | 0 | 0.0 % | 16 | 54 |
| Registrar diagnóstico | 29 | 0 | 0.0 % | 20 | 91 |
| Registrar receta | 29 | 0 | 0.0 % | 21 | 96 |
| Actualizar mascota | 29 | 0 | 0.0 % | 23 | 71 |
| Cerrar la consulta | 29 | 0 | 0.0 % | 39 | 111 |
| Caja: crear punto de cobro | 13 | 0 | 0.0 % | 18 | 37 |
| Caja: vincular equipo | 13 | 0 | 0.0 % | 15 | 50 |
| Caja: abrir la caja | 13 | 0 | 0.0 % | 21 | 59 |
| Cobrar | 18 | 0 | 0.0 % | 28 | 65 |
| Cancelar cita | 8 | 0 | 0.0 % | 25 | 458 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 62 | 0 (0 %) | 259 | 506 | 22 % |
| login | 20 | 0 (0 %) | 681 | 1 305 | 16 % |
| reporte_clinico | 3 | 0 (0 %) | 2 970 | 5 769 | 14 % |
| citas_del_mes | 36 | 0 (0 %) | 108 | 450 | 6 % |
| apoderado | 55 | 0 (0 %) | 52 | 249 | 5 % |
| cita | 55 | 0 (0 %) | 46 | 162 | 4 % |
| propietarios | 36 | 0 (0 %) | 66 | 160 | 4 % |
| mascota | 55 | 0 (0 %) | 36 | 100 | 3 % |
| navegacion | 62 | 0 (0 %) | 23 | 62 | 2 % |
| avisos_de_cumplimiento | 62 | 0 (0 %) | 20 | 58 | 2 % |
| cumplimiento | 62 | 0 (0 %) | 21 | 60 | 2 % |
| privacidad | 62 | 0 (0 %) | 20 | 54 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 80 | 897 | 0 % |
| 0.3 | 20 | 96 | 686 | 0 % |
| 0.5 | 20 | 87 | 370 | 0 % |
| 0.8 | 20 | 74 | 134 | 0 % |
| 1.0 | 20 | 84 | 293 | 0 % |
| 1.3 | 20 | 109 | 185 | 0 % |
| 1.5 | 20 | 79 | 402 | 0 % |
| 1.8 | 20 | 73 | 65 | 0 % |
| 2.0 | 20 | 77 | 257 | 0 % |
| 2.3 | 20 | 76 | 128 | 0 % |
| 2.5 | 20 | 70 | 84 | 0 % |
| 2.8 | 20 | 85 | 126 | 0 % |
| 3.0 | 20 | 38 | 73 | 0 % |
| 3.3 | 13 | 10 | 94 | 0 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 329; fallidas: 0.
- Acciones hechas: panel 59, agenda 50, historia 28, reportes 4, recepcion 47, atencion 20, cobro 9, cancelar 12; sin trabajo disponible: 35.
- CPU del backend: 18 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 2 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 46 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 832 MB (41 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **20 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 47 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 131 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 128 a 131 MB (+2.0 %).
- GC: 189 pausas, 8.0 s en total (3 % del tiempo de vida de la JVM); 16 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 354 MB (tope -Xmx1g).
- Clases cargadas: 24879 en total, 323 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 13 | 3711 |
| 0.1 | 17 | 18 | 4408 |
| 0.1 | 24 | 25 | 5759 |
| 0.2 | 36 | 36 | 8095 |
| 0.2 | 56 | 56 | 11870 |
| 0.3 | 71 | 72 | 14597 |
| 0.3 | 80 | 81 | 16072 |
| 0.4 | 80 | 81 | 16104 |
| 0.4 | 81 | 82 | 16264 |
| 0.5 | 86 | 87 | 17104 |
| 0.6 | 87 | 88 | 17213 |
| 0.9 | 115 | 117 | 22412 |
| 1.8 | 127 | 129 | 24348 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 284 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |
| `veterinaria.vargasvet.service.PuntoCobroService$$Lambda/` | 7 |
| `veterinaria.vargasvet.service.impl.CajaServiceImpl$$Lambda/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 295 |
| `com.fasterxml.jackson.databind` | 11 |
| `veterinaria.vargasvet.service.PuntoCobroService` | 7 |
| `veterinaria.vargasvet.service.impl` | 7 |
| `java.util.concurrent.locks` | 1 |
| `org.springframework.data.repository` | 1 |
| `org.postgresql.jdbc.PgConnection` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 47 | 47 | coincide |
| Mascotas | 47 | 47 | coincide |
| Citas agendadas | 47 | 47 | coincide |
| Diagnósticos | 20 | 20 | coincide |
| Recetas | 20 | 20 | coincide |
| Pagos | 9 | 9 | coincide |
| Citas completadas | 20 | 20 | coincide |
| Citas canceladas | 12 | 12 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 47 | 0 | 0.0 % | 50 | 269 |
| Registrar mascota | 47 | 0 | 0.0 % | 35 | 140 |
| Agendar cita | 47 | 0 | 0.0 % | 43 | 244 |
| Recepción (llegada) | 20 | 0 | 0.0 % | 16 | 35 |
| Iniciar atención | 20 | 0 | 0.0 % | 25 | 55 |
| Leer la consulta | 20 | 0 | 0.0 % | 16 | 27 |
| Registrar diagnóstico | 20 | 0 | 0.0 % | 14 | 43 |
| Registrar receta | 20 | 0 | 0.0 % | 17 | 53 |
| Actualizar mascota | 20 | 0 | 0.0 % | 21 | 40 |
| Cerrar la consulta | 20 | 0 | 0.0 % | 40 | 149 |
| Caja: crear punto de cobro | 9 | 0 | 0.0 % | 16 | 41 |
| Caja: vincular equipo | 9 | 0 | 0.0 % | 17 | 38 |
| Caja: abrir la caja | 9 | 0 | 0.0 % | 21 | 61 |
| Cobrar | 9 | 0 | 0.0 % | 30 | 75 |
| Cancelar cita | 12 | 0 | 0.0 % | 20 | 42 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 79 | 0 (0 %) | 189 | 486 | 24 % |
| login | 20 | 0 (0 %) | 472 | 2 006 | 17 % |
| reporte_clinico | 4 | 0 (0 %) | 3 216 | 5 339 | 17 % |
| propietarios | 50 | 0 (0 %) | 60 | 183 | 5 % |
| apoderado | 47 | 0 (0 %) | 50 | 269 | 4 % |
| cita | 47 | 0 (0 %) | 43 | 244 | 4 % |
| citas_del_mes | 50 | 0 (0 %) | 37 | 229 | 4 % |
| mascota | 47 | 0 (0 %) | 35 | 140 | 3 % |
| empleados | 50 | 0 (0 %) | 23 | 91 | 2 % |
| navegacion | 79 | 0 (0 %) | 16 | 44 | 2 % |
| cumplimiento | 79 | 0 (0 %) | 16 | 50 | 2 % |
| avisos_de_cumplimiento | 79 | 0 (0 %) | 15 | 43 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 80 | 642 | 0 % |
| 0.3 | 20 | 102 | 531 | 0 % |
| 0.5 | 20 | 54 | 111 | 0 % |
| 0.8 | 20 | 82 | 228 | 0 % |
| 1.0 | 20 | 80 | 187 | 0 % |
| 1.3 | 20 | 72 | 106 | 0 % |
| 1.5 | 20 | 77 | 82 | 0 % |
| 1.8 | 20 | 96 | 73 | 0 % |
| 2.0 | 20 | 83 | 266 | 0 % |
| 2.3 | 20 | 96 | 262 | 0 % |
| 2.5 | 20 | 74 | 412 | 0 % |
| 2.8 | 20 | 93 | 124 | 0 % |
| 3.0 | 20 | 59 | 355 | 0 % |
| 3.3 | 14 | 21 | 367 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
