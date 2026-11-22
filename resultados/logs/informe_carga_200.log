# Fase 3: 200 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 192 |
| Peticiones hechas | 14 640 |
| Peticiones correctas (2xx) | 14 602 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 35.8 |
| Tiempo mediano (ms) | 29 |
| Tiempo p95 (ms) | 961 |
| CPU del backend, media (% del límite de 2 CPU) | 33 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 57 |
| Memoria pico (MB, de 2048) | 1 269 |
| Sentencias SQL | 399 832 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5171; fallidas: 38.
- Acciones hechas: panel 756, agenda 565, historia 534, reportes 66, recepcion 707, atencion 339, cobro 213, cancelar 157; sin trabajo disponible: 516.
- CPU del backend: 33 % de media y 64 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 57 % de media y 103 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 269 MB (62 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **192 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.2 s.
- Correo: el servidor de pruebas recibió 707 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 407 pausas, 38.8 s en total (10 % del tiempo de vida de la JVM); 39 completas, 2 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 865 MB (tope -Xmx1g).
- Clases cargadas: 25578 en total, 181 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 19 | 19 | 4953 |
| 0.1 | 33 | 34 | 7605 |
| 0.1 | 67 | 67 | 13829 |
| 0.2 | 81 | 82 | 16192 |
| 0.2 | 86 | 87 | 17101 |
| 0.3 | 110 | 112 | 21280 |
| 1.4 | 130 | 132 | 24616 |
| 2.3 | 132 | 135 | 25041 |
| 2.9 | 134 | 136 | 25331 |
| 3.5 | 134 | 137 | 25399 |
| 4.0 | 135 | 138 | 25416 |
| 4.8 | 135 | 138 | 25491 |
| 5.5 | 136 | 139 | 25531 |
| 6.3 | 136 | 139 | 25560 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 170 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 177 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 707 | 707 | coincide |
| Mascotas | 707 | 707 | coincide |
| Citas agendadas | 684 | 684 | coincide |
| Diagnósticos | 324 | 324 | coincide |
| Recetas | 324 | 324 | coincide |
| Pagos | 213 | 213 | coincide |
| Citas completadas | 324 | 324 | coincide |
| Citas canceladas | 157 | 157 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 707 | 0 | 0.0 % | 49 | 827 |
| Registrar mascota | 707 | 0 | 0.0 % | 25 | 764 |
| Agendar cita | 684 | 23 | 3.3 % | 37 | 714 |
| Recepción (llegada) | 339 | 0 | 0.0 % | 21 | 806 |
| Iniciar atención | 324 | 15 | 4.4 % | 29 | 1 009 |
| Leer la consulta | 324 | 0 | 0.0 % | 18 | 467 |
| Registrar diagnóstico | 324 | 0 | 0.0 % | 16 | 556 |
| Registrar receta | 324 | 0 | 0.0 % | 18 | 520 |
| Actualizar mascota | 324 | 0 | 0.0 % | 17 | 373 |
| Cerrar la consulta | 324 | 0 | 0.0 % | 28 | 410 |
| Caja: crear punto de cobro | 140 | 0 | 0.0 % | 23 | 1 105 |
| Caja: vincular equipo | 140 | 0 | 0.0 % | 20 | 990 |
| Caja: abrir la caja | 140 | 0 | 0.0 % | 28 | 1 045 |
| Cobrar | 213 | 0 | 0.0 % | 28 | 857 |
| Cancelar cita | 157 | 0 | 0.0 % | 28 | 582 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 956 | 0 (0 %) | 330 | 1 811 | 21 % |
| reporte_clinico | 66 | 0 (0 %) | 2 809 | 6 955 | 9 % |
| propietarios | 565 | 0 (0 %) | 111 | 1 102 | 6 % |
| privacidad | 956 | 0 (0 %) | 47 | 656 | 5 % |
| apoderado | 707 | 0 (0 %) | 49 | 827 | 5 % |
| lista_de_historias | 534 | 0 (0 %) | 82 | 996 | 4 % |
| citas_del_mes | 565 | 0 (0 %) | 42 | 981 | 4 % |
| navegacion | 956 | 0 (0 %) | 13 | 644 | 4 % |
| cita | 707 | 23 (3 %) | 37 | 714 | 4 % |
| mascota | 707 | 0 (0 %) | 25 | 764 | 3 % |
| notificaciones | 956 | 0 (0 %) | 9 | 574 | 3 % |
| empleados | 565 | 0 (0 %) | 27 | 841 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 383 | 0 % |
| 0.1 | 20 | 125 | 350 | 0 % |
| 0.4 | 35 | 161 | 327 | 0 % |
| 0.6 | 49 | 230 | 382 | 0 % |
| 0.9 | 63 | 320 | 303 | 0 % |
| 1.1 | 77 | 356 | 258 | 1 % |
| 1.4 | 92 | 402 | 283 | 0 % |
| 1.6 | 106 | 458 | 201 | 0 % |
| 1.9 | 120 | 520 | 271 | 0 % |
| 2.1 | 135 | 576 | 890 | 0 % |
| 2.4 | 149 | 651 | 331 | 0 % |
| 2.6 | 163 | 717 | 507 | 0 % |
| 2.9 | 177 | 744 | 434 | 0 % |
| 3.1 | 192 | 735 | 1 083 | 0 % |
| 3.4 | 200 | 628 | 2 227 | 1 % |
| 3.6 | 200 | 791 | 1 893 | 1 % |
| 3.9 | 200 | 860 | 249 | 0 % |
| 4.1 | 200 | 704 | 2 101 | 1 % |
| 4.4 | 200 | 702 | 201 | 0 % |
| 4.6 | 200 | 742 | 239 | 0 % |
| 4.9 | 200 | 729 | 838 | 0 % |
| 5.1 | 200 | 755 | 797 | 0 % |
| 5.4 | 200 | 785 | 274 | 0 % |
| 5.6 | 200 | 758 | 243 | 0 % |
| 5.9 | 200 | 730 | 123 | 0 % |
| 6.1 | 190 | 385 | 265 | 0 % |
| 6.4 | 109 | 34 | 34 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
