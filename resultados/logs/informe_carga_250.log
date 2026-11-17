# Fase 2b: carga con escrituras, 250 usuarios simultáneos (Metaspace 256 MB, correo de pruebas)

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | DEGRADADO |
| Usuarios sin degradación | 87 | 232 |
| Peticiones hechas | 7 386 | 16 585 |
| Peticiones correctas (2xx) | 5 989 | 16 451 |
| Errores (%) | 18.9 % | 0.8 % |
| Ritmo medio (peticiones/s) | 15.1 | 37.0 |
| Tiempo mediano (ms) | 1 277 | 333 |
| Tiempo p95 (ms) | 30 001 | 2 982 |
| CPU del backend, media (%) | 54 | 78 |
| Tiempo con la CPU al tope (%) | 39 | 41 |
| CPU de PostgreSQL, media (%) | 38 | 64 |
| Memoria pico (MB) | 1 208 | 1 349 |
| Sentencias SQL | 465 088 | 460 759 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 1684; fallidas: 322.
- Acciones hechas: panel 331, agenda 294, historia 241, reportes 35, recepcion 470, atencion 145, cobro 50, cancelar 54; sin trabajo disponible: 328.
- Se agotó el pool de conexiones a la base de datos: 1382 avisos en el registro.
- CPU del backend: 54 % de media y 103 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 39 % del tiempo.
- CPU de PostgreSQL: 38 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 208 MB (59 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.0 con unos 250 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **87 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 400 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.0 %).
- GC: 286 pausas, 28.9 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 674 MB (tope -Xmx1g).
- Clases cargadas: 25665 en total, 90 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3809 |
| 0.1 | 22 | 23 | 5551 |
| 0.2 | 43 | 43 | 9635 |
| 0.3 | 66 | 67 | 13747 |
| 0.3 | 80 | 81 | 16093 |
| 0.4 | 81 | 82 | 16308 |
| 0.6 | 92 | 93 | 17903 |
| 1.4 | 127 | 130 | 24529 |
| 2.1 | 131 | 133 | 24936 |
| 2.9 | 133 | 136 | 25294 |
| 3.7 | 134 | 137 | 25510 |
| 4.3 | 135 | 138 | 25575 |
| 6.3 | 135 | 138 | 25647 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 37 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 41 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.hibernate.validator.internal` | 3 |
| `org.springframework.dao.support` | 2 |
| `org.springframework.security.core` | 2 |
| `org.apache.tomcat.util` | 2 |
| `java.util.stream.ReduceOps` | 2 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 331 | 400 | la base tiene más (ver nota) |
| Mascotas | 302 | 311 | la base tiene más (ver nota) |
| Citas agendadas | 286 | 288 | la base tiene más (ver nota) |
| Diagnósticos | 77 | 78 | la base tiene más (ver nota) |
| Recetas | 75 | 76 | la base tiene más (ver nota) |
| Pagos | 37 | 37 | coincide |
| Citas completadas | 73 | 75 | la base tiene más (ver nota) |
| Citas canceladas | 44 | 49 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 331 | 139 | 29.6 % | 2 448 | 30 001 |
| Registrar mascota | 302 | 29 | 8.8 % | 664 | 30 001 |
| Agendar cita | 286 | 16 | 5.3 % | 684 | 7 780 |
| Recepción (llegada) | 117 | 28 | 19.3 % | 1 671 | 30 001 |
| Iniciar atención | 80 | 65 | 44.8 % | 1 396 | 30 001 |
| Leer la consulta | 79 | 1 | 1.2 % | 557 | 8 558 |
| Registrar diagnóstico | 77 | 3 | 3.8 % | 528 | 9 072 |
| Registrar receta | 75 | 5 | 6.2 % | 759 | 30 001 |
| Actualizar mascota | 74 | 6 | 7.5 % | 507 | 30 001 |
| Cerrar la consulta | 73 | 7 | 8.8 % | 779 | 30 000 |
| Caja: crear punto de cobro | 38 | 9 | 19.1 % | 1 124 | 30 001 |
| Caja: vincular equipo | 36 | 2 | 5.3 % | 748 | 30 000 |
| Caja: abrir la caja | 35 | 1 | 2.8 % | 1 036 | 3 396 |
| Cobrar | 37 | 1 | 2.6 % | 805 | 17 975 |
| Cancelar cita | 44 | 10 | 18.5 % | 2 540 | 30 001 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| apoderado | 470 | 139 (30 %) | 2 448 | 30 001 | 10 % |
| panel | 557 | 103 (18 %) | 2 037 | 30 001 | 8 % |
| navegacion | 594 | 100 (17 %) | 916 | 30 001 | 8 % |
| privacidad | 594 | 106 (18 %) | 666 | 30 001 | 8 % |
| cumplimiento | 587 | 106 (18 %) | 854 | 30 001 | 7 % |
| notificaciones | 580 | 101 (17 %) | 558 | 30 001 | 7 % |
| login | 323 | 97 (30 %) | 4 228 | 30 001 | 7 % |
| avisos_de_cumplimiento | 569 | 96 (17 %) | 643 | 30 001 | 7 % |
| empleados | 302 | 83 (27 %) | 1 375 | 30 001 | 6 % |
| citas_del_mes | 302 | 78 (26 %) | 2 037 | 30 001 | 6 % |
| propietarios | 294 | 79 (27 %) | 1 688 | 30 001 | 5 % |
| lista_de_historias | 241 | 73 (30 %) | 2 297 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 91 | 1 248 | 0 % |
| 0.5 | 42 | 195 | 1 802 | 0 % |
| 1.0 | 72 | 325 | 1 275 | 0 % |
| 1.5 | 102 | 300 | 4 667 | 1 % |
| 2.0 | 132 | 502 | 1 965 | 0 % |
| 2.5 | 162 | 491 | 2 978 | 1 % |
| 3.0 | 192 | 473 | 5 258 | 1 % |
| 3.5 | 222 | 391 | 8 239 | 1 % |
| 4.0 | 250 | 11 | 30 001 | 100 % |
| 4.5 | 250 | 107 | 30 001 | 44 % |
| 5.0 | 250 | 131 | 30 001 | 83 % |
| 5.5 | 250 | 116 | 30 001 | 92 % |
| 6.0 | 250 | 112 | 30 001 | 97 % |
| 6.5 | 250 | 113 | 30 001 | 100 % |
| 7.0 | 226 | 112 | 30 001 | 100 % |
| 7.5 | 140 | 72 | 30 001 | 100 % |
| 8.0 | 41 | 25 | 9 266 | 0 % |

</details>

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 5539; fallidas: 134.
- Acciones hechas: panel 856, agenda 699, historia 566, reportes 77, recepcion 855, atencion 413, cobro 201, cancelar 168; sin trabajo disponible: 661.
- CPU del backend: 78 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 41 % del tiempo.
- CPU de PostgreSQL: 64 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 349 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **232 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 11.0 s.
- Correo: el servidor de pruebas recibió 855 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.5 %).
- GC: 433 pausas, 51.9 s en total (11 % del tiempo de vida de la JVM); 44 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 985 MB (tope -Xmx1g).
- Clases cargadas: 25560 en total, 212 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 18 | 18 | 4556 |
| 0.2 | 43 | 43 | 9662 |
| 0.3 | 78 | 79 | 15955 |
| 0.4 | 80 | 81 | 16113 |
| 0.6 | 87 | 88 | 17208 |
| 1.5 | 126 | 129 | 24096 |
| 2.4 | 131 | 134 | 24949 |
| 3.1 | 133 | 136 | 25271 |
| 4.1 | 134 | 137 | 25381 |
| 4.8 | 135 | 138 | 25447 |
| 5.6 | 136 | 139 | 25471 |
| 6.4 | 136 | 139 | 25513 |
| 7.0 | 136 | 139 | 25542 |
| 7.7 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 201 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 208 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 855 | 855 | coincide |
| Mascotas | 855 | 855 | coincide |
| Citas agendadas | 842 | 842 | coincide |
| Diagnósticos | 292 | 292 | coincide |
| Recetas | 292 | 292 | coincide |
| Pagos | 201 | 201 | coincide |
| Citas completadas | 292 | 292 | coincide |
| Citas canceladas | 168 | 168 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 855 | 0 | 0.0 % | 380 | 3 100 |
| Registrar mascota | 855 | 0 | 0.0 % | 302 | 3 097 |
| Agendar cita | 842 | 13 | 1.5 % | 299 | 2 779 |
| Recepción (llegada) | 413 | 0 | 0.0 % | 388 | 2 763 |
| Iniciar atención | 292 | 121 | 29.3 % | 413 | 3 225 |
| Leer la consulta | 292 | 0 | 0.0 % | 167 | 2 000 |
| Registrar diagnóstico | 292 | 0 | 0.0 % | 144 | 2 338 |
| Registrar receta | 292 | 0 | 0.0 % | 180 | 2 208 |
| Actualizar mascota | 292 | 0 | 0.0 % | 155 | 2 078 |
| Cerrar la consulta | 292 | 0 | 0.0 % | 215 | 2 223 |
| Caja: crear punto de cobro | 151 | 0 | 0.0 % | 336 | 2 806 |
| Caja: vincular equipo | 151 | 0 | 0.0 % | 391 | 2 816 |
| Caja: abrir la caja | 151 | 0 | 0.0 % | 520 | 2 648 |
| Cobrar | 201 | 0 | 0.0 % | 546 | 3 703 |
| Cancelar cita | 168 | 0 | 0.0 % | 402 | 2 764 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1106 | 0 (0 %) | 1 167 | 3 618 | 12 % |
| navegacion | 1106 | 0 (0 %) | 170 | 2 788 | 5 % |
| apoderado | 855 | 0 (0 %) | 380 | 3 100 | 5 % |
| propietarios | 699 | 0 (0 %) | 528 | 3 390 | 5 % |
| mascota | 855 | 0 (0 %) | 302 | 3 097 | 5 % |
| privacidad | 1106 | 0 (0 %) | 223 | 2 235 | 5 % |
| cita | 855 | 13 (2 %) | 299 | 2 779 | 5 % |
| citas_del_mes | 699 | 0 (0 %) | 471 | 3 162 | 5 % |
| cumplimiento | 1106 | 0 (0 %) | 143 | 2 434 | 5 % |
| reporte_clinico | 77 | 0 (0 %) | 7 231 | 15 866 | 4 % |
| notificaciones | 1106 | 0 (0 %) | 114 | 2 085 | 4 % |
| avisos_de_cumplimiento | 1106 | 0 (0 %) | 122 | 2 191 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 49 | 1 495 | 0 % |
| 0.4 | 37 | 177 | 399 | 0 % |
| 0.9 | 67 | 302 | 616 | 0 % |
| 1.4 | 97 | 411 | 428 | 0 % |
| 1.9 | 127 | 535 | 380 | 0 % |
| 2.4 | 157 | 664 | 681 | 0 % |
| 2.9 | 187 | 788 | 1 116 | 1 % |
| 3.4 | 217 | 745 | 1 084 | 1 % |
| 3.9 | 247 | 745 | 3 055 | 1 % |
| 4.4 | 250 | 639 | 3 439 | 0 % |
| 4.9 | 250 | 865 | 897 | 1 % |
| 5.4 | 250 | 667 | 3 322 | 2 % |
| 5.9 | 250 | 686 | 3 105 | 2 % |
| 6.4 | 250 | 625 | 8 076 | 2 % |
| 6.9 | 214 | 320 | 206 | 1 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor, registros de la JVM y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
