# Fase 3: 200 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 161 |
| Peticiones hechas | 13 859 |
| Peticiones correctas (2xx) | 13 810 |
| Errores (%) | 0.4 % |
| Ritmo medio (peticiones/s) | 33.9 |
| Tiempo mediano (ms) | 65 |
| Tiempo p95 (ms) | 1 306 |
| CPU del backend, media (% del límite de 1 CPU) | 72 |
| Tiempo con la CPU al tope (%) | 35 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 57 |
| Memoria pico (MB, de 2048) | 1 358 |
| Sentencias SQL | 383 186 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 4853; fallidas: 49.
- Acciones hechas: panel 704, agenda 556, historia 489, reportes 66, recepcion 699, atencion 327, cobro 177, cancelar 155; sin trabajo disponible: 512.
- CPU del backend: 72 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 35 % del tiempo.
- CPU de PostgreSQL: 57 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 358 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **161 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 5.8 s.
- Correo: el servidor de pruebas recibió 699 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 133 a 136 MB (+2.0 %).
- GC: 364 pausas, 40.7 s en total (9 % del tiempo de vida de la JVM); 36 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 989 MB (tope -Xmx1g).
- Clases cargadas: 25556 en total, 243 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 15 | 15 | 4050 |
| 0.1 | 32 | 32 | 7225 |
| 0.2 | 66 | 66 | 13660 |
| 0.3 | 80 | 81 | 16075 |
| 0.4 | 82 | 83 | 16409 |
| 0.5 | 87 | 88 | 17211 |
| 1.2 | 124 | 126 | 23807 |
| 2.2 | 131 | 133 | 24820 |
| 3.0 | 133 | 135 | 25149 |
| 3.9 | 134 | 137 | 25367 |
| 4.7 | 135 | 138 | 25400 |
| 5.6 | 135 | 138 | 25469 |
| 6.3 | 136 | 139 | 25513 |
| 7.0 | 136 | 139 | 25529 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 231 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 239 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 699 | 699 | coincide |
| Mascotas | 699 | 699 | coincide |
| Citas agendadas | 690 | 690 | coincide |
| Diagnósticos | 287 | 287 | coincide |
| Recetas | 287 | 287 | coincide |
| Pagos | 177 | 177 | coincide |
| Citas completadas | 287 | 287 | coincide |
| Citas canceladas | 155 | 155 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 699 | 0 | 0.0 % | 95 | 1 442 |
| Registrar mascota | 699 | 0 | 0.0 % | 53 | 1 052 |
| Agendar cita | 690 | 9 | 1.3 % | 76 | 968 |
| Recepción (llegada) | 327 | 0 | 0.0 % | 44 | 1 128 |
| Iniciar atención | 287 | 40 | 12.2 % | 60 | 1 198 |
| Leer la consulta | 287 | 0 | 0.0 % | 35 | 909 |
| Registrar diagnóstico | 287 | 0 | 0.0 % | 34 | 887 |
| Registrar receta | 287 | 0 | 0.0 % | 35 | 969 |
| Actualizar mascota | 287 | 0 | 0.0 % | 34 | 783 |
| Cerrar la consulta | 287 | 0 | 0.0 % | 63 | 691 |
| Caja: crear punto de cobro | 128 | 0 | 0.0 % | 40 | 979 |
| Caja: vincular equipo | 128 | 0 | 0.0 % | 38 | 940 |
| Caja: abrir la caja | 128 | 0 | 0.0 % | 49 | 984 |
| Cobrar | 177 | 0 | 0.0 % | 59 | 1 162 |
| Cancelar cita | 155 | 0 | 0.0 % | 50 | 646 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 904 | 0 (0 %) | 599 | 1 806 | 15 % |
| reporte_clinico | 66 | 0 (0 %) | 5 983 | 10 867 | 9 % |
| login | 200 | 0 (0 %) | 860 | 4 900 | 8 % |
| apoderado | 699 | 0 (0 %) | 95 | 1 442 | 6 % |
| propietarios | 556 | 0 (0 %) | 219 | 1 267 | 5 % |
| privacidad | 904 | 0 (0 %) | 87 | 895 | 5 % |
| navegacion | 904 | 0 (0 %) | 28 | 1 030 | 4 % |
| cita | 699 | 9 (1 %) | 76 | 968 | 4 % |
| citas_del_mes | 556 | 0 (0 %) | 79 | 1 450 | 4 % |
| cumplimiento | 904 | 0 (0 %) | 32 | 950 | 4 % |
| lista_de_historias | 489 | 0 (0 %) | 149 | 1 402 | 4 % |
| mascota | 699 | 0 (0 %) | 53 | 1 052 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 20 | 1 664 | 0 % |
| 0.1 | 18 | 119 | 503 | 0 % |
| 0.3 | 33 | 156 | 477 | 1 % |
| 0.6 | 47 | 230 | 435 | 0 % |
| 0.8 | 61 | 226 | 651 | 0 % |
| 1.1 | 75 | 326 | 601 | 0 % |
| 1.3 | 90 | 398 | 435 | 0 % |
| 1.6 | 104 | 469 | 364 | 0 % |
| 1.8 | 118 | 499 | 366 | 0 % |
| 2.1 | 133 | 562 | 1 469 | 1 % |
| 2.3 | 147 | 566 | 245 | 0 % |
| 2.6 | 161 | 489 | 1 544 | 0 % |
| 2.8 | 175 | 578 | 5 818 | 1 % |
| 3.1 | 190 | 658 | 2 153 | 1 % |
| 3.3 | 200 | 756 | 1 231 | 1 % |
| 3.6 | 200 | 689 | 546 | 0 % |
| 3.8 | 200 | 779 | 365 | 0 % |
| 4.1 | 200 | 718 | 1 460 | 0 % |
| 4.3 | 200 | 758 | 688 | 0 % |
| 4.6 | 200 | 837 | 298 | 0 % |
| 4.8 | 200 | 725 | 804 | 0 % |
| 5.1 | 200 | 775 | 642 | 1 % |
| 5.3 | 200 | 689 | 369 | 0 % |
| 5.6 | 200 | 661 | 1 833 | 1 % |
| 5.8 | 200 | 662 | 1 049 | 0 % |
| 6.1 | 192 | 420 | 209 | 0 % |
| 6.3 | 115 | 94 | 142 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
