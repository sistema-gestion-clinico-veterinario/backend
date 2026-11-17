# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 222 |
| Peticiones correctas (2xx) | 6 212 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.4 |
| Tiempo mediano (ms) | 34 |
| Tiempo p95 (ms) | 642 |
| CPU del backend, media (% del límite de 1 CPU) | 54 |
| Tiempo con la CPU al tope (%) | 20 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 59 |
| Memoria pico (MB, de 2048) | 1 069 |
| Sentencias SQL | 171 476 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2179; fallidas: 10.
- Acciones hechas: panel 319, agenda 246, historia 201, reportes 26, recepcion 306, atencion 140, cobro 76, cancelar 58; sin trabajo disponible: 201.
- CPU del backend: 54 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 20 % del tiempo.
- CPU de PostgreSQL: 59 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 069 MB (52 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 1.2 s.
- Correo: el servidor de pruebas recibió 306 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+2.0 %).
- GC: 296 pausas, 22.5 s en total (6 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 580 MB (tope -Xmx1g).
- Clases cargadas: 25410 en total, 349 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 23 | 24 | 5664 |
| 0.2 | 45 | 46 | 10187 |
| 0.3 | 69 | 70 | 14192 |
| 0.3 | 80 | 81 | 16075 |
| 0.4 | 80 | 81 | 16114 |
| 0.5 | 86 | 87 | 17104 |
| 0.6 | 91 | 92 | 17711 |
| 1.4 | 126 | 129 | 24165 |
| 2.1 | 130 | 133 | 24742 |
| 2.8 | 132 | 135 | 25034 |
| 3.3 | 133 | 136 | 25250 |
| 4.1 | 134 | 136 | 25304 |
| 5.0 | 134 | 137 | 25356 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 338 |
| `java.lang.invoke.LambdaForm$DMH/` | 11 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 349 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 306 | 306 | coincide |
| Mascotas | 306 | 306 | coincide |
| Citas agendadas | 300 | 300 | coincide |
| Diagnósticos | 136 | 136 | coincide |
| Recetas | 136 | 136 | coincide |
| Pagos | 76 | 76 | coincide |
| Citas completadas | 136 | 136 | coincide |
| Citas canceladas | 58 | 58 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 306 | 0 | 0.0 % | 65 | 262 |
| Registrar mascota | 306 | 0 | 0.0 % | 40 | 92 |
| Agendar cita | 300 | 6 | 2.0 % | 53 | 145 |
| Recepción (llegada) | 140 | 0 | 0.0 % | 31 | 261 |
| Iniciar atención | 136 | 4 | 2.9 % | 43 | 180 |
| Leer la consulta | 136 | 0 | 0.0 % | 28 | 57 |
| Registrar diagnóstico | 136 | 0 | 0.0 % | 27 | 69 |
| Registrar receta | 136 | 0 | 0.0 % | 30 | 62 |
| Actualizar mascota | 136 | 0 | 0.0 % | 26 | 66 |
| Cerrar la consulta | 136 | 0 | 0.0 % | 46 | 114 |
| Caja: crear punto de cobro | 59 | 0 | 0.0 % | 25 | 215 |
| Caja: vincular equipo | 59 | 0 | 0.0 % | 22 | 87 |
| Caja: abrir la caja | 59 | 0 | 0.0 % | 27 | 109 |
| Cobrar | 76 | 0 | 0.0 % | 32 | 228 |
| Cancelar cita | 58 | 0 | 0.0 % | 43 | 564 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 419 | 0 (0 %) | 644 | 2 399 | 44 % |
| reporte_clinico | 26 | 0 (0 %) | 3 864 | 6 524 | 12 % |
| login | 100 | 0 (0 %) | 607 | 1 984 | 9 % |
| propietarios | 246 | 0 (0 %) | 123 | 289 | 4 % |
| apoderado | 306 | 0 (0 %) | 65 | 262 | 4 % |
| lista_de_historias | 201 | 0 (0 %) | 80 | 380 | 3 % |
| cita | 306 | 6 (2 %) | 53 | 145 | 2 % |
| citas_del_mes | 246 | 0 (0 %) | 49 | 257 | 2 % |
| navegacion | 419 | 0 (0 %) | 20 | 106 | 2 % |
| mascota | 306 | 0 (0 %) | 40 | 92 | 2 % |
| historia | 201 | 0 (0 %) | 42 | 157 | 1 % |
| cumplimiento | 419 | 0 (0 %) | 24 | 56 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 66 | 1 151 | 0 % |
| 0.2 | 23 | 130 | 570 | 1 % |
| 0.4 | 37 | 186 | 539 | 1 % |
| 0.7 | 50 | 244 | 491 | 0 % |
| 0.9 | 64 | 296 | 696 | 0 % |
| 1.2 | 78 | 360 | 920 | 0 % |
| 1.4 | 91 | 397 | 464 | 0 % |
| 1.7 | 100 | 355 | 461 | 0 % |
| 1.9 | 100 | 445 | 990 | 0 % |
| 2.2 | 100 | 437 | 561 | 0 % |
| 2.4 | 100 | 363 | 757 | 0 % |
| 2.7 | 100 | 440 | 470 | 0 % |
| 2.9 | 100 | 400 | 1 143 | 0 % |
| 3.2 | 100 | 360 | 415 | 0 % |
| 3.4 | 100 | 409 | 1 073 | 0 % |
| 3.7 | 100 | 374 | 377 | 0 % |
| 3.9 | 100 | 386 | 493 | 0 % |
| 4.2 | 100 | 366 | 1 114 | 0 % |
| 4.4 | 98 | 190 | 508 | 0 % |
| 4.7 | 59 | 18 | 422 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
