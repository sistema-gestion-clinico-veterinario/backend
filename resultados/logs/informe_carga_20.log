# Fase 2: carga con escrituras, 20 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 20 | 20 |
| Peticiones hechas | 720 | 827 |
| Peticiones correctas (2xx) | 661 | 706 |
| Errores (%) | 8.2 % | 14.6 % |
| Ritmo medio (peticiones/s) | 2.7 | 3.6 |
| Tiempo mediano (ms) | 27 | 30 |
| Tiempo p95 (ms) | 30 000 | 7 744 |
| CPU del backend, media (%) | 69 | 59 |
| Tiempo con la CPU al tope (%) | 55 | 45 |
| CPU de PostgreSQL, media (%) | 2 | 5 |
| Memoria pico (MB) | 625 | 655 |
| Sentencias SQL | 34 994 | 20 097 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 275; fallidas: 13.
- Acciones hechas: panel 20, agenda 28, historia 25, reportes 1, recepcion 41, atencion 22, cobro 9, cancelar 3; sin trabajo disponible: 28.
- Java agotó su memoria de trabajo (heap): 65 errores `OutOfMemoryError` en el registro.
- CPU del backend: 69 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 55 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 16 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 625 MB (30 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 2.4 con unos 20 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **20 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 40 | 40 | coincide |
| Mascotas | 40 | 40 | coincide |
| Citas agendadas | 39 | 39 | coincide |
| Diagnósticos | 18 | 18 | coincide |
| Recetas | 18 | 18 | coincide |
| Pagos | 7 | 7 | coincide |
| Citas completadas | 18 | 18 | coincide |
| Citas canceladas | 2 | 2 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 40 | 1 | 2.4 % | 49 | 204 |
| Registrar mascota | 40 | 0 | 0.0 % | 26 | 188 |
| Agendar cita | 39 | 1 | 2.5 % | 40 | 293 |
| Recepción (llegada) | 18 | 4 | 18.2 % | 22 | 30 001 |
| Iniciar atención | 18 | 4 | 18.2 % | 27 | 30 001 |
| Leer la consulta | 18 | 0 | 0.0 % | 19 | 34 |
| Registrar diagnóstico | 18 | 0 | 0.0 % | 16 | 54 |
| Registrar receta | 18 | 0 | 0.0 % | 19 | 45 |
| Actualizar mascota | 18 | 0 | 0.0 % | 18 | 25 |
| Cerrar la consulta | 18 | 0 | 0.0 % | 32 | 134 |
| Caja: crear punto de cobro | 7 | 2 | 22.2 % | 20 | 30 001 |
| Caja: vincular equipo | 7 | 0 | 0.0 % | 19 | 27 |
| Caja: abrir la caja | 7 | 0 | 0.0 % | 17 | 40 |
| Cobrar | 7 | 0 | 0.0 % | 23 | 46 |
| Cancelar cita | 2 | 1 | 33.3 % | 32 | 30 001 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| citas_del_mes | 28 | 7 (25 %) | 117 | 30 001 | 12 % |
| propietarios | 28 | 7 (25 %) | 87 | 30 001 | 12 % |
| empleados | 28 | 7 (25 %) | 40 | 30 001 | 11 % |
| navegacion | 46 | 6 (13 %) | 23 | 30 001 | 10 % |
| cumplimiento | 46 | 6 (13 %) | 18 | 30 001 | 10 % |
| lista_de_historias | 25 | 5 (20 %) | 42 | 30 001 | 8 % |
| iniciar | 22 | 4 (18 %) | 27 | 30 001 | 7 % |
| checkin | 22 | 4 (18 %) | 22 | 30 001 | 7 % |
| notificaciones | 43 | 3 (7 %) | 15 | 30 000 | 5 % |
| punto_cobro | 9 | 2 (22 %) | 20 | 30 001 | 3 % |
| historia | 20 | 1 (5 %) | 41 | 30 000 | 2 % |
| apoderado | 41 | 1 (2 %) | 49 | 204 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 28 | 554 | 0 % |
| 0.1 | 13 | 87 | 337 | 0 % |
| 0.3 | 20 | 76 | 330 | 0 % |
| 0.6 | 20 | 80 | 148 | 0 % |
| 0.8 | 20 | 107 | 122 | 0 % |
| 1.1 | 20 | 60 | 72 | 0 % |
| 1.4 | 20 | 96 | 81 | 0 % |
| 1.6 | 20 | 78 | 77 | 0 % |
| 1.9 | 20 | 43 | 1 543 | 0 % |
| 2.4 | 20 | 9 | 30 001 | 100 % |
| 2.6 | 20 | 11 | 30 001 | 91 % |
| 2.9 | 20 | 7 | 30 001 | 71 % |
| 3.1 | 20 | 14 | 30 001 | 86 % |
| 3.4 | 19 | 7 | 30 001 | 100 % |
| 3.6 | 17 | 9 | 30 001 | 100 % |
| 3.9 | 14 | 5 | 30 001 | 80 % |
| 4.1 | 7 | 1 | 30 001 | 100 % |
| 4.3 | 3 | 2 | 30 001 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 210; fallidas: 16.
- Acciones hechas: panel 49, agenda 37, historia 19, reportes 3, recepcion 42, atencion 13, cobro 8, cancelar 12; sin trabajo disponible: 34.
- Java agotó su memoria de trabajo (heap): 158 errores `OutOfMemoryError` en el registro.
- CPU del backend: 59 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 45 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 36 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 655 MB (32 % de 2048 MB).
- Los fallos fueron: 99 % sin respuesta (tiempo agotado), 1 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 2.2 con unos 20 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **20 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 17.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 39 | 39 | coincide |
| Mascotas | 37 | 37 | coincide |
| Citas agendadas | 37 | 37 | coincide |
| Diagnósticos | 10 | 10 | coincide |
| Recetas | 10 | 10 | coincide |
| Pagos | 4 | 4 | coincide |
| Citas completadas | 10 | 10 | coincide |
| Citas canceladas | 10 | 10 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 39 | 3 | 7.1 % | 54 | 5 477 |
| Registrar mascota | 37 | 2 | 5.1 % | 28 | 8 667 |
| Agendar cita | 37 | 0 | 0.0 % | 41 | 271 |
| Recepción (llegada) | 11 | 2 | 15.4 % | 22 | 7 040 |
| Iniciar atención | 10 | 3 | 23.1 % | 31 | 6 876 |
| Leer la consulta | 10 | 0 | 0.0 % | 16 | 40 |
| Registrar diagnóstico | 10 | 0 | 0.0 % | 15 | 38 |
| Registrar receta | 10 | 0 | 0.0 % | 18 | 44 |
| Actualizar mascota | 10 | 0 | 0.0 % | 18 | 28 |
| Cerrar la consulta | 10 | 0 | 0.0 % | 35 | 121 |
| Caja: crear punto de cobro | 4 | 4 | 50.0 % | 2 010 | 8 778 |
| Caja: vincular equipo | 4 | 0 | 0.0 % | 31 | 58 |
| Caja: abrir la caja | 4 | 0 | 0.0 % | 27 | 58 |
| Cobrar | 4 | 0 | 0.0 % | 45 | 50 |
| Cancelar cita | 10 | 2 | 16.7 % | 30 | 10 410 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| privacidad | 69 | 5 (7 %) | 19 | 13 508 | 13 % |
| propietarios | 37 | 11 (30 %) | 70 | 11 762 | 10 % |
| navegacion | 69 | 11 (16 %) | 21 | 8 001 | 10 % |
| citas_del_mes | 37 | 10 (27 %) | 44 | 10 033 | 9 % |
| cumplimiento | 69 | 11 (16 %) | 20 | 7 555 | 8 % |
| notificaciones | 69 | 11 (16 %) | 16 | 7 364 | 7 % |
| empleados | 37 | 10 (27 %) | 30 | 8 661 | 7 % |
| panel | 69 | 11 (16 %) | 240 | 4 385 | 6 % |
| avisos_de_cumplimiento | 69 | 11 (16 %) | 19 | 6 189 | 6 % |
| lista_de_historias | 19 | 5 (26 %) | 43 | 9 150 | 4 % |
| mascota | 39 | 2 (5 %) | 28 | 8 667 | 3 % |
| apoderado | 42 | 3 (7 %) | 54 | 5 477 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 55 | 557 | 0 % |
| 0.2 | 17 | 108 | 320 | 0 % |
| 0.5 | 20 | 92 | 287 | 0 % |
| 0.7 | 20 | 57 | 244 | 0 % |
| 0.9 | 20 | 69 | 53 | 0 % |
| 1.2 | 20 | 75 | 253 | 0 % |
| 1.4 | 20 | 108 | 233 | 1 % |
| 1.7 | 20 | 87 | 51 | 0 % |
| 1.9 | 20 | 50 | 1 370 | 2 % |
| 2.2 | 20 | 11 | 9 717 | 91 % |
| 2.5 | 20 | 25 | 17 050 | 92 % |
| 2.7 | 20 | 22 | 11 762 | 86 % |
| 3.0 | 20 | 26 | 12 651 | 100 % |
| 3.2 | 19 | 27 | 13 728 | 96 % |
| 3.5 | 13 | 15 | 8 313 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
