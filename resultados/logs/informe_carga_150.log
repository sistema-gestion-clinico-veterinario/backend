# Fase 3: 150 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 150 |
| Peticiones hechas | 10 119 |
| Peticiones correctas (2xx) | 10 104 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 28.3 |
| Tiempo mediano (ms) | 33 |
| Tiempo p95 (ms) | 633 |
| CPU del backend, media (% del límite de 1 CPU) | 52 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 54 |
| Memoria pico (MB, de 2048) | 1 115 |
| Sentencias SQL | 278 230 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 3618; fallidas: 15.
- Acciones hechas: panel 497, agenda 397, historia 369, reportes 52, recepcion 506, atencion 229, cobro 147, cancelar 100; sin trabajo disponible: 352.
- CPU del backend: 52 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 54 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 115 MB (54 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **150 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.5 s.
- Correo: el servidor de pruebas recibió 506 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.6 %).
- GC: 370 pausas, 28.6 s en total (8 % del tiempo de vida de la JVM); 37 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 622 MB (tope -Xmx1g).
- Clases cargadas: 25513 en total, 258 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 15 | 15 | 4053 |
| 0.1 | 31 | 32 | 7221 |
| 0.2 | 66 | 66 | 13662 |
| 0.2 | 80 | 81 | 16076 |
| 0.3 | 82 | 83 | 16396 |
| 0.4 | 87 | 88 | 17211 |
| 1.2 | 126 | 128 | 24034 |
| 2.0 | 131 | 134 | 24877 |
| 2.7 | 132 | 135 | 25149 |
| 3.2 | 134 | 136 | 25346 |
| 3.6 | 134 | 137 | 25379 |
| 4.3 | 134 | 138 | 25409 |
| 4.9 | 135 | 138 | 25472 |
| 5.9 | 135 | 138 | 25513 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 249 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 256 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 506 | 506 | coincide |
| Mascotas | 506 | 506 | coincide |
| Citas agendadas | 498 | 498 | coincide |
| Diagnósticos | 222 | 222 | coincide |
| Recetas | 222 | 222 | coincide |
| Pagos | 147 | 147 | coincide |
| Citas completadas | 222 | 222 | coincide |
| Citas canceladas | 100 | 100 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 506 | 0 | 0.0 % | 58 | 399 |
| Registrar mascota | 506 | 0 | 0.0 % | 34 | 235 |
| Agendar cita | 498 | 8 | 1.6 % | 47 | 257 |
| Recepción (llegada) | 229 | 0 | 0.0 % | 30 | 510 |
| Iniciar atención | 222 | 7 | 3.1 % | 43 | 369 |
| Leer la consulta | 222 | 0 | 0.0 % | 26 | 207 |
| Registrar diagnóstico | 222 | 0 | 0.0 % | 24 | 253 |
| Registrar receta | 222 | 0 | 0.0 % | 26 | 220 |
| Actualizar mascota | 222 | 0 | 0.0 % | 24 | 170 |
| Cerrar la consulta | 222 | 0 | 0.0 % | 41 | 201 |
| Caja: crear punto de cobro | 100 | 0 | 0.0 % | 22 | 253 |
| Caja: vincular equipo | 100 | 0 | 0.0 % | 21 | 277 |
| Caja: abrir la caja | 100 | 0 | 0.0 % | 25 | 309 |
| Cobrar | 147 | 0 | 0.0 % | 33 | 441 |
| Cancelar cita | 100 | 0 | 0.0 % | 35 | 417 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 647 | 0 (0 %) | 582 | 2 900 | 42 % |
| reporte_clinico | 52 | 0 (0 %) | 2 685 | 5 425 | 10 % |
| login | 150 | 0 (0 %) | 368 | 1 334 | 5 % |
| propietarios | 397 | 0 (0 %) | 113 | 496 | 4 % |
| apoderado | 506 | 0 (0 %) | 58 | 399 | 4 % |
| citas_del_mes | 397 | 0 (0 %) | 67 | 551 | 3 % |
| lista_de_historias | 369 | 0 (0 %) | 71 | 451 | 3 % |
| cita | 506 | 8 (2 %) | 47 | 257 | 2 % |
| navegacion | 647 | 0 (0 %) | 18 | 311 | 2 % |
| mascota | 506 | 0 (0 %) | 34 | 235 | 2 % |
| empleados | 397 | 0 (0 %) | 35 | 246 | 2 % |
| privacidad | 647 | 0 (0 %) | 17 | 188 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 100 | 396 | 0 % |
| 0.3 | 28 | 148 | 368 | 0 % |
| 0.5 | 43 | 204 | 396 | 0 % |
| 0.8 | 57 | 256 | 473 | 0 % |
| 1.0 | 71 | 325 | 346 | 1 % |
| 1.3 | 85 | 380 | 333 | 0 % |
| 1.5 | 99 | 433 | 306 | 0 % |
| 1.8 | 113 | 511 | 622 | 0 % |
| 2.0 | 127 | 578 | 558 | 0 % |
| 2.3 | 141 | 536 | 336 | 0 % |
| 2.5 | 150 | 617 | 1 129 | 0 % |
| 2.8 | 150 | 531 | 235 | 0 % |
| 3.0 | 150 | 585 | 210 | 0 % |
| 3.3 | 150 | 632 | 450 | 0 % |
| 3.5 | 150 | 569 | 378 | 0 % |
| 3.8 | 150 | 557 | 2 480 | 1 % |
| 4.0 | 150 | 551 | 1 013 | 0 % |
| 4.3 | 150 | 574 | 402 | 0 % |
| 4.5 | 150 | 626 | 1 282 | 0 % |
| 4.8 | 150 | 575 | 1 296 | 0 % |
| 5.0 | 150 | 457 | 1 309 | 0 % |
| 5.3 | 141 | 327 | 1 783 | 0 % |
| 5.5 | 84 | 47 | 544 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
