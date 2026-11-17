# Fase 2: carga con escrituras, 100 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 59 | 66 |
| Peticiones hechas | 1 313 | 1 387 |
| Peticiones correctas (2xx) | 712 | 817 |
| Errores (%) | 45.8 % | 41.1 % |
| Ritmo medio (peticiones/s) | 3.8 | 4.0 |
| Tiempo mediano (ms) | 426 | 282 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 96 | 95 |
| Tiempo con la CPU al tope (%) | 88 | 89 |
| CPU de PostgreSQL, media (%) | 3 | 4 |
| Memoria pico (MB) | 607 | 670 |
| Sentencias SQL | 38 240 | 22 247 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 110; fallidas: 66.
- Acciones hechas: panel 45, agenda 41, historia 34, reportes 8, recepcion 80, atencion 7, cobro 0, cancelar 7; sin trabajo disponible: 57.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 416 errores `OutOfMemoryError` en el registro.
- CPU del backend: 96 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 88 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 35 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 607 MB (30 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 1.1 con unos 72 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **59 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 30 | 30 | coincide |
| Mascotas | 30 | 30 | coincide |
| Citas agendadas | 30 | 30 | coincide |
| Diagnósticos | 3 | 3 | coincide |
| Recetas | 3 | 3 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 2 | 2 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 30 | 50 | 62.5 % | 27 800 | 30 001 |
| Registrar mascota | 30 | 0 | 0.0 % | 56 | 165 |
| Agendar cita | 30 | 0 | 0.0 % | 88 | 911 |
| Recepción (llegada) | 3 | 4 | 57.1 % | 30 000 | 30 001 |
| Iniciar atención | 3 | 4 | 57.1 % | 30 000 | 30 001 |
| Leer la consulta | 3 | 0 | 0.0 % | 43 | 89 |
| Registrar diagnóstico | 3 | 0 | 0.0 % | 101 | 103 |
| Registrar receta | 3 | 0 | 0.0 % | 116 | 503 |
| Actualizar mascota | 3 | 0 | 0.0 % | 98 | 194 |
| Cerrar la consulta | 0 | 3 | 100.0 % | 997 | 1 447 |
| Cancelar cita | 2 | 5 | 71.4 % | 19 228 | 30 001 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 258 | 203 (79 %) | 30 000 | 30 001 | 35 % |
| apoderado | 80 | 50 (62 %) | 27 800 | 30 001 | 8 % |
| privacidad | 117 | 39 (33 %) | 24 | 30 001 | 7 % |
| navegacion | 117 | 39 (33 %) | 27 | 30 001 | 6 % |
| cumplimiento | 113 | 36 (32 %) | 25 | 30 001 | 6 % |
| notificaciones | 110 | 33 (30 %) | 19 | 30 001 | 6 % |
| avisos_de_cumplimiento | 107 | 30 (28 %) | 24 | 30 001 | 5 % |
| empleados | 46 | 30 (65 %) | 30 000 | 30 001 | 5 % |
| citas_del_mes | 46 | 28 (61 %) | 25 379 | 30 001 | 4 % |
| propietarios | 41 | 25 (61 %) | 30 000 | 30 001 | 4 % |
| panel | 100 | 23 (23 %) | 243 | 30 001 | 4 % |
| lista_de_historias | 34 | 23 (68 %) | 30 000 | 30 001 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 28 | 857 | 0 % |
| 0.1 | 18 | 104 | 439 | 0 % |
| 0.3 | 31 | 175 | 352 | 0 % |
| 0.6 | 45 | 184 | 407 | 1 % |
| 0.8 | 59 | 222 | 1 196 | 1 % |
| 1.1 | 72 | 6 | 13 731 | 67 % |
| 1.3 | 86 | 23 | 30 001 | 100 % |
| 1.6 | 99 | 45 | 30 001 | 100 % |
| 1.8 | 100 | 23 | 30 001 | 100 % |
| 2.1 | 100 | 54 | 30 001 | 100 % |
| 2.3 | 100 | 27 | 30 001 | 100 % |
| 2.6 | 100 | 43 | 30 001 | 100 % |
| 2.8 | 100 | 35 | 30 001 | 100 % |
| 3.1 | 100 | 40 | 30 001 | 100 % |
| 3.3 | 100 | 34 | 30 001 | 100 % |
| 3.6 | 100 | 51 | 30 001 | 100 % |
| 3.8 | 100 | 29 | 30 001 | 100 % |
| 4.1 | 100 | 45 | 30 001 | 100 % |
| 4.3 | 100 | 31 | 30 001 | 100 % |
| 4.6 | 93 | 43 | 30 001 | 100 % |
| 4.8 | 72 | 28 | 30 001 | 100 % |
| 5.1 | 47 | 18 | 30 001 | 100 % |
| 5.3 | 34 | 17 | 30 001 | 100 % |
| 5.6 | 28 | 8 | 30 001 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 138; fallidas: 93.
- Acciones hechas: panel 45, agenda 44, historia 57, reportes 5, recepcion 106, atencion 12, cobro 0, cancelar 10; sin trabajo disponible: 73.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 306 errores `OutOfMemoryError` en el registro.
- CPU del backend: 95 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 89 % del tiempo.
- CPU de PostgreSQL: 4 % de media y 49 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 670 MB (33 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 1.5 con unos 93 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **66 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 38 | 38 | coincide |
| Mascotas | 38 | 38 | coincide |
| Citas agendadas | 38 | 38 | coincide |
| Diagnósticos | 3 | 3 | coincide |
| Recetas | 3 | 3 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 3 | 3 | coincide |
| Citas canceladas | 3 | 3 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 38 | 68 | 64.2 % | 30 000 | 30 001 |
| Registrar mascota | 38 | 0 | 0.0 % | 92 | 571 |
| Agendar cita | 38 | 0 | 0.0 % | 119 | 366 |
| Recepción (llegada) | 3 | 9 | 75.0 % | 30 000 | 30 001 |
| Iniciar atención | 3 | 9 | 75.0 % | 30 001 | 30 001 |
| Leer la consulta | 3 | 0 | 0.0 % | 47 | 150 |
| Registrar diagnóstico | 3 | 0 | 0.0 % | 93 | 93 |
| Registrar receta | 3 | 0 | 0.0 % | 99 | 112 |
| Actualizar mascota | 3 | 0 | 0.0 % | 64 | 72 |
| Cerrar la consulta | 3 | 0 | 0.0 % | 210 | 249 |
| Cancelar cita | 3 | 7 | 70.0 % | 30 000 | 30 000 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 236 | 175 (74 %) | 30 000 | 30 001 | 31 % |
| apoderado | 106 | 68 (64 %) | 30 000 | 30 001 | 12 % |
| privacidad | 116 | 35 (30 %) | 34 | 30 001 | 6 % |
| lista_de_historias | 57 | 35 (61 %) | 30 000 | 30 001 | 6 % |
| navegacion | 116 | 35 (30 %) | 38 | 30 001 | 6 % |
| cumplimiento | 111 | 30 (27 %) | 35 | 30 001 | 5 % |
| notificaciones | 109 | 28 (26 %) | 27 | 30 001 | 5 % |
| avisos_de_cumplimiento | 107 | 26 (24 %) | 31 | 30 001 | 5 % |
| empleados | 48 | 26 (54 %) | 30 000 | 30 001 | 5 % |
| panel | 106 | 25 (24 %) | 283 | 30 001 | 5 % |
| citas_del_mes | 48 | 25 (52 %) | 9 215 | 30 001 | 4 % |
| propietarios | 44 | 22 (50 %) | 15 426 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 11 | 79 | 668 | 0 % |
| 0.2 | 25 | 159 | 335 | 0 % |
| 0.5 | 39 | 167 | 424 | 0 % |
| 0.7 | 52 | 204 | 785 | 0 % |
| 1.0 | 66 | 207 | 1 378 | 0 % |
| 1.2 | 79 | 4 | 8 872 | 50 % |
| 1.5 | 93 | 16 | 30 001 | 100 % |
| 1.7 | 100 | 62 | 30 001 | 100 % |
| 2.0 | 100 | 21 | 30 001 | 100 % |
| 2.2 | 100 | 48 | 30 001 | 100 % |
| 2.5 | 100 | 36 | 30 001 | 100 % |
| 2.7 | 100 | 42 | 30 001 | 100 % |
| 3.0 | 100 | 35 | 30 001 | 100 % |
| 3.2 | 100 | 41 | 30 001 | 100 % |
| 3.5 | 100 | 41 | 30 001 | 100 % |
| 3.7 | 100 | 36 | 30 001 | 100 % |
| 4.0 | 100 | 39 | 30 001 | 100 % |
| 4.2 | 100 | 43 | 30 001 | 100 % |
| 4.5 | 98 | 32 | 30 001 | 100 % |
| 4.7 | 76 | 32 | 30 001 | 100 % |
| 5.0 | 56 | 24 | 30 001 | 100 % |
| 5.2 | 34 | 9 | 30 001 | 100 % |
| 5.5 | 21 | 10 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
