# Fase 3: 150 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | DEGRADADO |
| Usuarios sin degradación | 132 |
| Peticiones hechas | 8 741 |
| Peticiones correctas (2xx) | 8 689 |
| Errores (%) | 0.6 % |
| Ritmo medio (peticiones/s) | 24.3 |
| Tiempo mediano (ms) | 360 |
| Tiempo p95 (ms) | 3 307 |
| CPU del backend, media (% del límite de 2 CPU) | 26 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 75 |
| Memoria pico (MB, de 2048) | 1 047 |
| Sentencias SQL | 239 136 |
| Reinicios del contenedor | 0 |

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 2968; fallidas: 52.
- Acciones hechas: panel 431, agenda 328, historia 326, reportes 41, recepcion 453, atencion 209, cobro 103, cancelar 69; sin trabajo disponible: 329.
- CPU del backend: 26 % de media y 82 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 75 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 047 MB (51 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **132 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.7 s.
- Correo: el servidor de pruebas recibió 453 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.3 %).
- GC: 342 pausas, 30.5 s en total (9 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 561 MB (tope -Xmx1g).
- Clases cargadas: 25494 en total, 179 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 18 | 18 | 4752 |
| 0.0 | 29 | 30 | 6947 |
| 0.1 | 54 | 55 | 11843 |
| 0.1 | 80 | 81 | 16136 |
| 0.2 | 81 | 82 | 16185 |
| 0.2 | 87 | 88 | 17215 |
| 0.6 | 119 | 121 | 22931 |
| 1.7 | 130 | 133 | 24784 |
| 2.5 | 133 | 135 | 25173 |
| 3.2 | 134 | 137 | 25368 |
| 3.7 | 134 | 137 | 25401 |
| 4.3 | 135 | 138 | 25424 |
| 5.0 | 135 | 138 | 25493 |
| 5.6 | 135 | 138 | 25493 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 173 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 177 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 453 | 453 | coincide |
| Mascotas | 453 | 453 | coincide |
| Citas agendadas | 445 | 445 | coincide |
| Diagnósticos | 165 | 165 | coincide |
| Recetas | 165 | 165 | coincide |
| Pagos | 103 | 103 | coincide |
| Citas completadas | 165 | 165 | coincide |
| Citas canceladas | 69 | 69 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 453 | 0 | 0.0 % | 532 | 2 784 |
| Registrar mascota | 453 | 0 | 0.0 % | 369 | 2 364 |
| Agendar cita | 445 | 8 | 1.8 % | 344 | 2 036 |
| Recepción (llegada) | 209 | 0 | 0.0 % | 553 | 2 659 |
| Iniciar atención | 165 | 44 | 21.1 % | 475 | 2 676 |
| Leer la consulta | 165 | 0 | 0.0 % | 219 | 2 373 |
| Registrar diagnóstico | 165 | 0 | 0.0 % | 278 | 1 815 |
| Registrar receta | 165 | 0 | 0.0 % | 311 | 1 630 |
| Actualizar mascota | 165 | 0 | 0.0 % | 266 | 2 412 |
| Cerrar la consulta | 165 | 0 | 0.0 % | 392 | 2 552 |
| Caja: crear punto de cobro | 82 | 0 | 0.0 % | 588 | 2 505 |
| Caja: vincular equipo | 82 | 0 | 0.0 % | 408 | 2 202 |
| Caja: abrir la caja | 82 | 0 | 0.0 % | 439 | 1 704 |
| Cobrar | 103 | 0 | 0.0 % | 421 | 2 034 |
| Cancelar cita | 69 | 0 | 0.0 % | 964 | 2 502 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 581 | 0 (0 %) | 3 803 | 5 902 | 25 % |
| apoderado | 453 | 0 (0 %) | 532 | 2 784 | 6 % |
| lista_de_historias | 326 | 0 (0 %) | 759 | 2 925 | 5 % |
| citas_del_mes | 328 | 0 (0 %) | 752 | 2 950 | 5 % |
| navegacion | 581 | 0 (0 %) | 176 | 2 361 | 5 % |
| mascota | 453 | 0 (0 %) | 369 | 2 364 | 4 % |
| propietarios | 328 | 0 (0 %) | 536 | 2 872 | 4 % |
| cita | 453 | 8 (2 %) | 344 | 2 036 | 4 % |
| reporte_clinico | 41 | 0 (0 %) | 6 222 | 8 080 | 4 % |
| avisos_de_cumplimiento | 581 | 0 (0 %) | 96 | 1 666 | 3 % |
| cumplimiento | 581 | 0 (0 %) | 135 | 1 645 | 3 % |
| historia | 326 | 0 (0 %) | 430 | 2 458 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 338 | 0 % |
| 0.1 | 20 | 123 | 331 | 0 % |
| 0.4 | 34 | 176 | 334 | 0 % |
| 0.6 | 48 | 224 | 382 | 0 % |
| 0.9 | 62 | 313 | 342 | 1 % |
| 1.1 | 76 | 363 | 360 | 0 % |
| 1.4 | 90 | 391 | 432 | 0 % |
| 1.6 | 104 | 459 | 429 | 0 % |
| 1.9 | 118 | 523 | 312 | 0 % |
| 2.1 | 132 | 520 | 897 | 0 % |
| 2.4 | 147 | 488 | 3 495 | 1 % |
| 2.6 | 150 | 467 | 4 134 | 1 % |
| 2.9 | 150 | 447 | 3 816 | 1 % |
| 3.1 | 150 | 457 | 4 461 | 1 % |
| 3.4 | 150 | 406 | 4 734 | 2 % |
| 3.6 | 150 | 436 | 4 221 | 1 % |
| 3.9 | 150 | 563 | 2 022 | 1 % |
| 4.1 | 150 | 431 | 4 090 | 0 % |
| 4.4 | 150 | 475 | 3 935 | 1 % |
| 4.6 | 150 | 371 | 4 523 | 0 % |
| 4.9 | 150 | 424 | 4 566 | 2 % |
| 5.1 | 150 | 408 | 4 345 | 0 % |
| 5.4 | 127 | 228 | 3 479 | 0 % |
| 5.6 | 58 | 6 | 432 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
