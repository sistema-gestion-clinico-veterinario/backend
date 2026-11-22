# Fase 3: 500 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 144 |
| Peticiones hechas | 23 524 |
| Peticiones correctas (2xx) | 23 072 |
| Errores (%) | 1.9 % |
| Ritmo medio (peticiones/s) | 32.7 |
| Tiempo mediano (ms) | 5 290 |
| Tiempo p95 (ms) | 12 348 |
| CPU del backend, media (% del límite de 2 CPU) | 31 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 88 |
| Memoria pico (MB, de 4096) | 1 356 |
| Sentencias SQL | 661 636 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 6690; fallidas: 452.
- Acciones hechas: panel 1180, agenda 1003, historia 824, reportes 107, recepcion 1358, atencion 642, cobro 150, cancelar 245; sin trabajo disponible: 1032.
- CPU del backend: 31 % de media y 92 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 88 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 356 MB (33 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 9.4 con unos 500 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **144 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 17.0 s.
- Correo: el servidor de pruebas recibió 1358 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.6 %).
- GC: 537 pausas, 53.9 s en total (8 % del tiempo de vida de la JVM); 51 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 749 MB (tope -Xmx2g).
- Clases cargadas: 25559 en total, 122 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1188 |
| 0.1 | 42 | 42 | 9233 |
| 0.2 | 81 | 81 | 16154 |
| 0.3 | 86 | 87 | 17101 |
| 0.5 | 117 | 119 | 22576 |
| 1.6 | 131 | 133 | 24833 |
| 2.8 | 134 | 136 | 25335 |
| 3.8 | 135 | 137 | 25404 |
| 5.1 | 135 | 138 | 25430 |
| 6.1 | 136 | 139 | 25495 |
| 7.5 | 136 | 139 | 25498 |
| 8.6 | 136 | 139 | 25509 |
| 9.6 | 136 | 139 | 25517 |
| 10.4 | 136 | 139 | 25558 |
| 11.6 | 137 | 140 | 25559 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 114 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 119 |
| `com.fasterxml.jackson.databind` | 2 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1358 | 1358 | coincide |
| Mascotas | 1358 | 1358 | coincide |
| Citas agendadas | 1341 | 1341 | coincide |
| Diagnósticos | 207 | 207 | coincide |
| Recetas | 207 | 207 | coincide |
| Pagos | 150 | 150 | coincide |
| Citas completadas | 207 | 207 | coincide |
| Citas canceladas | 245 | 245 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1358 | 0 | 0.0 % | 5 902 | 12 509 |
| Registrar mascota | 1358 | 0 | 0.0 % | 5 952 | 12 584 |
| Agendar cita | 1341 | 17 | 1.3 % | 5 856 | 12 294 |
| Recepción (llegada) | 642 | 0 | 0.0 % | 6 481 | 12 480 |
| Iniciar atención | 207 | 435 | 67.8 % | 7 391 | 14 240 |
| Leer la consulta | 207 | 0 | 0.0 % | 2 235 | 11 281 |
| Registrar diagnóstico | 207 | 0 | 0.0 % | 2 949 | 10 389 |
| Registrar receta | 207 | 0 | 0.0 % | 2 864 | 11 808 |
| Actualizar mascota | 207 | 0 | 0.0 % | 2 607 | 11 246 |
| Cerrar la consulta | 207 | 0 | 0.0 % | 2 679 | 10 914 |
| Caja: crear punto de cobro | 118 | 0 | 0.0 % | 4 175 | 11 186 |
| Caja: vincular equipo | 118 | 0 | 0.0 % | 4 175 | 10 833 |
| Caja: abrir la caja | 118 | 0 | 0.0 % | 4 272 | 12 099 |
| Cobrar | 150 | 0 | 0.0 % | 4 683 | 11 826 |
| Cancelar cita | 245 | 0 | 0.0 % | 6 632 | 13 237 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1680 | 0 (0 %) | 5 608 | 12 842 | 8 % |
| privacidad | 1680 | 0 (0 %) | 4 751 | 11 391 | 7 % |
| navegacion | 1680 | 0 (0 %) | 4 350 | 11 459 | 6 % |
| cita | 1358 | 17 (1 %) | 5 856 | 12 294 | 6 % |
| apoderado | 1358 | 0 (0 %) | 5 902 | 12 509 | 6 % |
| mascota | 1358 | 0 (0 %) | 5 952 | 12 584 | 6 % |
| cumplimiento | 1680 | 0 (0 %) | 4 387 | 11 137 | 6 % |
| notificaciones | 1680 | 0 (0 %) | 4 227 | 10 976 | 6 % |
| avisos_de_cumplimiento | 1680 | 0 (0 %) | 4 222 | 10 911 | 6 % |
| propietarios | 1003 | 0 (0 %) | 6 844 | 13 649 | 5 % |
| citas_del_mes | 1003 | 0 (0 %) | 6 626 | 13 046 | 5 % |
| empleados | 1003 | 0 (0 %) | 6 260 | 12 939 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 63 | 357 | 0 % |
| 0.7 | 54 | 249 | 319 | 0 % |
| 1.4 | 99 | 453 | 362 | 0 % |
| 2.2 | 144 | 564 | 942 | 0 % |
| 2.9 | 189 | 537 | 2 565 | 0 % |
| 3.7 | 234 | 506 | 6 107 | 2 % |
| 4.4 | 279 | 576 | 6 112 | 1 % |
| 5.2 | 324 | 565 | 9 705 | 2 % |
| 5.9 | 369 | 620 | 8 267 | 3 % |
| 6.7 | 414 | 565 | 10 322 | 2 % |
| 7.4 | 459 | 562 | 12 271 | 2 % |
| 8.2 | 500 | 523 | 15 921 | 2 % |
| 8.9 | 500 | 520 | 11 483 | 3 % |
| 9.7 | 500 | 754 | 12 383 | 3 % |
| 10.4 | 500 | 598 | 12 502 | 4 % |
| 11.2 | 423 | 479 | 14 331 | 3 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
