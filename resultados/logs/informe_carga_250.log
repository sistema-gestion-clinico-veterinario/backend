# Fase 3: 250 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
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
| Usuarios sin degradación | 201 |
| Peticiones hechas | 15 399 |
| Peticiones correctas (2xx) | 15 246 |
| Errores (%) | 1.0 % |
| Ritmo medio (peticiones/s) | 34.4 |
| Tiempo mediano (ms) | 791 |
| Tiempo p95 (ms) | 3 541 |
| CPU del backend, media (% del límite de 2 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 71 |
| Memoria pico (MB, de 4096) | 1 405 |
| Sentencias SQL | 425 777 |
| Reinicios del contenedor | 0 |

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 5045; fallidas: 153.
- Acciones hechas: panel 787, agenda 609, historia 557, reportes 77, recepcion 834, atencion 390, cobro 155, cancelar 167; sin trabajo disponible: 592.
- CPU del backend: 32 % de media y 79 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 71 % de media y 106 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 405 MB (34 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **201 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.0 s.
- Correo: el servidor de pruebas recibió 834 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.2 %).
- GC: 407 pausas, 37.7 s en total (8 % del tiempo de vida de la JVM); 42 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 774 MB (tope -Xmx2g).
- Clases cargadas: 25550 en total, 138 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.1 | 31 | 32 | 7234 |
| 0.1 | 66 | 67 | 13771 |
| 0.2 | 81 | 82 | 16168 |
| 0.2 | 83 | 83 | 16482 |
| 0.3 | 87 | 88 | 17227 |
| 1.2 | 127 | 130 | 24272 |
| 2.2 | 132 | 135 | 25084 |
| 2.7 | 133 | 136 | 25310 |
| 3.6 | 134 | 137 | 25397 |
| 4.1 | 135 | 138 | 25413 |
| 4.7 | 135 | 138 | 25423 |
| 5.4 | 136 | 139 | 25488 |
| 6.2 | 136 | 139 | 25491 |
| 7.1 | 136 | 139 | 25549 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 129 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 134 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 834 | 834 | coincide |
| Mascotas | 834 | 834 | coincide |
| Citas agendadas | 823 | 823 | coincide |
| Diagnósticos | 248 | 248 | coincide |
| Recetas | 248 | 248 | coincide |
| Pagos | 155 | 155 | coincide |
| Citas completadas | 248 | 248 | coincide |
| Citas canceladas | 167 | 167 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 834 | 0 | 0.0 % | 817 | 3 434 |
| Registrar mascota | 834 | 0 | 0.0 % | 709 | 3 490 |
| Agendar cita | 823 | 11 | 1.3 % | 725 | 3 177 |
| Recepción (llegada) | 390 | 0 | 0.0 % | 1 015 | 3 480 |
| Iniciar atención | 248 | 142 | 36.4 % | 1 130 | 4 044 |
| Leer la consulta | 248 | 0 | 0.0 % | 452 | 2 828 |
| Registrar diagnóstico | 248 | 0 | 0.0 % | 498 | 3 362 |
| Registrar receta | 248 | 0 | 0.0 % | 562 | 2 952 |
| Actualizar mascota | 248 | 0 | 0.0 % | 556 | 2 898 |
| Cerrar la consulta | 248 | 0 | 0.0 % | 528 | 2 626 |
| Caja: crear punto de cobro | 118 | 0 | 0.0 % | 687 | 2 750 |
| Caja: vincular equipo | 118 | 0 | 0.0 % | 768 | 3 306 |
| Caja: abrir la caja | 118 | 0 | 0.0 % | 775 | 3 054 |
| Cobrar | 155 | 0 | 0.0 % | 814 | 3 385 |
| Cancelar cita | 167 | 0 | 0.0 % | 985 | 3 604 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| privacidad | 1037 | 0 (0 %) | 1 656 | 3 687 | 9 % |
| panel | 1037 | 0 (0 %) | 1 405 | 3 948 | 9 % |
| navegacion | 1037 | 0 (0 %) | 470 | 3 216 | 5 % |
| apoderado | 834 | 0 (0 %) | 817 | 3 434 | 5 % |
| notificaciones | 1037 | 0 (0 %) | 424 | 3 232 | 5 % |
| mascota | 834 | 0 (0 %) | 709 | 3 490 | 5 % |
| propietarios | 609 | 0 (0 %) | 1 273 | 3 811 | 5 % |
| cita | 834 | 11 (1 %) | 725 | 3 177 | 5 % |
| avisos_de_cumplimiento | 1037 | 0 (0 %) | 470 | 2 820 | 5 % |
| cumplimiento | 1037 | 0 (0 %) | 360 | 2 986 | 5 % |
| citas_del_mes | 609 | 0 (0 %) | 979 | 4 116 | 5 % |
| empleados | 609 | 0 (0 %) | 995 | 3 738 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 49 | 466 | 0 % |
| 0.4 | 36 | 184 | 266 | 0 % |
| 0.9 | 66 | 320 | 254 | 0 % |
| 1.4 | 96 | 400 | 231 | 0 % |
| 1.9 | 126 | 558 | 333 | 0 % |
| 2.4 | 156 | 713 | 339 | 0 % |
| 2.9 | 186 | 765 | 1 359 | 1 % |
| 3.4 | 216 | 468 | 5 626 | 1 % |
| 3.9 | 246 | 556 | 3 884 | 1 % |
| 4.4 | 250 | 626 | 4 612 | 2 % |
| 4.9 | 250 | 598 | 4 600 | 1 % |
| 5.4 | 250 | 632 | 3 069 | 2 % |
| 5.9 | 250 | 690 | 2 483 | 1 % |
| 6.4 | 250 | 665 | 2 243 | 2 % |
| 6.9 | 227 | 297 | 735 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
