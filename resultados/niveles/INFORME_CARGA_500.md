# Fase 3: 500 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 133 |
| Peticiones hechas | 18 987 |
| Peticiones correctas (2xx) | 18 653 |
| Errores (%) | 1.8 % |
| Ritmo medio (peticiones/s) | 26.4 |
| Tiempo mediano (ms) | 7 749 |
| Tiempo p95 (ms) | 16 801 |
| CPU del backend, media (% del límite de 2 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 92 |
| Memoria pico (MB, de 2048) | 1 222 |
| Sentencias SQL | 533 625 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5199; fallidas: 334.
- Acciones hechas: panel 892, agenda 803, historia 649, reportes 82, recepcion 1078, atencion 480, cobro 112, cancelar 176; sin trabajo disponible: 792.
- CPU del backend: 29 % de media y 73 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 92 % de media y 105 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 222 MB (60 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **133 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 21.1 s.
- Correo: el servidor de pruebas recibió 1078 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+0.8 %).
- GC: 521 pausas, 56.8 s en total (8 % del tiempo de vida de la JVM); 50 completas, 2 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 654 MB (tope -Xmx1g).
- Clases cargadas: 25501 en total, 88 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 22 | 23 | 5555 |
| 0.1 | 56 | 56 | 12109 |
| 0.1 | 81 | 81 | 16170 |
| 0.2 | 86 | 87 | 17116 |
| 1.0 | 126 | 128 | 24042 |
| 2.2 | 132 | 135 | 25060 |
| 3.0 | 134 | 137 | 25320 |
| 3.8 | 134 | 137 | 25358 |
| 5.0 | 135 | 138 | 25413 |
| 6.6 | 136 | 139 | 25420 |
| 7.9 | 136 | 139 | 25439 |
| 9.2 | 136 | 139 | 25457 |
| 10.6 | 136 | 139 | 25500 |
| 11.7 | 136 | 139 | 25501 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 83 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 86 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1078 | 1078 | coincide |
| Mascotas | 1078 | 1078 | coincide |
| Citas agendadas | 1069 | 1069 | coincide |
| Diagnósticos | 155 | 155 | coincide |
| Recetas | 155 | 155 | coincide |
| Pagos | 112 | 112 | coincide |
| Citas completadas | 155 | 155 | coincide |
| Citas canceladas | 176 | 176 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1078 | 0 | 0.0 % | 8 386 | 16 771 |
| Registrar mascota | 1078 | 0 | 0.0 % | 8 433 | 17 006 |
| Agendar cita | 1069 | 9 | 0.8 % | 8 319 | 16 587 |
| Recepción (llegada) | 480 | 0 | 0.0 % | 8 986 | 17 395 |
| Iniciar atención | 155 | 325 | 67.7 % | 10 861 | 18 368 |
| Leer la consulta | 155 | 0 | 0.0 % | 3 946 | 15 397 |
| Registrar diagnóstico | 155 | 0 | 0.0 % | 2 972 | 13 590 |
| Registrar receta | 155 | 0 | 0.0 % | 4 028 | 15 214 |
| Actualizar mascota | 155 | 0 | 0.0 % | 4 038 | 14 880 |
| Cerrar la consulta | 155 | 0 | 0.0 % | 3 680 | 17 224 |
| Caja: crear punto de cobro | 92 | 0 | 0.0 % | 5 752 | 14 383 |
| Caja: vincular equipo | 92 | 0 | 0.0 % | 5 820 | 16 156 |
| Caja: abrir la caja | 92 | 0 | 0.0 % | 5 625 | 17 316 |
| Cobrar | 112 | 0 | 0.0 % | 6 898 | 18 063 |
| Cancelar cita | 176 | 0 | 0.0 % | 9 599 | 17 320 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1392 | 0 (0 %) | 8 268 | 18 079 | 8 % |
| privacidad | 1392 | 0 (0 %) | 6 799 | 15 739 | 7 % |
| notificaciones | 1392 | 0 (0 %) | 6 883 | 15 599 | 6 % |
| cumplimiento | 1392 | 0 (0 %) | 6 434 | 15 577 | 6 % |
| navegacion | 1392 | 0 (0 %) | 6 604 | 15 703 | 6 % |
| avisos_de_cumplimiento | 1392 | 0 (0 %) | 6 376 | 15 183 | 6 % |
| mascota | 1078 | 0 (0 %) | 8 433 | 17 006 | 6 % |
| cita | 1078 | 9 (1 %) | 8 319 | 16 587 | 6 % |
| apoderado | 1078 | 0 (0 %) | 8 386 | 16 771 | 6 % |
| propietarios | 803 | 0 (0 %) | 9 910 | 17 849 | 5 % |
| citas_del_mes | 803 | 0 (0 %) | 9 614 | 18 087 | 5 % |
| empleados | 803 | 0 (0 %) | 9 177 | 17 990 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 13 | 97 | 331 | 0 % |
| 0.7 | 58 | 290 | 691 | 0 % |
| 1.5 | 103 | 460 | 1 373 | 0 % |
| 2.2 | 148 | 494 | 2 113 | 1 % |
| 3.0 | 193 | 376 | 5 496 | 1 % |
| 3.7 | 238 | 447 | 8 555 | 1 % |
| 4.5 | 283 | 435 | 11 717 | 2 % |
| 5.2 | 328 | 468 | 12 137 | 3 % |
| 6.0 | 373 | 400 | 14 538 | 2 % |
| 6.7 | 418 | 445 | 16 948 | 2 % |
| 7.5 | 463 | 391 | 19 092 | 3 % |
| 8.2 | 500 | 360 | 19 859 | 1 % |
| 9.0 | 500 | 457 | 19 499 | 2 % |
| 9.7 | 500 | 448 | 18 058 | 2 % |
| 10.5 | 500 | 358 | 18 727 | 1 % |
| 11.2 | 419 | 467 | 13 541 | 4 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
