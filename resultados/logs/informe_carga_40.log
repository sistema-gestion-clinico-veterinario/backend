# Fase 2: carga con escrituras, 40 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 40 | 40 |
| Peticiones hechas | 777 | 948 |
| Peticiones correctas (2xx) | 508 | 727 |
| Errores (%) | 34.6 % | 23.3 % |
| Ritmo medio (peticiones/s) | 2.7 | 3.3 |
| Tiempo mediano (ms) | 287 | 54 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 94 | 88 |
| Tiempo con la CPU al tope (%) | 85 | 75 |
| CPU de PostgreSQL, media (%) | 3 | 3 |
| Memoria pico (MB) | 631 | 664 |
| Sentencias SQL | 32 323 | 20 042 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 68; fallidas: 55.
- Acciones hechas: panel 31, agenda 41, historia 20, reportes 2, recepcion 56, atencion 7, cobro 0, cancelar 5; sin trabajo disponible: 38.
- Java agotó su memoria de trabajo (heap): 377 errores `OutOfMemoryError` en el registro.
- CPU del backend: 94 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 85 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 32 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 631 MB (31 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.9 con unos 40 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **40 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 22 | 22 | coincide |
| Mascotas | 21 | 21 | coincide |
| Citas agendadas | 16 | 16 | coincide |
| Diagnósticos | 1 | 1 | coincide |
| Recetas | 1 | 1 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 1 | 1 | coincide |
| Citas canceladas | 2 | 2 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 22 | 34 | 60.7 % | 15 275 | 30 001 |
| Registrar mascota | 21 | 1 | 4.5 % | 76 | 2 570 |
| Agendar cita | 16 | 5 | 23.8 % | 94 | 26 864 |
| Recepción (llegada) | 1 | 6 | 85.7 % | 30 000 | 30 001 |
| Iniciar atención | 1 | 6 | 85.7 % | 18 462 | 30 001 |
| Leer la consulta | 1 | 0 | 0.0 % | 31 | 31 |
| Registrar diagnóstico | 1 | 0 | 0.0 % | 95 | 95 |
| Registrar receta | 1 | 0 | 0.0 % | 84 | 84 |
| Actualizar mascota | 1 | 0 | 0.0 % | 39 | 39 |
| Cerrar la consulta | 1 | 0 | 0.0 % | 243 | 243 |
| Cancelar cita | 2 | 3 | 60.0 % | 1 292 | 30 000 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| apoderado | 56 | 34 (61 %) | 15 275 | 30 001 | 14 % |
| propietarios | 41 | 26 (63 %) | 15 522 | 30 001 | 10 % |
| empleados | 41 | 24 (59 %) | 11 920 | 30 001 | 9 % |
| privacidad | 77 | 19 (25 %) | 29 | 30 000 | 8 % |
| citas_del_mes | 41 | 23 (56 %) | 8 695 | 30 000 | 8 % |
| navegacion | 77 | 19 (25 %) | 33 | 30 001 | 8 % |
| cumplimiento | 76 | 20 (26 %) | 28 | 30 001 | 8 % |
| notificaciones | 76 | 21 (28 %) | 24 | 30 001 | 7 % |
| panel | 71 | 22 (31 %) | 270 | 30 001 | 7 % |
| avisos_de_cumplimiento | 73 | 21 (29 %) | 27 | 30 000 | 7 % |
| lista_de_historias | 20 | 12 (60 %) | 3 586 | 30 001 | 4 % |
| checkin | 7 | 6 (86 %) | 30 000 | 30 001 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 41 | 650 | 0 % |
| 0.1 | 18 | 99 | 359 | 0 % |
| 0.4 | 30 | 145 | 433 | 0 % |
| 0.6 | 40 | 159 | 1 382 | 0 % |
| 0.9 | 40 | 66 | 7 129 | 8 % |
| 1.1 | 40 | 15 | 19 698 | 87 % |
| 1.4 | 40 | 27 | 30 001 | 96 % |
| 1.6 | 40 | 16 | 19 447 | 100 % |
| 1.9 | 40 | 16 | 30 001 | 100 % |
| 2.1 | 40 | 21 | 30 001 | 100 % |
| 2.4 | 40 | 19 | 30 001 | 100 % |
| 2.6 | 40 | 19 | 30 001 | 100 % |
| 2.9 | 40 | 19 | 30 001 | 100 % |
| 3.1 | 40 | 22 | 30 001 | 100 % |
| 3.4 | 40 | 12 | 30 001 | 100 % |
| 3.6 | 37 | 25 | 30 001 | 100 % |
| 3.9 | 36 | 16 | 30 001 | 100 % |
| 4.1 | 30 | 19 | 30 001 | 100 % |
| 4.4 | 20 | 15 | 30 000 | 100 % |
| 4.6 | 8 | 6 | 24 827 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 190; fallidas: 43.
- Acciones hechas: panel 37, agenda 40, historia 19, reportes 3, recepcion 61, atencion 17, cobro 6, cancelar 7; sin trabajo disponible: 46.
- Java agotó su memoria de trabajo (heap): 303 errores `OutOfMemoryError` en el registro.
- CPU del backend: 88 % de media y 100 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 75 % del tiempo.
- CPU de PostgreSQL: 3 % de media y 41 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 664 MB (32 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 1.4 con unos 40 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **40 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 39 | 39 | coincide |
| Mascotas | 38 | 38 | coincide |
| Citas agendadas | 38 | 38 | coincide |
| Diagnósticos | 9 | 9 | coincide |
| Recetas | 9 | 9 | coincide |
| Pagos | 1 | 1 | coincide |
| Citas completadas | 9 | 9 | coincide |
| Citas canceladas | 7 | 7 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 39 | 22 | 36.1 % | 91 | 30 001 |
| Registrar mascota | 38 | 1 | 2.6 % | 44 | 191 |
| Agendar cita | 38 | 0 | 0.0 % | 55 | 117 |
| Recepción (llegada) | 10 | 7 | 41.2 % | 60 | 30 001 |
| Iniciar atención | 9 | 8 | 47.1 % | 85 | 30 001 |
| Leer la consulta | 9 | 0 | 0.0 % | 35 | 526 |
| Registrar diagnóstico | 9 | 0 | 0.0 % | 37 | 110 |
| Registrar receta | 9 | 0 | 0.0 % | 35 | 96 |
| Actualizar mascota | 9 | 0 | 0.0 % | 35 | 62 |
| Cerrar la consulta | 9 | 0 | 0.0 % | 59 | 522 |
| Caja: crear punto de cobro | 1 | 5 | 83.3 % | 30 001 | 30 001 |
| Caja: vincular equipo | 1 | 0 | 0.0 % | 66 | 66 |
| Caja: abrir la caja | 1 | 0 | 0.0 % | 96 | 96 |
| Cobrar | 1 | 0 | 0.0 % | 92 | 92 |
| Cancelar cita | 7 | 0 | 0.0 % | 39 | 5 439 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| apoderado | 61 | 22 (36 %) | 91 | 30 001 | 11 % |
| propietarios | 40 | 24 (60 %) | 18 679 | 30 001 | 11 % |
| empleados | 40 | 22 (55 %) | 15 087 | 30 001 | 10 % |
| privacidad | 85 | 16 (19 %) | 21 | 30 001 | 10 % |
| cumplimiento | 84 | 19 (23 %) | 22 | 30 001 | 9 % |
| citas_del_mes | 40 | 22 (55 %) | 3 145 | 30 001 | 9 % |
| navegacion | 85 | 19 (22 %) | 26 | 30 001 | 8 % |
| notificaciones | 82 | 17 (21 %) | 17 | 30 001 | 8 % |
| avisos_de_cumplimiento | 79 | 14 (18 %) | 19 | 30 001 | 6 % |
| panel | 77 | 12 (16 %) | 259 | 27 622 | 4 % |
| checkin | 17 | 7 (41 %) | 60 | 30 001 | 3 % |
| iniciar | 17 | 8 (47 %) | 85 | 30 001 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 49 | 560 | 0 % |
| 0.1 | 19 | 96 | 347 | 0 % |
| 0.4 | 31 | 162 | 400 | 1 % |
| 0.7 | 40 | 172 | 343 | 0 % |
| 0.9 | 40 | 164 | 244 | 0 % |
| 1.1 | 40 | 81 | 1 779 | 1 % |
| 1.4 | 40 | 11 | 17 353 | 91 % |
| 1.6 | 40 | 15 | 30 000 | 93 % |
| 1.9 | 40 | 24 | 30 001 | 92 % |
| 2.1 | 40 | 10 | 30 001 | 100 % |
| 2.4 | 40 | 24 | 30 001 | 96 % |
| 2.6 | 40 | 15 | 30 001 | 100 % |
| 2.9 | 40 | 22 | 30 001 | 100 % |
| 3.1 | 40 | 7 | 30 005 | 100 % |
| 3.4 | 40 | 28 | 30 001 | 100 % |
| 3.6 | 40 | 12 | 30 001 | 100 % |
| 3.9 | 32 | 22 | 30 001 | 100 % |
| 4.2 | 27 | 14 | 30 001 | 100 % |
| 4.4 | 18 | 17 | 30 000 | 100 % |
| 4.7 | 7 | 3 | 11 878 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
