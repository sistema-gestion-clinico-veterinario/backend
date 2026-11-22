# Fase 3: 100 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

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
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 330 |
| Peticiones correctas (2xx) | 6 326 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.6 |
| Tiempo mediano (ms) | 23 |
| Tiempo p95 (ms) | 509 |
| CPU del backend, media (% del límite de 2 CPU) | 26 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 48 |
| Memoria pico (MB, de 2048) | 955 |
| Sentencias SQL | 171 883 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2335; fallidas: 4.
- Acciones hechas: panel 327, agenda 232, historia 183, reportes 28, recepcion 302, atencion 157, cobro 86, cancelar 55; sin trabajo disponible: 231.
- CPU del backend: 26 % de media y 77 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 48 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 955 MB (47 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.6 s.
- Correo: el servidor de pruebas recibió 302 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.5 %).
- GC: 294 pausas, 19.8 s en total (6 % del tiempo de vida de la JVM); 28 completas, 2 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 485 MB (tope -Xmx1g).
- Clases cargadas: 25463 en total, 164 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 17 | 17 | 4602 |
| 0.0 | 26 | 26 | 6140 |
| 0.1 | 39 | 39 | 8783 |
| 0.1 | 66 | 67 | 13762 |
| 0.1 | 81 | 82 | 16163 |
| 0.2 | 81 | 82 | 16194 |
| 0.2 | 86 | 87 | 17116 |
| 0.3 | 102 | 104 | 20013 |
| 1.3 | 129 | 131 | 24536 |
| 2.1 | 132 | 134 | 25013 |
| 2.7 | 133 | 136 | 25285 |
| 3.4 | 134 | 137 | 25339 |
| 4.0 | 134 | 137 | 25387 |
| 4.6 | 135 | 138 | 25458 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 159 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 163 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 302 | 302 | coincide |
| Mascotas | 302 | 302 | coincide |
| Citas agendadas | 299 | 299 | coincide |
| Diagnósticos | 156 | 156 | coincide |
| Recetas | 156 | 156 | coincide |
| Pagos | 86 | 86 | coincide |
| Citas completadas | 156 | 156 | coincide |
| Citas canceladas | 55 | 55 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 302 | 0 | 0.0 % | 49 | 426 |
| Registrar mascota | 302 | 0 | 0.0 % | 29 | 197 |
| Agendar cita | 299 | 3 | 1.0 % | 39 | 175 |
| Recepción (llegada) | 157 | 0 | 0.0 % | 27 | 372 |
| Iniciar atención | 156 | 1 | 0.6 % | 34 | 240 |
| Leer la consulta | 156 | 0 | 0.0 % | 21 | 136 |
| Registrar diagnóstico | 156 | 0 | 0.0 % | 20 | 87 |
| Registrar receta | 156 | 0 | 0.0 % | 24 | 224 |
| Actualizar mascota | 156 | 0 | 0.0 % | 20 | 66 |
| Cerrar la consulta | 156 | 0 | 0.0 % | 36 | 176 |
| Caja: crear punto de cobro | 66 | 0 | 0.0 % | 17 | 247 |
| Caja: vincular equipo | 66 | 0 | 0.0 % | 14 | 65 |
| Caja: abrir la caja | 66 | 0 | 0.0 % | 19 | 107 |
| Cobrar | 86 | 0 | 0.0 % | 26 | 133 |
| Cancelar cita | 55 | 0 | 0.0 % | 27 | 269 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 427 | 0 (0 %) | 462 | 2 405 | 45 % |
| reporte_clinico | 28 | 0 (0 %) | 2 525 | 5 390 | 10 % |
| login | 100 | 0 (0 %) | 292 | 553 | 4 % |
| apoderado | 302 | 0 (0 %) | 49 | 426 | 4 % |
| propietarios | 232 | 0 (0 %) | 83 | 332 | 4 % |
| citas_del_mes | 232 | 0 (0 %) | 48 | 465 | 3 % |
| lista_de_historias | 183 | 0 (0 %) | 65 | 432 | 3 % |
| cita | 302 | 3 (1 %) | 39 | 175 | 2 % |
| navegacion | 427 | 0 (0 %) | 12 | 217 | 2 % |
| mascota | 302 | 0 (0 %) | 29 | 197 | 2 % |
| checkin | 157 | 0 (0 %) | 27 | 372 | 2 % |
| empleados | 232 | 0 (0 %) | 25 | 261 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 81 | 311 | 0 % |
| 0.2 | 25 | 142 | 336 | 0 % |
| 0.5 | 39 | 199 | 394 | 0 % |
| 0.7 | 52 | 243 | 318 | 0 % |
| 1.0 | 66 | 283 | 301 | 0 % |
| 1.2 | 79 | 355 | 307 | 0 % |
| 1.5 | 93 | 410 | 391 | 0 % |
| 1.7 | 100 | 431 | 365 | 0 % |
| 2.0 | 100 | 400 | 364 | 0 % |
| 2.2 | 100 | 411 | 362 | 0 % |
| 2.5 | 100 | 460 | 1 606 | 0 % |
| 2.7 | 100 | 398 | 312 | 0 % |
| 3.0 | 100 | 381 | 982 | 1 % |
| 3.2 | 100 | 377 | 644 | 0 % |
| 3.5 | 100 | 433 | 1 070 | 0 % |
| 3.7 | 100 | 379 | 399 | 0 % |
| 4.0 | 100 | 381 | 667 | 0 % |
| 4.2 | 100 | 365 | 881 | 0 % |
| 4.5 | 89 | 184 | 370 | 0 % |
| 4.7 | 45 | 17 | 13 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
