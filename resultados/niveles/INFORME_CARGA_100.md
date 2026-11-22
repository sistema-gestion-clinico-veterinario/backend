# Fase 3: 100 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | AGUANTA |
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 349 |
| Peticiones correctas (2xx) | 6 344 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.6 |
| Tiempo mediano (ms) | 16 |
| Tiempo p95 (ms) | 279 |
| CPU del backend, media (% del límite de 2 CPU) | 21 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 39 |
| Memoria pico (MB, de 4096) | 923 |
| Sentencias SQL | 175 312 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2194; fallidas: 5.
- Acciones hechas: panel 335, agenda 264, historia 187, reportes 29, recepcion 304, atencion 137, cobro 85, cancelar 62; sin trabajo disponible: 225.
- CPU del backend: 21 % de media y 58 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 39 % de media y 98 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 923 MB (23 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.4 s.
- Correo: el servidor de pruebas recibió 304 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.6 %).
- GC: 318 pausas, 18.6 s en total (6 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 457 MB (tope -Xmx2g).
- Clases cargadas: 25410 en total, 188 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 26 | 26 | 6160 |
| 0.1 | 50 | 51 | 11160 |
| 0.2 | 75 | 75 | 15249 |
| 0.2 | 81 | 82 | 16193 |
| 0.2 | 82 | 83 | 16366 |
| 0.3 | 87 | 88 | 17209 |
| 0.4 | 99 | 100 | 19110 |
| 1.1 | 127 | 130 | 24350 |
| 1.8 | 131 | 133 | 24805 |
| 2.3 | 132 | 135 | 24977 |
| 2.9 | 133 | 136 | 25249 |
| 3.4 | 133 | 136 | 25303 |
| 4.1 | 134 | 137 | 25322 |
| 4.6 | 134 | 137 | 25359 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 183 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 188 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 304 | 304 | coincide |
| Mascotas | 304 | 304 | coincide |
| Citas agendadas | 300 | 300 | coincide |
| Diagnósticos | 136 | 136 | coincide |
| Recetas | 136 | 136 | coincide |
| Pagos | 85 | 85 | coincide |
| Citas completadas | 136 | 136 | coincide |
| Citas canceladas | 62 | 62 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 304 | 0 | 0.0 % | 37 | 108 |
| Registrar mascota | 304 | 0 | 0.0 % | 21 | 43 |
| Agendar cita | 300 | 4 | 1.3 % | 28 | 62 |
| Recepción (llegada) | 137 | 0 | 0.0 % | 12 | 38 |
| Iniciar atención | 136 | 1 | 0.7 % | 14 | 42 |
| Leer la consulta | 136 | 0 | 0.0 % | 7 | 32 |
| Registrar diagnóstico | 136 | 0 | 0.0 % | 6 | 30 |
| Registrar receta | 136 | 0 | 0.0 % | 7 | 41 |
| Actualizar mascota | 136 | 0 | 0.0 % | 8 | 28 |
| Cerrar la consulta | 136 | 0 | 0.0 % | 13 | 63 |
| Caja: crear punto de cobro | 62 | 0 | 0.0 % | 10 | 74 |
| Caja: vincular equipo | 62 | 0 | 0.0 % | 9 | 23 |
| Caja: abrir la caja | 62 | 0 | 0.0 % | 9 | 28 |
| Cobrar | 85 | 0 | 0.0 % | 11 | 41 |
| Cancelar cita | 62 | 0 | 0.0 % | 18 | 113 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 435 | 0 (0 %) | 274 | 748 | 42 % |
| reporte_clinico | 29 | 0 (0 %) | 1 471 | 2 270 | 13 % |
| login | 100 | 0 (0 %) | 278 | 563 | 9 % |
| propietarios | 264 | 0 (0 %) | 30 | 134 | 4 % |
| apoderado | 304 | 0 (0 %) | 37 | 108 | 4 % |
| citas_del_mes | 264 | 0 (0 %) | 31 | 104 | 3 % |
| cita | 304 | 4 (1 %) | 28 | 62 | 3 % |
| lista_de_historias | 187 | 0 (0 %) | 26 | 96 | 2 % |
| mascota | 304 | 0 (0 %) | 21 | 43 | 2 % |
| navegacion | 435 | 0 (0 %) | 9 | 29 | 2 % |
| cumplimiento | 435 | 0 (0 %) | 7 | 28 | 2 % |
| empleados | 264 | 0 (0 %) | 12 | 38 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 90 | 303 | 0 % |
| 0.2 | 26 | 147 | 278 | 0 % |
| 0.5 | 39 | 187 | 303 | 0 % |
| 0.7 | 53 | 239 | 269 | 0 % |
| 1.0 | 67 | 306 | 351 | 0 % |
| 1.2 | 80 | 367 | 324 | 0 % |
| 1.5 | 94 | 431 | 328 | 0 % |
| 1.7 | 100 | 408 | 311 | 0 % |
| 2.0 | 100 | 413 | 263 | 0 % |
| 2.2 | 100 | 412 | 237 | 0 % |
| 2.5 | 100 | 421 | 238 | 0 % |
| 2.7 | 100 | 395 | 396 | 0 % |
| 3.0 | 100 | 382 | 234 | 0 % |
| 3.2 | 100 | 419 | 249 | 0 % |
| 3.5 | 100 | 409 | 244 | 0 % |
| 3.7 | 100 | 357 | 264 | 0 % |
| 4.0 | 100 | 418 | 354 | 0 % |
| 4.2 | 100 | 336 | 345 | 0 % |
| 4.5 | 85 | 188 | 264 | 1 % |
| 4.7 | 43 | 24 | 267 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
