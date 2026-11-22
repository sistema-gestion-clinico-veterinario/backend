# Fase 3: 350 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 135 |
| Peticiones hechas | 14 756 |
| Peticiones correctas (2xx) | 14 509 |
| Errores (%) | 1.7 % |
| Ritmo medio (peticiones/s) | 26.4 |
| Tiempo mediano (ms) | 4 788 |
| Tiempo p95 (ms) | 10 686 |
| CPU del backend, media (% del límite de 2 CPU) | 28 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 87 |
| Memoria pico (MB, de 2048) | 1 232 |
| Sentencias SQL | 411 874 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4181; fallidas: 247.
- Acciones hechas: panel 736, agenda 597, historia 495, reportes 62, recepcion 849, atencion 371, cobro 85, cancelar 144; sin trabajo disponible: 607.
- CPU del backend: 28 % de media y 72 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 87 % de media y 104 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 232 MB (60 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **135 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 13.4 s.
- Correo: el servidor de pruebas recibió 849 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 407 pausas, 42.8 s en total (8 % del tiempo de vida de la JVM); 37 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 697 MB (tope -Xmx1g).
- Clases cargadas: 25504 en total, 95 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 19 | 20 | 5003 |
| 0.1 | 34 | 34 | 7831 |
| 0.1 | 71 | 72 | 14595 |
| 0.2 | 81 | 82 | 16196 |
| 0.2 | 87 | 88 | 17212 |
| 0.8 | 123 | 125 | 23735 |
| 1.8 | 131 | 134 | 24864 |
| 2.8 | 133 | 136 | 25300 |
| 3.6 | 134 | 137 | 25374 |
| 4.7 | 135 | 138 | 25409 |
| 5.9 | 135 | 138 | 25434 |
| 7.2 | 136 | 139 | 25436 |
| 8.3 | 136 | 139 | 25443 |
| 9.3 | 136 | 139 | 25504 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 91 |
| `java.lang.invoke.LambdaForm$DMH/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 94 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 849 | 849 | coincide |
| Mascotas | 849 | 849 | coincide |
| Citas agendadas | 833 | 833 | coincide |
| Diagnósticos | 140 | 140 | coincide |
| Recetas | 140 | 140 | coincide |
| Pagos | 85 | 85 | coincide |
| Citas completadas | 140 | 140 | coincide |
| Citas canceladas | 144 | 144 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 849 | 0 | 0.0 % | 5 158 | 10 417 |
| Registrar mascota | 849 | 0 | 0.0 % | 5 109 | 10 662 |
| Agendar cita | 833 | 16 | 1.9 % | 5 253 | 10 791 |
| Recepción (llegada) | 371 | 0 | 0.0 % | 5 001 | 11 065 |
| Iniciar atención | 140 | 231 | 62.3 % | 6 298 | 11 988 |
| Leer la consulta | 140 | 0 | 0.0 % | 3 452 | 9 794 |
| Registrar diagnóstico | 140 | 0 | 0.0 % | 3 739 | 10 018 |
| Registrar receta | 140 | 0 | 0.0 % | 3 919 | 10 520 |
| Actualizar mascota | 140 | 0 | 0.0 % | 3 588 | 10 541 |
| Cerrar la consulta | 140 | 0 | 0.0 % | 3 264 | 10 257 |
| Caja: crear punto de cobro | 70 | 0 | 0.0 % | 4 045 | 8 741 |
| Caja: vincular equipo | 70 | 0 | 0.0 % | 3 030 | 11 392 |
| Caja: abrir la caja | 70 | 0 | 0.0 % | 3 298 | 9 318 |
| Cobrar | 85 | 0 | 0.0 % | 3 901 | 8 722 |
| Cancelar cita | 144 | 0 | 0.0 % | 5 499 | 10 777 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1086 | 0 (0 %) | 5 656 | 11 605 | 8 % |
| privacidad | 1086 | 0 (0 %) | 4 770 | 9 838 | 7 % |
| cita | 849 | 16 (2 %) | 5 253 | 10 791 | 6 % |
| navegacion | 1086 | 0 (0 %) | 3 733 | 9 567 | 6 % |
| notificaciones | 1086 | 0 (0 %) | 3 872 | 9 454 | 6 % |
| cumplimiento | 1086 | 0 (0 %) | 3 744 | 9 395 | 6 % |
| avisos_de_cumplimiento | 1086 | 0 (0 %) | 3 845 | 9 144 | 6 % |
| apoderado | 849 | 0 (0 %) | 5 158 | 10 417 | 6 % |
| mascota | 849 | 0 (0 %) | 5 109 | 10 662 | 6 % |
| propietarios | 597 | 0 (0 %) | 6 133 | 11 406 | 5 % |
| citas_del_mes | 597 | 0 (0 %) | 5 862 | 11 941 | 5 % |
| empleados | 597 | 0 (0 %) | 5 437 | 11 188 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 15 | 123 | 316 | 0 % |
| 0.5 | 45 | 221 | 299 | 0 % |
| 1.0 | 75 | 287 | 807 | 0 % |
| 1.5 | 105 | 436 | 497 | 0 % |
| 2.0 | 135 | 471 | 1 791 | 0 % |
| 2.5 | 165 | 386 | 5 464 | 2 % |
| 3.0 | 195 | 364 | 6 559 | 1 % |
| 3.5 | 225 | 275 | 11 050 | 2 % |
| 4.0 | 255 | 453 | 10 191 | 1 % |
| 4.5 | 285 | 465 | 11 676 | 2 % |
| 5.0 | 315 | 450 | 10 723 | 2 % |
| 5.5 | 345 | 423 | 12 394 | 1 % |
| 6.0 | 350 | 478 | 11 448 | 3 % |
| 6.5 | 350 | 439 | 13 181 | 3 % |
| 7.0 | 350 | 590 | 9 863 | 3 % |
| 7.5 | 350 | 420 | 9 089 | 2 % |
| 8.0 | 350 | 491 | 10 082 | 3 % |
| 8.5 | 333 | 477 | 11 853 | 3 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
