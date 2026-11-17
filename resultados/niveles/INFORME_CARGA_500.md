# Fase 3: 500 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 54 |
| Peticiones hechas | 10 400 |
| Peticiones correctas (2xx) | 5 538 |
| Errores (%) | 46.8 % |
| Ritmo medio (peticiones/s) | 14.1 |
| Tiempo mediano (ms) | 9 505 |
| Tiempo p95 (ms) | 30 001 |
| CPU del backend, media (% del límite de 1 CPU) | 33 |
| Tiempo con la CPU al tope (%) | 12 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 31 |
| Memoria pico (MB, de 2048) | 1 224 |
| Sentencias SQL | 161 604 |
| Reinicios del contenedor | 0 |

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1215; fallidas: 726.
- Acciones hechas: panel 441, agenda 389, historia 396, reportes 54, recepcion 762, atencion 194, cobro 4, cancelar 71; sin trabajo disponible: 537.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3891 avisos en el registro.
- CPU del backend: 33 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 12 % del tiempo.
- CPU de PostgreSQL: 31 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 224 MB (60 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.4 con unos 279 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **54 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 372 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.3 %).
- GC: 301 pausas, 24.4 s en total (3 % del tiempo de vida de la JVM); 28 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 645 MB (tope -Xmx1g).
- Clases cargadas: 25311 en total, 82 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3837 |
| 0.1 | 24 | 25 | 5800 |
| 0.2 | 50 | 50 | 10965 |
| 0.3 | 75 | 76 | 15362 |
| 0.4 | 80 | 81 | 16100 |
| 0.4 | 82 | 83 | 16409 |
| 0.5 | 87 | 88 | 17205 |
| 1.1 | 123 | 125 | 23633 |
| 1.9 | 129 | 132 | 24483 |
| 2.7 | 131 | 134 | 24724 |
| 3.4 | 132 | 135 | 25000 |
| 4.1 | 133 | 136 | 25087 |
| 4.8 | 134 | 137 | 25198 |
| 12.3 | 135 | 138 | 25311 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 35 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 39 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.security.core` | 2 |
| `org.springframework.dao.support` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `com.fasterxml.jackson.annotation` | 2 |
| `org.hibernate.internal.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 352 | 372 | la base tiene más (ver nota) |
| Mascotas | 333 | 333 | coincide |
| Citas agendadas | 312 | 313 | la base tiene más (ver nota) |
| Diagnósticos | 5 | 5 | coincide |
| Recetas | 5 | 5 | coincide |
| Pagos | 2 | 2 | coincide |
| Citas completadas | 4 | 5 | la base tiene más (ver nota) |
| Citas canceladas | 47 | 49 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 352 | 410 | 53.8 % | 30 000 | 30 001 |
| Registrar mascota | 333 | 19 | 5.4 % | 1 378 | 30 000 |
| Agendar cita | 312 | 21 | 6.3 % | 1 051 | 16 755 |
| Recepción (llegada) | 134 | 60 | 30.9 % | 4 538 | 30 001 |
| Iniciar atención | 5 | 189 | 97.4 % | 5 234 | 30 001 |
| Leer la consulta | 5 | 0 | 0.0 % | 783 | 20 841 |
| Registrar diagnóstico | 5 | 0 | 0.0 % | 2 626 | 6 764 |
| Registrar receta | 5 | 0 | 0.0 % | 925 | 14 186 |
| Actualizar mascota | 5 | 0 | 0.0 % | 4 633 | 7 587 |
| Cerrar la consulta | 4 | 1 | 20.0 % | 1 543 | 30 001 |
| Caja: crear punto de cobro | 2 | 2 | 50.0 % | 20 384 | 30 000 |
| Caja: vincular equipo | 2 | 0 | 0.0 % | 4 258 | 5 697 |
| Caja: abrir la caja | 2 | 0 | 0.0 % | 5 401 | 6 676 |
| Cobrar | 2 | 0 | 0.0 % | 5 321 | 7 049 |
| Cancelar cita | 47 | 24 | 33.8 % | 4 816 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1888 | 1632 (86 %) | 30 000 | 30 001 | 31 % |
| apoderado | 762 | 410 (54 %) | 30 000 | 30 001 | 9 % |
| privacidad | 766 | 271 (35 %) | 4 522 | 30 001 | 6 % |
| cumplimiento | 753 | 271 (36 %) | 3 960 | 30 001 | 6 % |
| navegacion | 766 | 260 (34 %) | 3 785 | 30 001 | 6 % |
| notificaciones | 736 | 263 (36 %) | 3 303 | 30 001 | 6 % |
| panel | 697 | 247 (35 %) | 4 380 | 30 001 | 5 % |
| avisos_de_cumplimiento | 713 | 249 (35 %) | 3 783 | 30 001 | 5 % |
| empleados | 412 | 218 (53 %) | 30 000 | 30 001 | 5 % |
| citas_del_mes | 412 | 211 (51 %) | 30 000 | 30 001 | 4 % |
| lista_de_historias | 396 | 203 (51 %) | 30 000 | 30 001 | 4 % |
| propietarios | 389 | 207 (53 %) | 30 000 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 56 | 1 746 | 0 % |
| 0.7 | 54 | 240 | 1 019 | 1 % |
| 1.4 | 99 | 374 | 1 218 | 4 % |
| 2.2 | 144 | 392 | 3 150 | 4 % |
| 2.9 | 189 | 314 | 9 947 | 0 % |
| 3.7 | 234 | 338 | 15 668 | 3 % |
| 4.4 | 279 | 76 | 21 453 | 8 % |
| 5.2 | 324 | 87 | 30 001 | 100 % |
| 5.9 | 369 | 142 | 30 001 | 100 % |
| 6.7 | 414 | 165 | 30 001 | 100 % |
| 7.4 | 459 | 172 | 30 001 | 100 % |
| 8.2 | 500 | 208 | 30 001 | 100 % |
| 8.9 | 500 | 177 | 30 001 | 100 % |
| 9.7 | 500 | 186 | 30 001 | 100 % |
| 10.4 | 500 | 185 | 30 001 | 100 % |
| 11.2 | 419 | 167 | 30 001 | 100 % |
| 11.9 | 136 | 69 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
