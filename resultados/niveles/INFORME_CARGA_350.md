# Fase 3: 350 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 2g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 4g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 102 |
| Peticiones hechas | 14 097 |
| Peticiones correctas (2xx) | 13 832 |
| Errores (%) | 1.9 % |
| Ritmo medio (peticiones/s) | 25.4 |
| Tiempo mediano (ms) | 5 131 |
| Tiempo p95 (ms) | 11 553 |
| CPU del backend, media (% del límite de 2 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 89 |
| Memoria pico (MB, de 4096) | 1 368 |
| Sentencias SQL | 391 233 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4053; fallidas: 265.
- Acciones hechas: panel 697, agenda 533, historia 463, reportes 53, recepcion 826, atencion 382, cobro 89, cancelar 120; sin trabajo disponible: 627.
- CPU del backend: 30 % de media y 83 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 89 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 368 MB (33 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **102 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 14.8 s.
- Correo: el servidor de pruebas recibió 826 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.1 %).
- GC: 357 pausas, 41.7 s en total (8 % del tiempo de vida de la JVM); 36 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 766 MB (tope -Xmx2g).
- Clases cargadas: 25460 en total, 112 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 29 | 29 | 6846 |
| 0.1 | 58 | 58 | 12441 |
| 0.1 | 81 | 81 | 16156 |
| 0.2 | 81 | 82 | 16203 |
| 0.3 | 87 | 88 | 17223 |
| 1.1 | 127 | 130 | 24290 |
| 2.1 | 132 | 135 | 25023 |
| 2.9 | 133 | 136 | 25338 |
| 4.0 | 134 | 137 | 25346 |
| 5.2 | 135 | 138 | 25433 |
| 6.2 | 135 | 138 | 25435 |
| 7.3 | 136 | 139 | 25442 |
| 8.0 | 136 | 139 | 25458 |
| 8.9 | 136 | 139 | 25460 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 109 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 110 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 826 | 826 | coincide |
| Mascotas | 826 | 826 | coincide |
| Citas agendadas | 817 | 817 | coincide |
| Diagnósticos | 126 | 126 | coincide |
| Recetas | 126 | 126 | coincide |
| Pagos | 89 | 89 | coincide |
| Citas completadas | 126 | 126 | coincide |
| Citas canceladas | 120 | 120 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 826 | 0 | 0.0 % | 5 566 | 11 682 |
| Registrar mascota | 826 | 0 | 0.0 % | 5 813 | 11 573 |
| Agendar cita | 817 | 9 | 1.1 % | 5 427 | 12 015 |
| Recepción (llegada) | 382 | 0 | 0.0 % | 6 153 | 12 026 |
| Iniciar atención | 126 | 256 | 67.0 % | 7 602 | 13 392 |
| Leer la consulta | 126 | 0 | 0.0 % | 4 020 | 10 090 |
| Registrar diagnóstico | 126 | 0 | 0.0 % | 3 408 | 11 576 |
| Registrar receta | 126 | 0 | 0.0 % | 3 239 | 11 213 |
| Actualizar mascota | 126 | 0 | 0.0 % | 2 477 | 9 958 |
| Cerrar la consulta | 126 | 0 | 0.0 % | 3 800 | 11 309 |
| Caja: crear punto de cobro | 79 | 0 | 0.0 % | 4 667 | 9 806 |
| Caja: vincular equipo | 79 | 0 | 0.0 % | 4 121 | 9 443 |
| Caja: abrir la caja | 79 | 0 | 0.0 % | 4 322 | 9 454 |
| Cobrar | 89 | 0 | 0.0 % | 4 376 | 10 576 |
| Cancelar cita | 120 | 0 | 0.0 % | 5 971 | 11 721 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1047 | 0 (0 %) | 5 826 | 12 203 | 8 % |
| privacidad | 1047 | 0 (0 %) | 4 948 | 9 816 | 7 % |
| mascota | 826 | 0 (0 %) | 5 813 | 11 573 | 6 % |
| apoderado | 826 | 0 (0 %) | 5 566 | 11 682 | 6 % |
| cita | 826 | 9 (1 %) | 5 427 | 12 015 | 6 % |
| notificaciones | 1047 | 0 (0 %) | 4 225 | 9 869 | 6 % |
| navegacion | 1047 | 0 (0 %) | 3 948 | 10 032 | 6 % |
| avisos_de_cumplimiento | 1047 | 0 (0 %) | 3 981 | 10 294 | 6 % |
| cumplimiento | 1047 | 0 (0 %) | 3 808 | 10 420 | 6 % |
| propietarios | 533 | 0 (0 %) | 6 686 | 12 577 | 5 % |
| citas_del_mes | 533 | 0 (0 %) | 6 377 | 12 685 | 5 % |
| empleados | 533 | 0 (0 %) | 5 816 | 12 511 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 84 | 379 | 0 % |
| 0.5 | 42 | 213 | 304 | 0 % |
| 1.0 | 72 | 334 | 364 | 0 % |
| 1.5 | 102 | 413 | 819 | 0 % |
| 2.0 | 132 | 435 | 2 912 | 1 % |
| 2.5 | 162 | 348 | 5 080 | 0 % |
| 3.0 | 192 | 379 | 6 848 | 1 % |
| 3.5 | 222 | 352 | 10 411 | 2 % |
| 4.0 | 252 | 417 | 8 499 | 0 % |
| 4.5 | 282 | 461 | 9 928 | 3 % |
| 5.0 | 312 | 395 | 12 295 | 3 % |
| 5.5 | 342 | 397 | 13 668 | 2 % |
| 6.0 | 350 | 433 | 14 772 | 3 % |
| 6.5 | 350 | 453 | 11 446 | 3 % |
| 7.0 | 350 | 548 | 12 115 | 3 % |
| 7.5 | 350 | 457 | 11 596 | 5 % |
| 8.0 | 350 | 533 | 10 957 | 4 % |
| 8.5 | 335 | 503 | 13 006 | 4 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
