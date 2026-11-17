# Fase 3: 350 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 88 |
| Peticiones hechas | 8 192 |
| Peticiones correctas (2xx) | 5 473 |
| Errores (%) | 33.2 % |
| Ritmo medio (peticiones/s) | 13.9 |
| Tiempo mediano (ms) | 2 544 |
| Tiempo p95 (ms) | 30 001 |
| CPU del backend, media (% del límite de 1 CPU) | 37 |
| Tiempo con la CPU al tope (%) | 15 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 32 |
| Memoria pico (MB, de 2048) | 1 075 |
| Sentencias SQL | 157 082 |
| Reinicios del contenedor | 0 |

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 1486; fallidas: 491.
- Acciones hechas: panel 342, agenda 310, historia 293, reportes 27, recepcion 569, atencion 180, cobro 36, cancelar 58; sin trabajo disponible: 412.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2740 avisos en el registro.
- CPU del backend: 37 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 32 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 075 MB (52 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 4.2 con unos 268 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **88 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 354 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 135 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 135 MB (+0.3 %).
- GC: 272 pausas, 17.7 s en total (3 % del tiempo de vida de la JVM); 25 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 537 MB (tope -Xmx1g).
- Clases cargadas: 25411 en total, 81 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 13 | 14 | 3807 |
| 0.1 | 22 | 22 | 5456 |
| 0.2 | 39 | 40 | 8794 |
| 0.2 | 66 | 66 | 13661 |
| 0.3 | 80 | 81 | 16070 |
| 0.4 | 80 | 81 | 16103 |
| 0.5 | 86 | 87 | 17085 |
| 0.5 | 87 | 88 | 17213 |
| 1.1 | 119 | 121 | 22853 |
| 1.9 | 129 | 132 | 24506 |
| 2.4 | 131 | 134 | 24924 |
| 3.2 | 133 | 136 | 25251 |
| 4.2 | 134 | 137 | 25302 |
| 4.9 | 134 | 137 | 25330 |

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
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 336 | 354 | la base tiene más (ver nota) |
| Mascotas | 318 | 323 | la base tiene más (ver nota) |
| Citas agendadas | 306 | 308 | la base tiene más (ver nota) |
| Diagnósticos | 43 | 43 | coincide |
| Recetas | 43 | 43 | coincide |
| Pagos | 22 | 22 | coincide |
| Citas completadas | 41 | 41 | coincide |
| Citas canceladas | 50 | 50 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 336 | 233 | 40.9 % | 4 416 | 30 001 |
| Registrar mascota | 318 | 18 | 5.4 % | 340 | 30 000 |
| Agendar cita | 306 | 12 | 3.8 % | 322 | 10 468 |
| Recepción (llegada) | 124 | 56 | 31.1 % | 2 991 | 30 001 |
| Iniciar atención | 46 | 134 | 74.4 % | 3 255 | 30 001 |
| Leer la consulta | 44 | 2 | 4.3 % | 404 | 11 115 |
| Registrar diagnóstico | 43 | 3 | 6.5 % | 163 | 30 000 |
| Registrar receta | 43 | 3 | 6.5 % | 166 | 30 000 |
| Actualizar mascota | 43 | 3 | 6.5 % | 155 | 30 000 |
| Cerrar la consulta | 41 | 5 | 10.9 % | 439 | 30 001 |
| Caja: crear punto de cobro | 24 | 9 | 27.3 % | 2 573 | 30 001 |
| Caja: vincular equipo | 24 | 0 | 0.0 % | 763 | 11 253 |
| Caja: abrir la caja | 22 | 2 | 8.3 % | 1 390 | 30 000 |
| Cobrar | 22 | 3 | 12.0 % | 1 188 | 30 001 |
| Cancelar cita | 50 | 8 | 13.8 % | 1 740 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 843 | 610 (72 %) | 30 000 | 30 001 | 21 % |
| apoderado | 569 | 233 (41 %) | 4 416 | 30 001 | 9 % |
| privacidad | 638 | 180 (28 %) | 2 647 | 30 001 | 7 % |
| cumplimiento | 623 | 182 (29 %) | 1 764 | 30 001 | 7 % |
| panel | 575 | 164 (29 %) | 2 389 | 30 001 | 7 % |
| notificaciones | 609 | 173 (28 %) | 1 300 | 30 001 | 6 % |
| navegacion | 638 | 157 (25 %) | 1 646 | 30 001 | 6 % |
| avisos_de_cumplimiento | 595 | 165 (28 %) | 901 | 30 001 | 6 % |
| empleados | 334 | 148 (44 %) | 6 383 | 30 001 | 5 % |
| citas_del_mes | 334 | 140 (42 %) | 4 940 | 30 001 | 5 % |
| propietarios | 310 | 130 (42 %) | 4 166 | 30 001 | 5 % |
| lista_de_historias | 293 | 119 (41 %) | 4 333 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 13 | 97 | 940 | 0 % |
| 0.5 | 43 | 221 | 515 | 0 % |
| 1.0 | 73 | 288 | 684 | 0 % |
| 1.5 | 103 | 396 | 1 279 | 1 % |
| 2.0 | 133 | 435 | 2 309 | 2 % |
| 2.5 | 163 | 365 | 4 453 | 1 % |
| 3.0 | 193 | 412 | 7 448 | 2 % |
| 3.5 | 223 | 431 | 9 977 | 2 % |
| 4.0 | 253 | 111 | 15 774 | 2 % |
| 4.5 | 283 | 156 | 30 001 | 93 % |
| 5.0 | 313 | 142 | 30 001 | 99 % |
| 5.5 | 343 | 129 | 30 001 | 100 % |
| 6.0 | 350 | 141 | 30 001 | 100 % |
| 6.5 | 350 | 159 | 30 001 | 100 % |
| 7.0 | 350 | 144 | 30 001 | 100 % |
| 7.5 | 350 | 144 | 30 001 | 100 % |
| 8.0 | 350 | 121 | 30 001 | 100 % |
| 8.5 | 339 | 129 | 30 001 | 100 % |
| 9.0 | 203 | 84 | 30 001 | 100 % |
| 9.5 | 117 | 36 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
