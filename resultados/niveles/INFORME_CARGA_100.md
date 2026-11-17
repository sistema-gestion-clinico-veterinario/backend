# Fase 3: 100 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 112 |
| Peticiones correctas (2xx) | 6 102 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 20.0 |
| Tiempo mediano (ms) | 24 |
| Tiempo p95 (ms) | 499 |
| CPU del backend, media (% del límite de 2 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 56 |
| Memoria pico (MB, de 4096) | 1 126 |
| Sentencias SQL | 167 922 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2348; fallidas: 10.
- Acciones hechas: panel 281, agenda 222, historia 197, reportes 37, recepcion 322, atencion 152, cobro 87, cancelar 56; sin trabajo disponible: 228.
- CPU del backend: 29 % de media y 90 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 56 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 126 MB (28 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.5 s.
- Correo: el servidor de pruebas recibió 322 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.6 %).
- GC: 279 pausas, 26.1 s en total (8 % del tiempo de vida de la JVM); 30 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 598 MB (tope -Xmx2g).
- Clases cargadas: 25446 en total, 175 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 24 | 25 | 5842 |
| 0.1 | 42 | 43 | 9395 |
| 0.1 | 67 | 67 | 13802 |
| 0.1 | 81 | 81 | 16162 |
| 0.2 | 81 | 82 | 16194 |
| 0.2 | 85 | 86 | 17016 |
| 0.3 | 87 | 88 | 17211 |
| 0.8 | 122 | 124 | 23382 |
| 1.5 | 130 | 132 | 24612 |
| 2.2 | 132 | 134 | 24981 |
| 2.6 | 133 | 136 | 25243 |
| 3.3 | 134 | 136 | 25322 |
| 4.1 | 134 | 137 | 25370 |
| 4.6 | 134 | 137 | 25428 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 170 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 174 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 322 | 322 | coincide |
| Mascotas | 322 | 322 | coincide |
| Citas agendadas | 314 | 314 | coincide |
| Diagnósticos | 150 | 150 | coincide |
| Recetas | 150 | 150 | coincide |
| Pagos | 87 | 87 | coincide |
| Citas completadas | 150 | 150 | coincide |
| Citas canceladas | 56 | 56 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 322 | 0 | 0.0 % | 48 | 352 |
| Registrar mascota | 322 | 0 | 0.0 % | 30 | 131 |
| Agendar cita | 314 | 8 | 2.5 % | 42 | 133 |
| Recepción (llegada) | 152 | 0 | 0.0 % | 26 | 178 |
| Iniciar atención | 150 | 2 | 1.3 % | 31 | 119 |
| Leer la consulta | 150 | 0 | 0.0 % | 19 | 59 |
| Registrar diagnóstico | 150 | 0 | 0.0 % | 18 | 80 |
| Registrar receta | 150 | 0 | 0.0 % | 20 | 66 |
| Actualizar mascota | 150 | 0 | 0.0 % | 19 | 64 |
| Cerrar la consulta | 150 | 0 | 0.0 % | 33 | 90 |
| Caja: crear punto de cobro | 65 | 0 | 0.0 % | 17 | 89 |
| Caja: vincular equipo | 65 | 0 | 0.0 % | 15 | 50 |
| Caja: abrir la caja | 65 | 0 | 0.0 % | 15 | 79 |
| Cobrar | 87 | 0 | 0.0 % | 26 | 156 |
| Cancelar cita | 56 | 0 | 0.0 % | 34 | 541 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 381 | 0 (0 %) | 515 | 2 931 | 44 % |
| reporte_clinico | 37 | 0 (0 %) | 4 281 | 6 173 | 18 % |
| login | 100 | 0 (0 %) | 299 | 757 | 5 % |
| apoderado | 322 | 0 (0 %) | 48 | 352 | 4 % |
| lista_de_historias | 197 | 0 (0 %) | 64 | 545 | 3 % |
| propietarios | 222 | 0 (0 %) | 92 | 272 | 3 % |
| citas_del_mes | 222 | 0 (0 %) | 37 | 311 | 2 % |
| cita | 322 | 8 (2 %) | 42 | 133 | 2 % |
| navegacion | 381 | 0 (0 %) | 14 | 153 | 2 % |
| mascota | 322 | 0 (0 %) | 30 | 131 | 2 % |
| historia | 197 | 0 (0 %) | 33 | 143 | 1 % |
| empleados | 222 | 0 (0 %) | 28 | 85 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 10 | 70 | 328 | 0 % |
| 0.2 | 24 | 137 | 396 | 1 % |
| 0.5 | 38 | 166 | 307 | 1 % |
| 0.7 | 51 | 220 | 365 | 1 % |
| 0.9 | 65 | 306 | 349 | 0 % |
| 1.2 | 79 | 327 | 292 | 0 % |
| 1.4 | 92 | 403 | 504 | 0 % |
| 1.7 | 100 | 393 | 1 987 | 0 % |
| 1.9 | 100 | 399 | 359 | 0 % |
| 2.2 | 100 | 411 | 391 | 0 % |
| 2.5 | 100 | 397 | 138 | 0 % |
| 2.7 | 100 | 372 | 555 | 0 % |
| 3.0 | 100 | 390 | 776 | 0 % |
| 3.2 | 100 | 385 | 61 | 0 % |
| 3.5 | 100 | 422 | 370 | 0 % |
| 3.7 | 100 | 382 | 681 | 1 % |
| 4.0 | 100 | 336 | 2 491 | 0 % |
| 4.2 | 100 | 377 | 1 640 | 0 % |
| 4.5 | 95 | 178 | 363 | 0 % |
| 4.7 | 54 | 41 | 23 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
