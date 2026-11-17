# Fase 2: carga con escrituras, 60 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 34 | 38 |
| Peticiones hechas | 871 | 1 010 |
| Peticiones correctas (2xx) | 486 | 671 |
| Errores (%) | 44.2 % | 33.6 % |
| Ritmo medio (peticiones/s) | 2.8 | 3.3 |
| Tiempo mediano (ms) | 333 | 303 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 98 | 96 |
| Tiempo con la CPU al tope (%) | 89 | 88 |
| CPU de PostgreSQL, media (%) | 4 | 5 |
| Memoria pico (MB) | 619 | 1 106 |
| Sentencias SQL | 28 552 | 17 999 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 69; fallidas: 82.
- Acciones hechas: panel 23, agenda 31, historia 36, reportes 3, recepcion 75, atencion 11, cobro 0, cancelar 6; sin trabajo disponible: 48.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 390 errores `OutOfMemoryError` en el registro.
- CPU del backend: 98 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 89 % del tiempo.
- CPU de PostgreSQL: 4 % de media y 63 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 619 MB (30 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.9 con unos 59 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **34 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 19 | 19 | coincide |
| Mascotas | 19 | 19 | coincide |
| Citas agendadas | 19 | 19 | coincide |
| Diagnósticos | 2 | 2 | coincide |
| Recetas | 2 | 2 | coincide |
| Pagos | 0 | 0 | coincide |
| Citas completadas | 0 | 0 | coincide |
| Citas canceladas | 0 | 0 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 19 | 56 | 74.7 % | 23 805 | 30 001 |
| Registrar mascota | 19 | 0 | 0.0 % | 65 | 215 |
| Agendar cita | 19 | 0 | 0.0 % | 91 | 376 |
| Recepción (llegada) | 2 | 9 | 81.8 % | 30 001 | 30 001 |
| Iniciar atención | 2 | 9 | 81.8 % | 5 021 | 30 001 |
| Leer la consulta | 2 | 0 | 0.0 % | 39 | 44 |
| Registrar diagnóstico | 2 | 0 | 0.0 % | 118 | 127 |
| Registrar receta | 2 | 0 | 0.0 % | 148 | 175 |
| Actualizar mascota | 2 | 0 | 0.0 % | 105 | 135 |
| Cerrar la consulta | 0 | 2 | 100.0 % | 1 166 | 1 176 |
| Cancelar cita | 0 | 6 | 100.0 % | 18 293 | 30 001 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 127 | 85 (67 %) | 16 357 | 30 001 | 23 % |
| apoderado | 75 | 56 (75 %) | 23 805 | 30 001 | 15 % |
| citas_del_mes | 35 | 25 (71 %) | 19 082 | 30 001 | 7 % |
| empleados | 35 | 25 (71 %) | 16 154 | 30 001 | 6 % |
| navegacion | 75 | 23 (31 %) | 28 | 30 001 | 6 % |
| lista_de_historias | 36 | 25 (69 %) | 13 995 | 30 001 | 6 % |
| privacidad | 75 | 23 (31 %) | 27 | 30 001 | 6 % |
| propietarios | 31 | 21 (68 %) | 24 697 | 30 001 | 6 % |
| notificaciones | 69 | 17 (25 %) | 22 | 30 001 | 4 % |
| panel | 65 | 13 (20 %) | 260 | 30 001 | 4 % |
| cumplimiento | 73 | 21 (29 %) | 30 | 30 000 | 4 % |
| avisos_de_cumplimiento | 68 | 16 (24 %) | 25 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 8 | 56 | 725 | 0 % |
| 0.2 | 21 | 129 | 376 | 0 % |
| 0.4 | 34 | 151 | 331 | 0 % |
| 0.7 | 47 | 150 | 1 148 | 1 % |
| 0.9 | 59 | 8 | 15 420 | 88 % |
| 1.2 | 60 | 17 | 30 001 | 94 % |
| 1.4 | 60 | 42 | 30 001 | 100 % |
| 1.7 | 60 | 29 | 30 001 | 100 % |
| 1.9 | 60 | 29 | 30 001 | 100 % |
| 2.2 | 60 | 33 | 30 001 | 100 % |
| 2.4 | 60 | 36 | 30 001 | 100 % |
| 2.7 | 60 | 19 | 30 001 | 100 % |
| 2.9 | 60 | 27 | 30 001 | 100 % |
| 3.2 | 60 | 25 | 30 001 | 100 % |
| 3.4 | 60 | 23 | 30 001 | 100 % |
| 3.7 | 60 | 29 | 30 001 | 100 % |
| 3.9 | 56 | 23 | 30 001 | 100 % |
| 4.2 | 42 | 19 | 30 009 | 100 % |
| 4.4 | 27 | 13 | 30 001 | 100 % |
| 4.7 | 21 | 13 | 30 001 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 108; fallidas: 58.
- Acciones hechas: panel 36, agenda 33, historia 28, reportes 11, recepcion 62, atencion 14, cobro 0, cancelar 2; sin trabajo disponible: 37.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 399 errores `OutOfMemoryError` en el registro.
- CPU del backend: 96 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 88 % del tiempo.
- CPU de PostgreSQL: 5 % de media y 69 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 106 MB (54 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 1.2 con unos 60 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **38 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 32 | 32 | coincide |
| Mascotas | 32 | 32 | coincide |
| Citas agendadas | 32 | 32 | coincide |
| Diagnósticos | 2 | 2 | coincide |
| Recetas | 1 | 1 | coincide |
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
| Registrar propietario | 32 | 30 | 48.4 % | 2 311 | 30 001 |
| Registrar mascota | 32 | 0 | 0.0 % | 92 | 2 000 |
| Agendar cita | 32 | 0 | 0.0 % | 128 | 1 020 |
| Recepción (llegada) | 2 | 12 | 85.7 % | 27 314 | 30 001 |
| Iniciar atención | 2 | 12 | 85.7 % | 30 000 | 30 001 |
| Leer la consulta | 2 | 0 | 0.0 % | 198 | 246 |
| Registrar diagnóstico | 2 | 0 | 0.0 % | 1 235 | 1 662 |
| Registrar receta | 1 | 1 | 50.0 % | 15 971 | 30 001 |
| Actualizar mascota | 1 | 1 | 50.0 % | 15 266 | 30 001 |
| Cerrar la consulta | 0 | 2 | 100.0 % | 16 977 | 30 000 |
| Cancelar cita | 2 | 0 | 0.0 % | 456 | 843 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 84 | 32 (38 %) | 1 875 | 30 001 | 10 % |
| privacidad | 103 | 31 (30 %) | 66 | 30 001 | 9 % |
| apoderado | 62 | 30 (48 %) | 2 311 | 30 001 | 9 % |
| cumplimiento | 99 | 28 (28 %) | 59 | 30 001 | 9 % |
| navegacion | 103 | 31 (30 %) | 74 | 30 001 | 8 % |
| notificaciones | 93 | 24 (26 %) | 33 | 30 001 | 7 % |
| panel | 88 | 21 (24 %) | 303 | 30 001 | 7 % |
| empleados | 35 | 19 (54 %) | 30 000 | 30 001 | 6 % |
| avisos_de_cumplimiento | 88 | 20 (23 %) | 30 | 30 001 | 6 % |
| propietarios | 33 | 18 (55 %) | 30 000 | 30 001 | 5 % |
| citas_del_mes | 35 | 19 (54 %) | 5 482 | 30 001 | 5 % |
| lista_de_historias | 28 | 17 (61 %) | 30 000 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 90 | 594 | 0 % |
| 0.2 | 25 | 127 | 306 | 0 % |
| 0.5 | 38 | 185 | 356 | 0 % |
| 0.8 | 51 | 181 | 2 336 | 0 % |
| 1.0 | 60 | 86 | 5 804 | 3 % |
| 1.2 | 60 | 11 | 30 001 | 82 % |
| 1.5 | 60 | 37 | 30 001 | 92 % |
| 1.8 | 60 | 22 | 30 001 | 100 % |
| 2.0 | 60 | 24 | 30 001 | 100 % |
| 2.2 | 60 | 28 | 30 001 | 100 % |
| 2.5 | 60 | 22 | 30 001 | 100 % |
| 2.8 | 60 | 24 | 30 001 | 100 % |
| 3.0 | 60 | 27 | 30 001 | 100 % |
| 3.2 | 60 | 19 | 30 001 | 100 % |
| 3.5 | 60 | 29 | 30 001 | 100 % |
| 3.8 | 60 | 26 | 30 001 | 100 % |
| 4.0 | 59 | 22 | 30 001 | 100 % |
| 4.2 | 48 | 25 | 30 001 | 100 % |
| 4.5 | 38 | 13 | 30 001 | 100 % |
| 4.8 | 25 | 11 | 30 001 | 100 % |
| 5.0 | 8 | 1 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
