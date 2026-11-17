# Fase 3: 150 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 150 |
| Peticiones hechas | 9 871 |
| Peticiones correctas (2xx) | 9 846 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 27.5 |
| Tiempo mediano (ms) | 48 |
| Tiempo p95 (ms) | 1 207 |
| CPU del backend, media (% del límite de 1 CPU) | 64 |
| Tiempo con la CPU al tope (%) | 22 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 58 |
| Memoria pico (MB, de 2048) | 1 322 |
| Sentencias SQL | 272 778 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 3606; fallidas: 25.
- Acciones hechas: panel 482, agenda 396, historia 329, reportes 41, recepcion 486, atencion 243, cobro 138, cancelar 94; sin trabajo disponible: 384.
- CPU del backend: 64 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 22 % del tiempo.
- CPU de PostgreSQL: 58 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 322 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **150 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 3.7 s.
- Correo: el servidor de pruebas recibió 486 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 135 MB (+1.8 %).
- GC: 317 pausas, 28.8 s en total (8 % del tiempo de vida de la JVM); 31 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 976 MB (tope -Xmx1g).
- Clases cargadas: 25511 en total, 264 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 14 | 14 | 3891 |
| 0.1 | 25 | 25 | 5942 |
| 0.2 | 53 | 54 | 11354 |
| 0.3 | 80 | 81 | 16044 |
| 0.4 | 80 | 81 | 16104 |
| 0.5 | 86 | 87 | 17086 |
| 0.5 | 87 | 88 | 17210 |
| 1.0 | 123 | 125 | 23691 |
| 2.1 | 130 | 133 | 24824 |
| 2.7 | 132 | 135 | 25084 |
| 3.4 | 133 | 136 | 25326 |
| 4.5 | 134 | 137 | 25401 |
| 5.1 | 135 | 138 | 25467 |
| 6.2 | 135 | 138 | 25511 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 253 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 260 |
| `io.jsonwebtoken.impl.ParameterMap` | 1 |
| `java.util.concurrent.locks` | 1 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 486 | 486 | coincide |
| Mascotas | 486 | 486 | coincide |
| Citas agendadas | 476 | 476 | coincide |
| Diagnósticos | 228 | 228 | coincide |
| Recetas | 228 | 228 | coincide |
| Pagos | 138 | 138 | coincide |
| Citas completadas | 228 | 228 | coincide |
| Citas canceladas | 94 | 94 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 486 | 0 | 0.0 % | 92 | 1 103 |
| Registrar mascota | 486 | 0 | 0.0 % | 54 | 768 |
| Agendar cita | 476 | 10 | 2.1 % | 72 | 1 075 |
| Recepción (llegada) | 243 | 0 | 0.0 % | 37 | 1 126 |
| Iniciar atención | 228 | 15 | 6.2 % | 55 | 661 |
| Leer la consulta | 228 | 0 | 0.0 % | 32 | 281 |
| Registrar diagnóstico | 228 | 0 | 0.0 % | 32 | 244 |
| Registrar receta | 228 | 0 | 0.0 % | 34 | 365 |
| Actualizar mascota | 228 | 0 | 0.0 % | 33 | 381 |
| Cerrar la consulta | 228 | 0 | 0.0 % | 55 | 441 |
| Caja: crear punto de cobro | 105 | 0 | 0.0 % | 31 | 475 |
| Caja: vincular equipo | 105 | 0 | 0.0 % | 27 | 902 |
| Caja: abrir la caja | 105 | 0 | 0.0 % | 33 | 382 |
| Cobrar | 138 | 0 | 0.0 % | 39 | 638 |
| Cancelar cita | 94 | 0 | 0.0 % | 41 | 1 509 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 632 | 0 (0 %) | 565 | 3 754 | 26 % |
| reporte_clinico | 41 | 0 (0 %) | 5 701 | 10 982 | 10 % |
| login | 150 | 0 (0 %) | 756 | 3 587 | 7 % |
| apoderado | 486 | 0 (0 %) | 92 | 1 103 | 5 % |
| propietarios | 396 | 0 (0 %) | 165 | 1 085 | 5 % |
| citas_del_mes | 396 | 0 (0 %) | 68 | 1 286 | 4 % |
| cita | 486 | 10 (2 %) | 72 | 1 075 | 4 % |
| navegacion | 632 | 0 (0 %) | 24 | 705 | 4 % |
| mascota | 486 | 0 (0 %) | 54 | 768 | 3 % |
| lista_de_historias | 329 | 0 (0 %) | 109 | 1 144 | 3 % |
| privacidad | 632 | 0 (0 %) | 32 | 581 | 3 % |
| empleados | 396 | 0 (0 %) | 47 | 988 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 77 | 978 | 0 % |
| 0.2 | 25 | 164 | 573 | 0 % |
| 0.5 | 39 | 190 | 672 | 0 % |
| 0.7 | 53 | 226 | 631 | 0 % |
| 0.9 | 67 | 305 | 487 | 0 % |
| 1.2 | 81 | 392 | 509 | 0 % |
| 1.4 | 95 | 372 | 590 | 0 % |
| 1.7 | 109 | 452 | 639 | 0 % |
| 1.9 | 123 | 507 | 1 094 | 0 % |
| 2.2 | 137 | 549 | 1 789 | 0 % |
| 2.5 | 150 | 565 | 1 086 | 0 % |
| 2.7 | 150 | 465 | 3 722 | 2 % |
| 3.0 | 150 | 570 | 1 328 | 0 % |
| 3.2 | 150 | 582 | 415 | 0 % |
| 3.5 | 150 | 565 | 147 | 0 % |
| 3.7 | 150 | 559 | 783 | 1 % |
| 4.0 | 150 | 482 | 3 285 | 1 % |
| 4.2 | 150 | 568 | 610 | 0 % |
| 4.5 | 150 | 574 | 151 | 0 % |
| 4.7 | 150 | 611 | 210 | 0 % |
| 5.0 | 150 | 588 | 288 | 0 % |
| 5.2 | 145 | 399 | 154 | 0 % |
| 5.5 | 104 | 109 | 131 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
