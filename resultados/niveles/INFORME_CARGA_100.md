# Fase 2b: carga con escrituras, 100 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Usuarios sin degradación | 100 | 100 |
| Peticiones hechas | 6 173 | 6 187 |
| Peticiones correctas (2xx) | 6 162 | 6 178 |
| Errores (%) | 0.2 % | 0.1 % |
| Ritmo medio (peticiones/s) | 20.1 | 20.1 |
| Tiempo mediano (ms) | 32 | 24 |
| Tiempo p95 (ms) | 503 | 245 |
| CPU del backend, media (%) | 61 | 53 |
| Tiempo con la CPU al tope (%) | 25 | 18 |
| CPU de PostgreSQL, media (%) | 46 | 31 |
| Memoria pico (MB) | 1 075 | 891 |
| Sentencias SQL | 389 799 | 170 288 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2240; fallidas: 11.
- Acciones hechas: panel 301, agenda 243, historia 203, reportes 26, recepcion 321, atencion 139, cobro 80, cancelar 56; sin trabajo disponible: 232.
- CPU del backend: 61 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 25 % del tiempo.
- CPU de PostgreSQL: 46 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 075 MB (52 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 321 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.7 %).
- GC: 299 pausas, 22.7 s en total (6 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 554 MB (tope -Xmx1g).
- Clases cargadas: 25605 en total, 291 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3809 |
| 0.1 | 22 | 23 | 5553 |
| 0.2 | 43 | 43 | 9638 |
| 0.3 | 66 | 67 | 13747 |
| 0.4 | 80 | 81 | 16087 |
| 0.4 | 82 | 83 | 16422 |
| 0.5 | 87 | 88 | 17227 |
| 1.1 | 125 | 127 | 24104 |
| 1.9 | 130 | 133 | 24834 |
| 2.5 | 132 | 135 | 25137 |
| 2.9 | 133 | 136 | 25314 |
| 3.5 | 134 | 137 | 25464 |
| 4.2 | 134 | 137 | 25545 |
| 4.9 | 135 | 138 | 25553 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 280 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 291 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 321 | 321 | coincide |
| Mascotas | 321 | 321 | coincide |
| Citas agendadas | 312 | 312 | coincide |
| Diagnósticos | 137 | 137 | coincide |
| Recetas | 137 | 137 | coincide |
| Pagos | 80 | 80 | coincide |
| Citas completadas | 137 | 137 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 321 | 0 | 0.0 % | 59 | 226 |
| Registrar mascota | 321 | 0 | 0.0 % | 34 | 100 |
| Agendar cita | 312 | 9 | 2.8 % | 54 | 163 |
| Recepción (llegada) | 139 | 0 | 0.0 % | 28 | 195 |
| Iniciar atención | 137 | 2 | 1.4 % | 37 | 117 |
| Leer la consulta | 137 | 0 | 0.0 % | 24 | 72 |
| Registrar diagnóstico | 137 | 0 | 0.0 % | 26 | 63 |
| Registrar receta | 137 | 0 | 0.0 % | 24 | 68 |
| Actualizar mascota | 137 | 0 | 0.0 % | 22 | 62 |
| Cerrar la consulta | 137 | 0 | 0.0 % | 41 | 101 |
| Caja: crear punto de cobro | 63 | 0 | 0.0 % | 20 | 262 |
| Caja: vincular equipo | 63 | 0 | 0.0 % | 21 | 45 |
| Caja: abrir la caja | 63 | 0 | 0.0 % | 28 | 52 |
| Cobrar | 80 | 0 | 0.0 % | 34 | 77 |
| Cancelar cita | 56 | 0 | 0.0 % | 36 | 272 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 401 | 0 (0 %) | 467 | 1 475 | 32 % |
| reporte_clinico | 26 | 0 (0 %) | 4 833 | 8 253 | 17 % |
| login | 100 | 0 (0 %) | 727 | 1 905 | 11 % |
| citas_del_mes | 243 | 0 (0 %) | 177 | 486 | 7 % |
| propietarios | 243 | 0 (0 %) | 155 | 351 | 5 % |
| apoderado | 321 | 0 (0 %) | 59 | 226 | 3 % |
| cita | 321 | 9 (3 %) | 54 | 163 | 3 % |
| empleados | 243 | 0 (0 %) | 49 | 149 | 2 % |
| mascota | 321 | 0 (0 %) | 34 | 100 | 2 % |
| lista_de_historias | 203 | 0 (0 %) | 48 | 166 | 2 % |
| navegacion | 401 | 0 (0 %) | 18 | 97 | 2 % |
| privacidad | 401 | 0 (0 %) | 19 | 70 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 51 | 1 140 | 0 % |
| 0.1 | 21 | 116 | 725 | 0 % |
| 0.4 | 35 | 165 | 476 | 0 % |
| 0.7 | 49 | 241 | 415 | 0 % |
| 0.9 | 62 | 261 | 723 | 0 % |
| 1.1 | 76 | 322 | 358 | 0 % |
| 1.4 | 89 | 404 | 896 | 0 % |
| 1.6 | 100 | 406 | 675 | 0 % |
| 1.9 | 100 | 408 | 405 | 1 % |
| 2.1 | 100 | 356 | 356 | 0 % |
| 2.4 | 100 | 409 | 330 | 0 % |
| 2.6 | 100 | 370 | 433 | 1 % |
| 2.9 | 100 | 436 | 396 | 0 % |
| 3.1 | 100 | 385 | 326 | 0 % |
| 3.4 | 100 | 352 | 656 | 0 % |
| 3.6 | 100 | 415 | 301 | 0 % |
| 3.9 | 100 | 411 | 456 | 0 % |
| 4.2 | 100 | 372 | 649 | 1 % |
| 4.4 | 95 | 233 | 349 | 0 % |
| 4.7 | 59 | 60 | 540 | 0 % |

</details>

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2196; fallidas: 9.
- Acciones hechas: panel 307, agenda 241, historia 215, reportes 24, recepcion 315, atencion 133, cobro 83, cancelar 67; sin trabajo disponible: 199.
- CPU del backend: 53 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 18 % del tiempo.
- CPU de PostgreSQL: 31 % de media y 85 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 891 MB (44 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.1 s.
- Correo: el servidor de pruebas recibió 315 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 135 MB (+1.7 %).
- GC: 294 pausas, 19.9 s en total (6 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 461 MB (tope -Xmx1g).
- Clases cargadas: 25410 en total, 186 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 23 | 24 | 5664 |
| 0.2 | 45 | 46 | 10172 |
| 0.3 | 66 | 67 | 13753 |
| 0.3 | 80 | 81 | 16074 |
| 0.4 | 81 | 81 | 16113 |
| 0.5 | 87 | 88 | 17199 |
| 0.6 | 99 | 100 | 18995 |
| 1.5 | 129 | 131 | 24548 |
| 2.6 | 132 | 135 | 25064 |
| 3.3 | 133 | 136 | 25249 |
| 3.8 | 134 | 137 | 25331 |
| 4.3 | 134 | 137 | 25349 |
| 5.0 | 134 | 137 | 25358 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 181 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 186 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 315 | 315 | coincide |
| Mascotas | 315 | 315 | coincide |
| Citas agendadas | 308 | 308 | coincide |
| Diagnósticos | 131 | 131 | coincide |
| Recetas | 131 | 131 | coincide |
| Pagos | 83 | 83 | coincide |
| Citas completadas | 131 | 131 | coincide |
| Citas canceladas | 67 | 67 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 315 | 0 | 0.0 % | 43 | 299 |
| Registrar mascota | 315 | 0 | 0.0 % | 27 | 96 |
| Agendar cita | 308 | 7 | 2.2 % | 38 | 149 |
| Recepción (llegada) | 133 | 0 | 0.0 % | 17 | 390 |
| Iniciar atención | 131 | 2 | 1.5 % | 26 | 84 |
| Leer la consulta | 131 | 0 | 0.0 % | 16 | 51 |
| Registrar diagnóstico | 131 | 0 | 0.0 % | 16 | 58 |
| Registrar receta | 131 | 0 | 0.0 % | 17 | 53 |
| Actualizar mascota | 131 | 0 | 0.0 % | 17 | 47 |
| Cerrar la consulta | 131 | 0 | 0.0 % | 31 | 82 |
| Caja: crear punto de cobro | 63 | 0 | 0.0 % | 14 | 93 |
| Caja: vincular equipo | 63 | 0 | 0.0 % | 12 | 41 |
| Caja: abrir la caja | 63 | 0 | 0.0 % | 15 | 45 |
| Cobrar | 83 | 0 | 0.0 % | 20 | 76 |
| Cancelar cita | 67 | 0 | 0.0 % | 27 | 181 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 407 | 0 (0 %) | 176 | 620 | 21 % |
| login | 100 | 0 (0 %) | 568 | 1 984 | 17 % |
| reporte_clinico | 24 | 0 (0 %) | 2 543 | 4 701 | 16 % |
| propietarios | 241 | 0 (0 %) | 77 | 231 | 5 % |
| apoderado | 315 | 0 (0 %) | 43 | 299 | 5 % |
| cita | 315 | 7 (2 %) | 38 | 149 | 4 % |
| citas_del_mes | 241 | 0 (0 %) | 42 | 179 | 3 % |
| lista_de_historias | 215 | 0 (0 %) | 43 | 165 | 3 % |
| navegacion | 407 | 0 (0 %) | 14 | 90 | 3 % |
| mascota | 315 | 0 (0 %) | 27 | 96 | 3 % |
| privacidad | 407 | 0 (0 %) | 18 | 59 | 2 % |
| cumplimiento | 407 | 0 (0 %) | 15 | 49 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 79 | 1 091 | 0 % |
| 0.2 | 25 | 119 | 584 | 0 % |
| 0.5 | 39 | 205 | 976 | 0 % |
| 0.7 | 52 | 241 | 392 | 0 % |
| 1.0 | 66 | 286 | 261 | 0 % |
| 1.2 | 79 | 350 | 326 | 0 % |
| 1.5 | 93 | 406 | 237 | 0 % |
| 1.7 | 100 | 398 | 153 | 0 % |
| 2.0 | 100 | 413 | 267 | 0 % |
| 2.2 | 100 | 368 | 138 | 0 % |
| 2.5 | 100 | 393 | 145 | 0 % |
| 2.7 | 100 | 397 | 604 | 1 % |
| 3.0 | 100 | 419 | 360 | 0 % |
| 3.2 | 100 | 417 | 147 | 0 % |
| 3.5 | 100 | 377 | 130 | 1 % |
| 3.7 | 100 | 377 | 230 | 0 % |
| 4.0 | 100 | 372 | 158 | 0 % |
| 4.2 | 100 | 343 | 169 | 0 % |
| 4.5 | 87 | 204 | 149 | 0 % |
| 4.7 | 47 | 23 | 181 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
