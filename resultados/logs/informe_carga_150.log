# Fase 3: 150 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | DEGRADADO |
| Usuarios sin degradación | 101 |
| Peticiones hechas | 8 695 |
| Peticiones correctas (2xx) | 8 659 |
| Errores (%) | 0.4 % |
| Ritmo medio (peticiones/s) | 24.5 |
| Tiempo mediano (ms) | 254 |
| Tiempo p95 (ms) | 3 674 |
| CPU del backend, media (% del límite de 2 CPU) | 30 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 66 |
| Memoria pico (MB, de 4096) | 1 086 |
| Sentencias SQL | 240 347 |
| Reinicios del contenedor | 0 |

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 2974; fallidas: 36.
- Acciones hechas: panel 446, agenda 342, historia 273, reportes 38, recepcion 444, atencion 205, cobro 94, cancelar 83; sin trabajo disponible: 306.
- CPU del backend: 30 % de media y 74 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 66 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 086 MB (27 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **101 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.2 s.
- Correo: el servidor de pruebas recibió 444 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.4 %).
- GC: 299 pausas, 28.5 s en total (8 % del tiempo de vida de la JVM); 33 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 584 MB (tope -Xmx2g).
- Clases cargadas: 25479 en total, 225 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 25 | 25 | 5950 |
| 0.1 | 46 | 47 | 10178 |
| 0.1 | 72 | 72 | 14691 |
| 0.2 | 81 | 82 | 16169 |
| 0.2 | 81 | 82 | 16256 |
| 0.3 | 87 | 88 | 17206 |
| 0.5 | 113 | 115 | 21981 |
| 1.2 | 128 | 130 | 24493 |
| 1.9 | 131 | 134 | 24864 |
| 2.5 | 132 | 135 | 25083 |
| 3.6 | 134 | 137 | 25321 |
| 4.3 | 134 | 137 | 25399 |
| 4.8 | 135 | 138 | 25410 |
| 5.5 | 135 | 138 | 25462 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 216 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 223 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 444 | 444 | coincide |
| Mascotas | 444 | 444 | coincide |
| Citas agendadas | 435 | 435 | coincide |
| Diagnósticos | 178 | 178 | coincide |
| Recetas | 178 | 178 | coincide |
| Pagos | 94 | 94 | coincide |
| Citas completadas | 178 | 178 | coincide |
| Citas canceladas | 83 | 83 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 444 | 0 | 0.0 % | 350 | 3 103 |
| Registrar mascota | 444 | 0 | 0.0 % | 240 | 2 386 |
| Agendar cita | 435 | 9 | 2.0 % | 201 | 2 614 |
| Recepción (llegada) | 205 | 0 | 0.0 % | 265 | 2 832 |
| Iniciar atención | 178 | 27 | 13.2 % | 172 | 2 432 |
| Leer la consulta | 178 | 0 | 0.0 % | 44 | 1 841 |
| Registrar diagnóstico | 178 | 0 | 0.0 % | 49 | 2 552 |
| Registrar receta | 178 | 0 | 0.0 % | 40 | 2 340 |
| Actualizar mascota | 178 | 0 | 0.0 % | 39 | 2 307 |
| Cerrar la consulta | 178 | 0 | 0.0 % | 68 | 2 917 |
| Caja: crear punto de cobro | 67 | 0 | 0.0 % | 375 | 2 725 |
| Caja: vincular equipo | 67 | 0 | 0.0 % | 257 | 2 642 |
| Caja: abrir la caja | 67 | 0 | 0.0 % | 330 | 2 049 |
| Cobrar | 94 | 0 | 0.0 % | 70 | 2 689 |
| Cancelar cita | 83 | 0 | 0.0 % | 383 | 2 997 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 596 | 0 (0 %) | 3 092 | 6 630 | 24 % |
| navegacion | 596 | 0 (0 %) | 143 | 2 560 | 6 % |
| apoderado | 444 | 0 (0 %) | 350 | 3 103 | 5 % |
| citas_del_mes | 342 | 0 (0 %) | 397 | 3 251 | 5 % |
| avisos_de_cumplimiento | 596 | 0 (0 %) | 86 | 2 335 | 4 % |
| notificaciones | 596 | 0 (0 %) | 90 | 2 128 | 4 % |
| lista_de_historias | 273 | 0 (0 %) | 639 | 3 380 | 4 % |
| cumplimiento | 596 | 0 (0 %) | 71 | 1 968 | 4 % |
| privacidad | 596 | 0 (0 %) | 117 | 2 066 | 4 % |
| mascota | 444 | 0 (0 %) | 240 | 2 386 | 4 % |
| cita | 444 | 9 (2 %) | 201 | 2 614 | 4 % |
| propietarios | 342 | 0 (0 %) | 460 | 2 407 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 2 | 20 | 697 | 0 % |
| 0.1 | 16 | 113 | 392 | 0 % |
| 0.3 | 30 | 147 | 291 | 0 % |
| 0.6 | 44 | 225 | 536 | 0 % |
| 0.8 | 58 | 258 | 482 | 1 % |
| 1.1 | 72 | 359 | 378 | 0 % |
| 1.3 | 87 | 385 | 1 036 | 0 % |
| 1.6 | 101 | 413 | 1 475 | 0 % |
| 1.8 | 115 | 410 | 3 861 | 0 % |
| 2.0 | 129 | 416 | 3 800 | 0 % |
| 2.3 | 143 | 360 | 5 360 | 1 % |
| 2.5 | 150 | 361 | 6 230 | 0 % |
| 2.8 | 150 | 436 | 5 305 | 1 % |
| 3.0 | 150 | 371 | 4 518 | 1 % |
| 3.3 | 150 | 507 | 4 079 | 1 % |
| 3.5 | 150 | 426 | 4 503 | 0 % |
| 3.8 | 150 | 439 | 4 215 | 1 % |
| 4.0 | 150 | 456 | 4 774 | 2 % |
| 4.3 | 150 | 577 | 1 505 | 0 % |
| 4.5 | 150 | 614 | 149 | 0 % |
| 4.8 | 150 | 579 | 405 | 0 % |
| 5.0 | 150 | 527 | 114 | 0 % |
| 5.3 | 133 | 275 | 104 | 0 % |
| 5.5 | 70 | 21 | 108 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
