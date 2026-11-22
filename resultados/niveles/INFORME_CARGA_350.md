# Fase 3: 350 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 154 |
| Peticiones hechas | 16 961 |
| Peticiones correctas (2xx) | 16 668 |
| Errores (%) | 1.7 % |
| Ritmo medio (peticiones/s) | 30.8 |
| Tiempo mediano (ms) | 2 808 |
| Tiempo p95 (ms) | 9 933 |
| CPU del backend, media (% del límite de 1 CPU) | 57 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 83 |
| Memoria pico (MB, de 2048) | 1 318 |
| Sentencias SQL | 471 547 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5005; fallidas: 293.
- Acciones hechas: panel 864, agenda 687, historia 575, reportes 81, recepcion 951, atencion 462, cobro 135, cancelar 157; sin trabajo disponible: 696.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 83 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 318 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **154 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 15.2 s.
- Correo: el servidor de pruebas recibió 951 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.2 %).
- GC: 429 pausas, 43.7 s en total (8 % del tiempo de vida de la JVM); 41 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 904 MB (tope -Xmx1g).
- Clases cargadas: 25539 en total, 129 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 18 | 18 | 4556 |
| 0.1 | 43 | 43 | 9649 |
| 0.2 | 80 | 81 | 16040 |
| 0.3 | 80 | 81 | 16088 |
| 0.4 | 87 | 88 | 17208 |
| 1.4 | 127 | 130 | 24242 |
| 2.2 | 132 | 134 | 25003 |
| 3.0 | 133 | 136 | 25301 |
| 4.3 | 135 | 138 | 25403 |
| 5.4 | 135 | 138 | 25421 |
| 6.5 | 136 | 139 | 25479 |
| 7.3 | 136 | 139 | 25480 |
| 8.3 | 136 | 139 | 25497 |
| 9.3 | 136 | 139 | 25539 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 122 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 127 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 951 | 951 | coincide |
| Mascotas | 951 | 951 | coincide |
| Citas agendadas | 936 | 936 | coincide |
| Diagnósticos | 184 | 184 | coincide |
| Recetas | 184 | 184 | coincide |
| Pagos | 135 | 135 | coincide |
| Citas completadas | 184 | 184 | coincide |
| Citas canceladas | 157 | 157 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 951 | 0 | 0.0 % | 3 160 | 10 574 |
| Registrar mascota | 951 | 0 | 0.0 % | 2 852 | 9 881 |
| Agendar cita | 936 | 15 | 1.6 % | 3 066 | 10 369 |
| Recepción (llegada) | 462 | 0 | 0.0 % | 3 624 | 10 083 |
| Iniciar atención | 184 | 278 | 60.2 % | 3 610 | 12 034 |
| Leer la consulta | 184 | 0 | 0.0 % | 936 | 8 739 |
| Registrar diagnóstico | 184 | 0 | 0.0 % | 1 043 | 9 767 |
| Registrar receta | 184 | 0 | 0.0 % | 1 288 | 8 175 |
| Actualizar mascota | 184 | 0 | 0.0 % | 1 198 | 10 851 |
| Cerrar la consulta | 184 | 0 | 0.0 % | 1 374 | 8 094 |
| Caja: crear punto de cobro | 103 | 0 | 0.0 % | 1 839 | 8 213 |
| Caja: vincular equipo | 103 | 0 | 0.0 % | 1 911 | 7 466 |
| Caja: abrir la caja | 103 | 0 | 0.0 % | 1 953 | 7 060 |
| Cobrar | 135 | 0 | 0.0 % | 2 659 | 10 639 |
| Cancelar cita | 157 | 0 | 0.0 % | 3 033 | 11 496 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1214 | 0 (0 %) | 3 698 | 10 060 | 8 % |
| privacidad | 1214 | 0 (0 %) | 2 947 | 8 279 | 7 % |
| navegacion | 1214 | 0 (0 %) | 1 950 | 9 682 | 6 % |
| apoderado | 951 | 0 (0 %) | 3 160 | 10 574 | 6 % |
| cumplimiento | 1214 | 0 (0 %) | 2 171 | 9 407 | 6 % |
| notificaciones | 1214 | 0 (0 %) | 2 076 | 8 649 | 6 % |
| cita | 951 | 15 (2 %) | 3 066 | 10 369 | 6 % |
| avisos_de_cumplimiento | 1214 | 0 (0 %) | 1 905 | 9 200 | 6 % |
| mascota | 951 | 0 (0 %) | 2 852 | 9 881 | 6 % |
| propietarios | 687 | 0 (0 %) | 3 990 | 10 512 | 5 % |
| citas_del_mes | 687 | 0 (0 %) | 3 617 | 10 899 | 5 % |
| empleados | 687 | 0 (0 %) | 3 433 | 10 596 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 30 | 908 | 0 % |
| 0.3 | 34 | 162 | 419 | 0 % |
| 0.8 | 64 | 319 | 375 | 0 % |
| 1.3 | 94 | 393 | 547 | 1 % |
| 1.8 | 124 | 538 | 1 341 | 0 % |
| 2.3 | 154 | 526 | 1 811 | 0 % |
| 2.8 | 184 | 485 | 3 706 | 0 % |
| 3.3 | 214 | 480 | 6 277 | 0 % |
| 3.8 | 244 | 539 | 7 038 | 1 % |
| 4.3 | 274 | 464 | 7 520 | 1 % |
| 4.8 | 304 | 526 | 11 206 | 2 % |
| 5.3 | 334 | 519 | 15 162 | 4 % |
| 5.8 | 350 | 529 | 14 004 | 1 % |
| 6.3 | 350 | 579 | 12 691 | 3 % |
| 6.8 | 350 | 564 | 9 701 | 2 % |
| 7.3 | 350 | 553 | 8 661 | 5 % |
| 7.8 | 350 | 565 | 8 789 | 3 % |
| 8.3 | 350 | 561 | 7 063 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
