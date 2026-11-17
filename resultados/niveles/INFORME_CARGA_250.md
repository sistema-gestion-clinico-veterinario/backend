# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Veredicto | FALLA |
| Usuarios sin degradación | 141 |
| Peticiones hechas | 5 153 |
| Peticiones correctas (2xx) | 3 393 |
| Errores (%) | 34.2 % |
| Ritmo medio (peticiones/s) | 10.5 |
| Tiempo mediano (ms) | 177 |
| Tiempo p95 (ms) | 30 001 |
| CPU del backend, media (% del límite de 1 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 24 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 13 |
| Memoria pico (MB, de 2048) | 1 355 |
| Sentencias SQL | 95 391 |
| Reinicios del contenedor | 0 |

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1024; fallidas: 249.
- Acciones hechas: panel 198, agenda 180, historia 145, reportes 26, recepcion 308, atencion 91, cobro 39, cancelar 29; sin trabajo disponible: 203.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2972 avisos en el registro.
- CPU del backend: 32 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 24 % del tiempo.
- CPU de PostgreSQL: 13 % de media y 93 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 355 MB (66 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 2.9 con unos 186 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **141 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 196 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 134 MB (+0.4 %).
- GC: 266 pausas, 21.4 s en total (4 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 940 MB (tope -Xmx1g).
- Clases cargadas: 25388 en total, 19 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 13 | 3784 |
| 0.1 | 21 | 21 | 5078 |
| 0.2 | 35 | 35 | 7853 |
| 0.2 | 58 | 59 | 12538 |
| 0.3 | 80 | 81 | 16049 |
| 0.4 | 80 | 81 | 16101 |
| 0.4 | 82 | 83 | 16410 |
| 0.5 | 87 | 88 | 17205 |
| 0.7 | 102 | 104 | 19968 |
| 1.5 | 128 | 130 | 24470 |
| 2.0 | 130 | 133 | 24794 |
| 2.6 | 132 | 135 | 25056 |
| 3.0 | 133 | 136 | 25270 |
| 3.3 | 133 | 136 | 25312 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `org.apache.catalina.core` | 6 |
| `java.lang.invoke.LambdaForm` | 5 |
| `java.util.stream.ReduceOps` | 2 |
| `org.apache.catalina.AsyncDispatcher` | 1 |
| `java.util.function.IntBinaryOperator` | 1 |
| `java.util.stream.IntPipeline` | 1 |
| `org.springframework.boot.web` | 1 |
| `org.springframework.boot.autoconfigure` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 191 | 196 | la base tiene más (ver nota) |
| Mascotas | 176 | 178 | la base tiene más (ver nota) |
| Citas agendadas | 169 | 169 | coincide |
| Diagnósticos | 55 | 55 | coincide |
| Recetas | 52 | 53 | la base tiene más (ver nota) |
| Pagos | 22 | 22 | coincide |
| Citas completadas | 52 | 53 | la base tiene más (ver nota) |
| Citas canceladas | 16 | 16 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 191 | 117 | 38.0 % | 321 | 30 001 |
| Registrar mascota | 176 | 15 | 7.9 % | 82 | 30 001 |
| Agendar cita | 169 | 7 | 4.0 % | 108 | 2 623 |
| Recepción (llegada) | 61 | 30 | 33.0 % | 158 | 30 001 |
| Iniciar atención | 56 | 35 | 38.5 % | 126 | 30 001 |
| Leer la consulta | 55 | 1 | 1.8 % | 54 | 837 |
| Registrar diagnóstico | 55 | 1 | 1.8 % | 55 | 4 982 |
| Registrar receta | 52 | 4 | 7.1 % | 60 | 30 000 |
| Actualizar mascota | 51 | 5 | 8.9 % | 58 | 30 001 |
| Cerrar la consulta | 52 | 4 | 7.1 % | 102 | 30 000 |
| Caja: crear punto de cobro | 25 | 11 | 30.6 % | 102 | 30 001 |
| Caja: vincular equipo | 22 | 3 | 12.0 % | 62 | 30 001 |
| Caja: abrir la caja | 21 | 1 | 4.5 % | 74 | 683 |
| Cobrar | 22 | 2 | 8.3 % | 85 | 30 000 |
| Cancelar cita | 16 | 13 | 44.8 % | 4 222 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 688 | 544 (79 %) | 30 000 | 30 001 | 30 % |
| apoderado | 308 | 117 (38 %) | 321 | 30 001 | 7 % |
| navegacion | 391 | 116 (30 %) | 40 | 30 001 | 7 % |
| privacidad | 391 | 114 (29 %) | 76 | 30 001 | 7 % |
| cumplimiento | 379 | 110 (29 %) | 47 | 30 001 | 6 % |
| notificaciones | 368 | 104 (28 %) | 35 | 30 001 | 6 % |
| avisos_de_cumplimiento | 358 | 93 (26 %) | 39 | 30 001 | 5 % |
| empleados | 190 | 88 (46 %) | 674 | 30 001 | 5 % |
| panel | 342 | 82 (24 %) | 359 | 30 001 | 5 % |
| citas_del_mes | 190 | 83 (44 %) | 547 | 30 001 | 5 % |
| propietarios | 180 | 79 (44 %) | 478 | 30 001 | 5 % |
| lista_de_historias | 145 | 55 (38 %) | 262 | 30 001 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 37 | 1 727 | 0 % |
| 0.4 | 36 | 170 | 1 192 | 0 % |
| 0.9 | 66 | 312 | 582 | 0 % |
| 1.4 | 96 | 421 | 438 | 0 % |
| 1.9 | 126 | 515 | 932 | 0 % |
| 2.4 | 156 | 181 | 8 088 | 0 % |
| 2.9 | 186 | 136 | 30 001 | 80 % |
| 3.4 | 216 | 88 | 30 001 | 91 % |
| 3.9 | 246 | 108 | 30 001 | 99 % |
| 4.4 | 250 | 110 | 30 001 | 100 % |
| 4.9 | 250 | 93 | 30 001 | 100 % |
| 5.4 | 250 | 90 | 30 001 | 100 % |
| 5.9 | 250 | 86 | 30 001 | 100 % |
| 6.4 | 250 | 91 | 30 001 | 100 % |
| 6.9 | 232 | 92 | 30 001 | 100 % |
| 7.4 | 128 | 47 | 30 001 | 100 % |
| 7.9 | 78 | 24 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
