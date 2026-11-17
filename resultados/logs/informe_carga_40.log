# Fase 2b: carga con escrituras, 40 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 40 | 40 |
| Peticiones hechas | 2 221 | 2 246 |
| Peticiones correctas (2xx) | 2 221 | 2 246 |
| Errores (%) | 0.0 % | 0.0 % |
| Ritmo medio (peticiones/s) | 9.2 | 9.1 |
| Tiempo mediano (ms) | 24 | 25 |
| Tiempo p95 (ms) | 298 | 401 |
| CPU del backend, media (%) | 33 | 32 |
| Tiempo con la CPU al tope (%) | 5 | 7 |
| CPU de PostgreSQL, media (%) | 17 | 19 |
| Memoria pico (MB) | 740 | 837 |
| Sentencias SQL | 135 153 | 62 108 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 810; fallidas: 0.
- Acciones hechas: panel 106, agenda 93, historia 64, reportes 8, recepcion 108, atencion 52, cobro 32, cancelar 15; sin trabajo disponible: 85.
- CPU del backend: 33 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 5 % del tiempo.
- CPU de PostgreSQL: 17 % de media y 70 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 740 MB (36 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **40 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.6 s.
- Correo: el servidor de pruebas recibió 108 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 133 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 131 a 133 MB (+1.6 %).
- GC: 234 pausas, 11.0 s en total (4 % del tiempo de vida de la JVM); 22 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 311 MB (tope -Xmx1g).
- Clases cargadas: 25371 en total, 405 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 13 | 3737 |
| 0.1 | 19 | 19 | 4649 |
| 0.1 | 26 | 27 | 6180 |
| 0.2 | 47 | 48 | 10547 |
| 0.3 | 66 | 66 | 13684 |
| 0.3 | 80 | 81 | 16085 |
| 0.4 | 80 | 81 | 16110 |
| 0.4 | 82 | 83 | 16419 |
| 0.5 | 86 | 87 | 17120 |
| 0.6 | 93 | 95 | 18057 |
| 1.3 | 126 | 128 | 24324 |
| 2.3 | 131 | 133 | 24993 |
| 2.7 | 131 | 134 | 25051 |
| 3.8 | 133 | 135 | 25281 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 387 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 398 |
| `io.micrometer.core.instrument` | 2 |
| `org.springframework.jdbc.core` | 1 |
| `com.zaxxer.hikari.pool` | 1 |
| `org.postgresql.jdbc.PgConnection` | 1 |
| `java.util.concurrent.locks` | 1 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 108 | 108 | coincide |
| Mascotas | 108 | 108 | coincide |
| Citas agendadas | 108 | 108 | coincide |
| Diagnósticos | 52 | 52 | coincide |
| Recetas | 52 | 52 | coincide |
| Pagos | 32 | 32 | coincide |
| Citas completadas | 52 | 52 | coincide |
| Citas canceladas | 15 | 15 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 108 | 0 | 0.0 % | 39 | 123 |
| Registrar mascota | 108 | 0 | 0.0 % | 27 | 80 |
| Agendar cita | 108 | 0 | 0.0 % | 41 | 122 |
| Recepción (llegada) | 52 | 0 | 0.0 % | 17 | 84 |
| Iniciar atención | 52 | 0 | 0.0 % | 21 | 65 |
| Leer la consulta | 52 | 0 | 0.0 % | 15 | 43 |
| Registrar diagnóstico | 52 | 0 | 0.0 % | 14 | 51 |
| Registrar receta | 52 | 0 | 0.0 % | 17 | 47 |
| Actualizar mascota | 52 | 0 | 0.0 % | 15 | 49 |
| Cerrar la consulta | 52 | 0 | 0.0 % | 31 | 69 |
| Caja: crear punto de cobro | 25 | 0 | 0.0 % | 15 | 35 |
| Caja: vincular equipo | 25 | 0 | 0.0 % | 12 | 36 |
| Caja: abrir la caja | 25 | 0 | 0.0 % | 16 | 44 |
| Cobrar | 32 | 0 | 0.0 % | 19 | 63 |
| Cancelar cita | 15 | 0 | 0.0 % | 30 | 285 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 146 | 0 (0 %) | 264 | 559 | 26 % |
| login | 40 | 0 (0 %) | 582 | 1 604 | 17 % |
| reporte_clinico | 8 | 0 (0 %) | 2 802 | 7 023 | 15 % |
| citas_del_mes | 93 | 0 (0 %) | 76 | 272 | 6 % |
| propietarios | 93 | 0 (0 %) | 61 | 269 | 5 % |
| cita | 108 | 0 (0 %) | 41 | 122 | 3 % |
| apoderado | 108 | 0 (0 %) | 39 | 123 | 3 % |
| mascota | 108 | 0 (0 %) | 27 | 80 | 2 % |
| empleados | 93 | 0 (0 %) | 24 | 94 | 2 % |
| historia | 64 | 0 (0 %) | 31 | 116 | 2 % |
| cumplimiento | 146 | 0 (0 %) | 15 | 40 | 2 % |
| navegacion | 146 | 0 (0 %) | 14 | 43 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 12 | 1 604 | 0 % |
| 0.1 | 15 | 99 | 725 | 0 % |
| 0.3 | 27 | 134 | 411 | 0 % |
| 0.6 | 39 | 198 | 482 | 0 % |
| 0.8 | 40 | 163 | 210 | 0 % |
| 1.1 | 40 | 169 | 234 | 0 % |
| 1.3 | 40 | 176 | 153 | 0 % |
| 1.6 | 40 | 146 | 269 | 0 % |
| 1.8 | 40 | 156 | 188 | 0 % |
| 2.1 | 40 | 172 | 315 | 0 % |
| 2.3 | 40 | 155 | 154 | 0 % |
| 2.6 | 40 | 159 | 255 | 0 % |
| 2.8 | 40 | 156 | 285 | 0 % |
| 3.1 | 40 | 153 | 250 | 0 % |
| 3.3 | 40 | 132 | 235 | 0 % |
| 3.6 | 30 | 41 | 58 | 0 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 814; fallidas: 0.
- Acciones hechas: panel 107, agenda 94, historia 69, reportes 7, recepcion 104, atencion 53, cobro 35, cancelar 15; sin trabajo disponible: 89.
- CPU del backend: 32 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 19 % de media y 66 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 837 MB (41 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **40 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.0 s.
- Correo: el servidor de pruebas recibió 104 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 132 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 130 a 132 MB (+1.7 %).
- GC: 220 pausas, 10.1 s en total (4 % del tiempo de vida de la JVM); 19 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 389 MB (tope -Xmx1g).
- Clases cargadas: 25173 en total, 361 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 13 | 3737 |
| 0.1 | 18 | 18 | 4557 |
| 0.1 | 24 | 25 | 5814 |
| 0.2 | 41 | 41 | 9207 |
| 0.2 | 63 | 63 | 13156 |
| 0.3 | 80 | 81 | 16044 |
| 0.4 | 80 | 81 | 16074 |
| 0.4 | 80 | 81 | 16103 |
| 0.5 | 84 | 85 | 16824 |
| 0.5 | 87 | 88 | 17201 |
| 0.6 | 91 | 92 | 17728 |
| 1.4 | 125 | 128 | 24065 |
| 2.4 | 130 | 133 | 24813 |
| 3.7 | 132 | 135 | 25058 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 349 |
| `java.lang.invoke.LambdaForm$DMH/` | 9 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 358 |
| `io.micrometer.core.instrument` | 2 |
| `org.postgresql.jdbc.PgConnection` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 104 | 104 | coincide |
| Mascotas | 104 | 104 | coincide |
| Citas agendadas | 104 | 104 | coincide |
| Diagnósticos | 53 | 53 | coincide |
| Recetas | 53 | 53 | coincide |
| Pagos | 35 | 35 | coincide |
| Citas completadas | 53 | 53 | coincide |
| Citas canceladas | 15 | 15 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 104 | 0 | 0.0 % | 41 | 123 |
| Registrar mascota | 104 | 0 | 0.0 % | 31 | 70 |
| Agendar cita | 104 | 0 | 0.0 % | 41 | 90 |
| Recepción (llegada) | 53 | 0 | 0.0 % | 16 | 124 |
| Iniciar atención | 53 | 0 | 0.0 % | 25 | 65 |
| Leer la consulta | 53 | 0 | 0.0 % | 16 | 38 |
| Registrar diagnóstico | 53 | 0 | 0.0 % | 17 | 43 |
| Registrar receta | 53 | 0 | 0.0 % | 20 | 54 |
| Actualizar mascota | 53 | 0 | 0.0 % | 19 | 52 |
| Cerrar la consulta | 53 | 0 | 0.0 % | 33 | 133 |
| Caja: crear punto de cobro | 27 | 0 | 0.0 % | 12 | 43 |
| Caja: vincular equipo | 27 | 0 | 0.0 % | 12 | 38 |
| Caja: abrir la caja | 27 | 0 | 0.0 % | 16 | 51 |
| Cobrar | 35 | 0 | 0.0 % | 20 | 66 |
| Cancelar cita | 15 | 0 | 0.0 % | 17 | 40 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 147 | 0 (0 %) | 408 | 818 | 37 % |
| login | 40 | 0 (0 %) | 416 | 1 289 | 14 % |
| reporte_clinico | 7 | 0 (0 %) | 1 779 | 4 006 | 9 % |
| propietarios | 94 | 0 (0 %) | 58 | 161 | 4 % |
| apoderado | 104 | 0 (0 %) | 41 | 123 | 4 % |
| cita | 104 | 0 (0 %) | 41 | 90 | 3 % |
| citas_del_mes | 94 | 0 (0 %) | 35 | 98 | 3 % |
| mascota | 104 | 0 (0 %) | 31 | 70 | 3 % |
| lista_de_historias | 69 | 0 (0 %) | 35 | 141 | 2 % |
| navegacion | 147 | 0 (0 %) | 17 | 41 | 2 % |
| avisos_de_cumplimiento | 147 | 0 (0 %) | 17 | 37 | 2 % |
| cumplimiento | 147 | 0 (0 %) | 17 | 40 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 1 012 | 0 % |
| 0.2 | 20 | 114 | 527 | 0 % |
| 0.4 | 32 | 162 | 487 | 0 % |
| 0.7 | 40 | 175 | 461 | 0 % |
| 0.9 | 40 | 157 | 336 | 0 % |
| 1.2 | 40 | 190 | 122 | 0 % |
| 1.4 | 40 | 174 | 408 | 0 % |
| 1.7 | 40 | 143 | 53 | 0 % |
| 1.9 | 40 | 194 | 581 | 0 % |
| 2.2 | 40 | 139 | 125 | 0 % |
| 2.4 | 40 | 145 | 387 | 0 % |
| 2.7 | 40 | 196 | 89 | 0 % |
| 2.9 | 40 | 142 | 401 | 0 % |
| 3.2 | 40 | 143 | 141 | 0 % |
| 3.4 | 38 | 85 | 403 | 0 % |
| 3.7 | 23 | 31 | 397 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
