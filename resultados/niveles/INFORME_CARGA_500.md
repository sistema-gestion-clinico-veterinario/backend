# Fase 3: 500 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 84 |
| Peticiones hechas | 17 583 |
| Peticiones correctas (2xx) | 17 302 |
| Errores (%) | 1.6 % |
| Ritmo medio (peticiones/s) | 24.2 |
| Tiempo mediano (ms) | 9 179 |
| Tiempo p95 (ms) | 18 570 |
| CPU del backend, media (% del límite de 2 CPU) | 28 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 92 |
| Memoria pico (MB, de 4096) | 1 187 |
| Sentencias SQL | 489 259 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4514; fallidas: 281.
- Acciones hechas: panel 875, agenda 697, historia 585, reportes 64, recepcion 974, atencion 389, cobro 81, cancelar 170; sin trabajo disponible: 733.
- CPU del backend: 28 % de media y 81 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 92 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 187 MB (29 % de 4096 MB).
- Los fallos fueron: 0 % errores 5xx del servidor, 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **84 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 22.3 s.
- Correo: el servidor de pruebas recibió 974 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+0.7 %).
- GC: 450 pausas, 48.8 s en total (6 % del tiempo de vida de la JVM); 44 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 637 MB (tope -Xmx2g).
- Clases cargadas: 25572 en total, 89 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.1 | 33 | 33 | 7574 |
| 0.1 | 72 | 72 | 14688 |
| 0.2 | 81 | 82 | 16195 |
| 0.2 | 86 | 87 | 17117 |
| 0.7 | 123 | 125 | 23673 |
| 1.8 | 131 | 133 | 24858 |
| 2.8 | 133 | 136 | 25350 |
| 4.0 | 134 | 137 | 25438 |
| 4.9 | 135 | 138 | 25459 |
| 6.0 | 135 | 138 | 25472 |
| 7.4 | 136 | 139 | 25486 |
| 8.7 | 136 | 139 | 25487 |
| 10.3 | 136 | 139 | 25495 |
| 11.7 | 136 | 139 | 25513 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 85 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 88 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 974 | 974 | coincide |
| Mascotas | 974 | 974 | coincide |
| Citas agendadas | 954 | 954 | coincide |
| Diagnósticos | 128 | 128 | coincide |
| Recetas | 128 | 128 | coincide |
| Pagos | 81 | 81 | coincide |
| Citas completadas | 128 | 128 | coincide |
| Citas canceladas | 170 | 170 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 974 | 0 | 0.0 % | 10 339 | 18 651 |
| Registrar mascota | 974 | 0 | 0.0 % | 9 921 | 18 704 |
| Agendar cita | 954 | 20 | 2.1 % | 10 238 | 18 670 |
| Recepción (llegada) | 389 | 0 | 0.0 % | 11 178 | 18 530 |
| Iniciar atención | 128 | 261 | 67.1 % | 12 210 | 20 554 |
| Leer la consulta | 128 | 0 | 0.0 % | 4 530 | 17 797 |
| Registrar diagnóstico | 128 | 0 | 0.0 % | 4 623 | 17 222 |
| Registrar receta | 128 | 0 | 0.0 % | 4 438 | 17 171 |
| Actualizar mascota | 128 | 0 | 0.0 % | 3 550 | 17 639 |
| Cerrar la consulta | 128 | 0 | 0.0 % | 4 000 | 15 664 |
| Caja: crear punto de cobro | 68 | 0 | 0.0 % | 5 156 | 16 667 |
| Caja: vincular equipo | 68 | 0 | 0.0 % | 5 258 | 15 082 |
| Caja: abrir la caja | 68 | 0 | 0.0 % | 5 717 | 19 180 |
| Cobrar | 81 | 0 | 0.0 % | 7 992 | 18 198 |
| Cancelar cita | 170 | 0 | 0.0 % | 10 373 | 18 935 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1375 | 0 (0 %) | 9 676 | 19 911 | 9 % |
| privacidad | 1375 | 0 (0 %) | 8 166 | 17 227 | 7 % |
| notificaciones | 1375 | 0 (0 %) | 8 039 | 17 318 | 7 % |
| avisos_de_cumplimiento | 1375 | 0 (0 %) | 7 880 | 17 090 | 7 % |
| cumplimiento | 1375 | 0 (0 %) | 8 067 | 17 150 | 7 % |
| navegacion | 1375 | 0 (0 %) | 7 966 | 16 962 | 7 % |
| apoderado | 974 | 0 (0 %) | 10 339 | 18 651 | 6 % |
| cita | 974 | 20 (2 %) | 10 238 | 18 670 | 6 % |
| mascota | 974 | 0 (0 %) | 9 921 | 18 704 | 6 % |
| propietarios | 697 | 0 (0 %) | 11 692 | 19 614 | 5 % |
| citas_del_mes | 697 | 0 (0 %) | 11 471 | 19 713 | 5 % |
| empleados | 697 | 0 (0 %) | 10 985 | 19 425 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 64 | 376 | 0 % |
| 0.7 | 54 | 258 | 444 | 0 % |
| 1.4 | 99 | 383 | 2 008 | 0 % |
| 2.2 | 144 | 442 | 2 610 | 0 % |
| 2.9 | 189 | 460 | 5 322 | 1 % |
| 3.7 | 234 | 432 | 8 019 | 1 % |
| 4.4 | 279 | 396 | 11 238 | 1 % |
| 5.2 | 324 | 402 | 12 532 | 1 % |
| 5.9 | 369 | 380 | 16 802 | 2 % |
| 6.7 | 414 | 354 | 18 032 | 2 % |
| 7.4 | 459 | 383 | 19 846 | 2 % |
| 8.2 | 500 | 392 | 20 648 | 3 % |
| 8.9 | 500 | 372 | 22 302 | 2 % |
| 9.7 | 500 | 428 | 18 892 | 2 % |
| 10.4 | 500 | 367 | 17 891 | 2 % |
| 11.2 | 432 | 346 | 17 674 | 3 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
