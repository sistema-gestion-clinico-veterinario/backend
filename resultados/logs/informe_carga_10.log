# Fase 2: carga con escrituras, 10 usuarios simultáneos

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   5b27bbabb33224241586674bb2401e47bb98f92f
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 10 | 10 |
| Peticiones hechas | 407 | 414 |
| Peticiones correctas (2xx) | 384 | 393 |
| Errores (%) | 5.7 % | 5.1 % |
| Ritmo medio (peticiones/s) | 1.5 | 1.5 |
| Tiempo mediano (ms) | 40 | 36 |
| Tiempo p95 (ms) | 22 971 | 2 378 |
| CPU del backend, media (%) | 57 | 59 |
| Tiempo con la CPU al tope (%) | 45 | 49 |
| CPU de PostgreSQL, media (%) | 2 | 1 |
| Memoria pico (MB) | 627 | 755 |
| Sentencias SQL | 21 751 | 10 730 |
| Reinicios del contenedor | 0 | 0 |

## ANTES: FALLA; datos CONSISTENTES

- Escrituras correctas: 129; fallidas: 7.
- Acciones hechas: panel 18, agenda 13, historia 14, reportes 2, recepcion 24, atencion 8, cobro 4, cancelar 2; sin trabajo disponible: 16.
- Java agotó su memoria de trabajo (heap): 24 errores `OutOfMemoryError` en el registro.
- CPU del backend: 57 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 45 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 19 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 627 MB (31 % de 2048 MB).
- Los fallos fueron: 91 % sin respuesta (tiempo agotado), 9 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 3.0 con unos 10 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 24 | 24 | coincide |
| Mascotas | 24 | 24 | coincide |
| Citas agendadas | 21 | 21 | coincide |
| Diagnósticos | 6 | 6 | coincide |
| Recetas | 6 | 6 | coincide |
| Pagos | 4 | 4 | coincide |
| Citas completadas | 6 | 6 | coincide |
| Citas canceladas | 2 | 2 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 24 | 0 | 0.0 % | 65 | 169 |
| Registrar mascota | 24 | 0 | 0.0 % | 38 | 517 |
| Agendar cita | 21 | 3 | 12.5 % | 57 | 567 |
| Recepción (llegada) | 6 | 2 | 25.0 % | 43 | 30 001 |
| Iniciar atención | 6 | 2 | 25.0 % | 44 | 30 000 |
| Leer la consulta | 6 | 0 | 0.0 % | 24 | 28 |
| Registrar diagnóstico | 6 | 0 | 0.0 % | 30 | 46 |
| Registrar receta | 6 | 0 | 0.0 % | 34 | 58 |
| Actualizar mascota | 6 | 0 | 0.0 % | 31 | 59 |
| Cerrar la consulta | 6 | 0 | 0.0 % | 56 | 143 |
| Caja: crear punto de cobro | 4 | 0 | 0.0 % | 37 | 49 |
| Caja: vincular equipo | 4 | 0 | 0.0 % | 24 | 43 |
| Caja: abrir la caja | 4 | 0 | 0.0 % | 25 | 63 |
| Cobrar | 4 | 0 | 0.0 % | 52 | 269 |
| Cancelar cita | 2 | 0 | 0.0 % | 49 | 55 |

**Endpoints que más tiempo acumulan, ANTES:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| cumplimiento | 31 | 3 (10 %) | 24 | 30 001 | 13 % |
| lista_de_historias | 14 | 3 (21 %) | 50 | 30 001 | 13 % |
| navegacion | 31 | 2 (6 %) | 28 | 30 001 | 9 % |
| notificaciones | 30 | 2 (7 %) | 21 | 30 000 | 9 % |
| iniciar | 8 | 2 (25 %) | 44 | 30 000 | 9 % |
| checkin | 8 | 2 (25 %) | 43 | 30 001 | 9 % |
| reporte_clinico | 2 | 1 (50 %) | 16 615 | 30 001 | 5 % |
| cita | 24 | 3 (12 %) | 57 | 567 | 5 % |
| propietarios | 13 | 1 (8 %) | 86 | 30 001 | 5 % |
| avisos_de_cumplimiento | 29 | 1 (3 %) | 23 | 65 | 5 % |
| empleados | 13 | 1 (8 %) | 47 | 30 001 | 4 % |
| reporte_de_ventas | 2 | 1 (50 %) | 15 153 | 30 001 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 35 | 527 | 0 % |
| 0.3 | 10 | 56 | 322 | 2 % |
| 0.5 | 10 | 33 | 177 | 0 % |
| 0.8 | 10 | 43 | 224 | 2 % |
| 1.0 | 10 | 41 | 280 | 0 % |
| 1.3 | 10 | 31 | 517 | 0 % |
| 1.5 | 10 | 51 | 214 | 0 % |
| 1.8 | 10 | 38 | 79 | 0 % |
| 2.0 | 10 | 30 | 605 | 0 % |
| 2.3 | 10 | 24 | 1 393 | 0 % |
| 2.5 | 10 | 2 | 6 571 | 0 % |
| 2.8 | 10 | 2 | 30 001 | 100 % |
| 3.0 | 10 | 10 | 30 001 | 80 % |
| 3.5 | 8 | 7 | 30 001 | 100 % |
| 3.8 | 7 | 1 | 30 001 | 100 % |
| 4.0 | 5 | 3 | 30 001 | 100 % |

</details>

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 133; fallidas: 9.
- Acciones hechas: panel 17, agenda 13, historia 18, reportes 3, recepcion 24, atencion 9, cobro 2, cancelar 5; sin trabajo disponible: 14.
- Java agotó su memoria de trabajo (heap): 21 errores `OutOfMemoryError` en el registro.
- CPU del backend: 59 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 49 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 17 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 755 MB (37 % de 2048 MB).
- Los fallos fueron: 90 % sin respuesta (tiempo agotado), 10 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 10 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **10 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 21 | 21 | coincide |
| Mascotas | 21 | 21 | coincide |
| Citas agendadas | 19 | 19 | coincide |
| Diagnósticos | 9 | 9 | coincide |
| Recetas | 8 | 8 | coincide |
| Pagos | 2 | 2 | coincide |
| Citas completadas | 8 | 8 | coincide |
| Citas canceladas | 4 | 4 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 21 | 3 | 12.5 % | 69 | 30 001 |
| Registrar mascota | 21 | 0 | 0.0 % | 37 | 76 |
| Agendar cita | 19 | 2 | 9.5 % | 49 | 271 |
| Recepción (llegada) | 9 | 0 | 0.0 % | 30 | 418 |
| Iniciar atención | 9 | 0 | 0.0 % | 36 | 73 |
| Leer la consulta | 9 | 0 | 0.0 % | 26 | 52 |
| Registrar diagnóstico | 9 | 0 | 0.0 % | 41 | 872 |
| Registrar receta | 8 | 1 | 11.1 % | 36 | 30 001 |
| Actualizar mascota | 8 | 1 | 11.1 % | 32 | 30 000 |
| Cerrar la consulta | 8 | 1 | 11.1 % | 102 | 30 001 |
| Caja: crear punto de cobro | 2 | 0 | 0.0 % | 343 | 642 |
| Caja: vincular equipo | 2 | 0 | 0.0 % | 37 | 45 |
| Caja: abrir la caja | 2 | 0 | 0.0 % | 50 | 63 |
| Cobrar | 2 | 0 | 0.0 % | 241 | 424 |
| Cancelar cita | 4 | 1 | 20.0 % | 39 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| lista_de_historias | 18 | 4 (22 %) | 50 | 30 001 | 20 % |
| apoderado | 24 | 3 (12 %) | 69 | 30 001 | 15 % |
| citas_del_mes | 14 | 2 (14 %) | 48 | 30 000 | 10 % |
| empleados | 14 | 2 (14 %) | 32 | 30 000 | 10 % |
| navegacion | 28 | 1 (4 %) | 25 | 724 | 5 % |
| cerrar_consulta | 9 | 1 (11 %) | 102 | 30 001 | 5 % |
| propietarios | 13 | 1 (8 %) | 69 | 30 000 | 5 % |
| cumplimiento | 28 | 1 (4 %) | 21 | 244 | 5 % |
| notificaciones | 28 | 1 (4 %) | 17 | 51 | 5 % |
| receta | 9 | 1 (11 %) | 36 | 30 001 | 5 % |
| actualizar_mascota | 9 | 1 (11 %) | 32 | 30 000 | 5 % |
| cancelar_cita | 5 | 1 (20 %) | 39 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 6 | 869 | 0 % |
| 0.1 | 6 | 42 | 353 | 0 % |
| 0.3 | 10 | 50 | 321 | 0 % |
| 0.6 | 10 | 33 | 108 | 0 % |
| 0.8 | 10 | 34 | 272 | 6 % |
| 1.1 | 10 | 35 | 1 851 | 0 % |
| 1.3 | 10 | 43 | 78 | 0 % |
| 1.6 | 10 | 49 | 247 | 0 % |
| 1.8 | 10 | 35 | 78 | 0 % |
| 2.1 | 10 | 39 | 78 | 0 % |
| 2.3 | 10 | 28 | 872 | 0 % |
| 2.8 | 10 | 7 | 30 001 | 86 % |
| 3.1 | 10 | 4 | 30 001 | 100 % |
| 3.3 | 9 | 3 | 30 000 | 100 % |
| 3.6 | 6 | 1 | 30 000 | 100 % |
| 3.8 | 5 | 4 | 30 001 | 100 % |
| 4.3 | 1 | 1 | 30 000 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba (k6, estadísticas del contenedor y consultas a la base). Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
