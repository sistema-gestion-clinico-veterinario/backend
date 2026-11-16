# Capacidad: 100 usuarios simultáneos, configuración A

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- mezcla de acciones (panel, agenda, historia clínica, reportes): 30,35,30,5 %

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 42 | 19 |
| Peticiones hechas | 968 | 922 |
| Peticiones correctas (2xx) | 371 | 380 |
| Errores (%) | 61.7 % | 58.8 % |
| Ritmo medio (peticiones/s) | 2.8 | 2.9 |
| Tiempo mediano (ms) | 30 000 | 30 000 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 60 | 17 |
| Tiempo con la CPU al tope (%) | 56 | 10 |
| CPU de PostgreSQL, media (%) | 2 | 2 |
| Memoria pico (MB) | 425 | 443 |
| Sentencias SQL | 37 169 | 11 144 |
| Reinicios del contenedor | 0 | 0 |

## Qué pasó en el ANTES: FALLA

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 44 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 16 avisos en el registro.
- CPU del backend: 60 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 56 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 27 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 425 MB (21 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % errores 5xx del servidor.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 56 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **42 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, ANTES** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 369 | 338 (92 %) | 30 000 | 30 001 | 56 % |
| citas_del_mes | 105 | 40 (38 %) | 489 | 30 001 | 7 % |
| privacidad | 64 | 27 (42 %) | 41 | 30 001 | 4 % |
| navegacion | 64 | 25 (39 %) | 31 | 30 001 | 4 % |
| empleados | 37 | 25 (68 %) | 30 000 | 30 001 | 4 % |
| cumplimiento | 61 | 24 (39 %) | 23 | 30 001 | 4 % |
| notificaciones | 58 | 23 (40 %) | 17 | 30 001 | 4 % |
| propietarios | 31 | 19 (61 %) | 30 000 | 30 001 | 3 % |
| avisos_de_cumplimiento | 54 | 19 (35 %) | 19 | 30 001 | 3 % |
| panel | 53 | 18 (34 %) | 195 | 30 001 | 3 % |
| lista_de_historias | 21 | 11 (52 %) | 30 000 | 30 001 | 2 % |
| reporte_de_ventas | 7 | 7 (100 %) | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 6 | 715 | 0 % |
| 0.0 | 15 | 119 | 360 | 0 % |
| 0.3 | 29 | 184 | 328 | 0 % |
| 0.5 | 42 | 44 | 970 | 0 % |
| 0.8 | 56 | 21 | 26 805 | 14 % |
| 1.0 | 69 | 21 | 30 001 | 100 % |
| 1.3 | 83 | 34 | 30 001 | 100 % |
| 1.5 | 97 | 26 | 30 001 | 100 % |
| 1.8 | 100 | 35 | 30 001 | 100 % |
| 2.0 | 100 | 43 | 30 001 | 100 % |
| 2.3 | 100 | 33 | 30 001 | 100 % |
| 2.5 | 100 | 37 | 30 001 | 100 % |
| 2.8 | 100 | 44 | 30 001 | 100 % |
| 3.0 | 100 | 28 | 30 001 | 100 % |
| 3.3 | 100 | 44 | 30 001 | 100 % |
| 3.5 | 100 | 37 | 30 001 | 100 % |
| 3.8 | 100 | 34 | 30 001 | 100 % |
| 4.0 | 100 | 36 | 30 001 | 100 % |
| 4.3 | 100 | 37 | 30 001 | 100 % |
| 4.5 | 93 | 32 | 30 001 | 100 % |
| 4.8 | 72 | 31 | 30 001 | 100 % |
| 5.0 | 45 | 18 | 30 001 | 100 % |
| 5.3 | 29 | 10 | 30 001 | 100 % |
| 5.5 | 25 | 13 | 30 001 | 100 % |
| 5.8 | 6 | 1 | 30 000 | 100 % |

</details>

## Qué pasó en el DESPUÉS: FALLA

- Java agotó su memoria de trabajo (heap): 9 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 2009 avisos en el registro.
- CPU del backend: 17 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 10 % del tiempo.
- CPU de PostgreSQL: 2 % de media y 44 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 443 MB (22 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 59 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **19 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, DESPUÉS** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 407 | 377 (93 %) | 30 000 | 30 001 | 67 % |
| citas_del_mes | 86 | 38 (44 %) | 17 181 | 30 001 | 8 % |
| empleados | 30 | 16 (53 %) | 30 000 | 30 001 | 4 % |
| propietarios | 30 | 15 (50 %) | 29 791 | 30 001 | 3 % |
| privacidad | 55 | 12 (22 %) | 73 | 30 001 | 3 % |
| navegacion | 55 | 16 (29 %) | 88 | 30 001 | 3 % |
| lista_de_historias | 19 | 14 (74 %) | 30 000 | 30 001 | 3 % |
| notificaciones | 55 | 13 (24 %) | 64 | 30 001 | 2 % |
| avisos_de_cumplimiento | 55 | 12 (22 %) | 83 | 30 001 | 2 % |
| cumplimiento | 55 | 12 (22 %) | 217 | 30 001 | 2 % |
| panel | 55 | 10 (18 %) | 485 | 30 001 | 2 % |
| historia | 5 | 1 (20 %) | 491 | 30 001 | 0 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 35 | 491 | 0 % |
| 0.1 | 19 | 126 | 343 | 0 % |
| 0.3 | 32 | 95 | 3 050 | 0 % |
| 0.6 | 46 | 5 | 4 653 | 0 % |
| 0.8 | 59 | 20 | 30 001 | 70 % |
| 1.1 | 73 | 24 | 30 001 | 100 % |
| 1.4 | 87 | 25 | 30 001 | 100 % |
| 1.6 | 100 | 36 | 30 001 | 100 % |
| 1.9 | 100 | 34 | 30 001 | 97 % |
| 2.1 | 100 | 38 | 30 001 | 100 % |
| 2.4 | 100 | 38 | 30 001 | 92 % |
| 2.6 | 100 | 28 | 30 001 | 100 % |
| 2.9 | 100 | 44 | 30 001 | 100 % |
| 3.1 | 100 | 35 | 30 001 | 100 % |
| 3.4 | 100 | 36 | 30 001 | 100 % |
| 3.6 | 100 | 36 | 30 001 | 100 % |
| 3.9 | 100 | 42 | 30 001 | 100 % |
| 4.1 | 100 | 31 | 30 001 | 100 % |
| 4.3 | 100 | 32 | 30 001 | 97 % |
| 4.6 | 90 | 40 | 30 001 | 100 % |
| 4.8 | 64 | 122 | 30 001 | 11 % |

</details>

> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; SATURADO = menos de 5 % de errores pero p95 de 5 s o más; FALLA = 5 % o más de errores. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.
