# Capacidad: 250 usuarios simultáneos, configuración A

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- mezcla de acciones (panel, agenda, historia clínica, reportes): 30,35,30,5 %

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 34 | 18 |
| Peticiones hechas | 2 144 | 1 921 |
| Peticiones correctas (2xx) | 349 | 130 |
| Errores (%) | 83.7 % | 93.2 % |
| Ritmo medio (peticiones/s) | 4.4 | 3.9 |
| Tiempo mediano (ms) | 30 000 | 30 000 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 19 | 15 |
| Tiempo con la CPU al tope (%) | 15 | 13 |
| CPU de PostgreSQL, media (%) | 1 | 0 |
| Memoria pico (MB) | 420 | 410 |
| Sentencias SQL | 29 841 | 3 230 |
| Reinicios del contenedor | 0 | 0 |

## Qué pasó en el ANTES: FALLA

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 21 errores `OutOfMemoryError` en el registro.
- CPU del backend: 19 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 15 % del tiempo.
- CPU de PostgreSQL: 1 % de media y 34 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 420 MB (21 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado), 0 % errores 5xx del servidor, 0 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 64 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **34 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, ANTES** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1491 | 1463 (98 %) | 30 000 | 30 001 | 81 % |
| citas_del_mes | 83 | 42 (51 %) | 23 487 | 30 001 | 3 % |
| empleados | 47 | 39 (83 %) | 30 001 | 30 001 | 2 % |
| propietarios | 44 | 36 (82 %) | 30 000 | 30 001 | 2 % |
| privacidad | 64 | 33 (52 %) | 30 000 | 30 001 | 2 % |
| lista_de_historias | 42 | 25 (60 %) | 30 000 | 30 001 | 2 % |
| navegacion | 64 | 32 (50 %) | 15 221 | 30 001 | 2 % |
| cumplimiento | 62 | 31 (50 %) | 15 151 | 30 001 | 2 % |
| notificaciones | 56 | 25 (45 %) | 47 | 30 001 | 1 % |
| avisos_de_cumplimiento | 54 | 23 (43 %) | 58 | 30 001 | 1 % |
| panel | 52 | 21 (40 %) | 570 | 30 001 | 1 % |
| reporte_de_ventas | 8 | 8 (100 %) | 30 001 | 30 001 | 0 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 32 | 800 | 0 % |
| 0.3 | 34 | 60 | 946 | 0 % |
| 1.1 | 79 | 16 | 30 001 | 100 % |
| 1.6 | 109 | 36 | 30 001 | 100 % |
| 2.1 | 139 | 49 | 30 001 | 100 % |
| 2.6 | 169 | 67 | 30 001 | 100 % |
| 3.1 | 199 | 64 | 30 001 | 100 % |
| 3.6 | 229 | 82 | 30 001 | 100 % |
| 4.1 | 250 | 88 | 30 001 | 100 % |
| 4.6 | 250 | 81 | 30 001 | 100 % |
| 5.1 | 250 | 84 | 30 001 | 100 % |
| 5.6 | 250 | 87 | 30 001 | 100 % |
| 6.1 | 250 | 96 | 30 001 | 100 % |
| 6.6 | 250 | 88 | 30 001 | 100 % |
| 7.1 | 184 | 75 | 30 001 | 100 % |
| 7.6 | 46 | 13 | 30 001 | 100 % |
| 8.1 | 19 | 2 | 30 001 | 100 % |

</details>

## Qué pasó en el DESPUÉS: FALLA

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Java agotó su memoria de trabajo (heap): 29 errores `OutOfMemoryError` en el registro.
- Se agotó el pool de conexiones a la base de datos: 4 avisos en el registro.
- CPU del backend: 15 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 13 % del tiempo.
- CPU de PostgreSQL: 0 % de media y 14 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 410 MB (20 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 0.8 con unos 63 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **18 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, DESPUÉS** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1576 | 1558 (99 %) | 30 000 | 30 001 | 87 % |
| empleados | 24 | 24 (100 %) | 30 001 | 30 001 | 1 % |
| citas_del_mes | 24 | 24 (100 %) | 30 000 | 30 001 | 1 % |
| privacidad | 42 | 23 (55 %) | 30 000 | 30 001 | 1 % |
| navegacion | 42 | 22 (52 %) | 30 000 | 30 001 | 1 % |
| propietarios | 22 | 22 (100 %) | 30 000 | 30 001 | 1 % |
| lista_de_historias | 22 | 22 (100 %) | 30 000 | 30 001 | 1 % |
| cumplimiento | 40 | 21 (52 %) | 29 999 | 30 001 | 1 % |
| avisos_de_cumplimiento | 38 | 20 (53 %) | 30 000 | 30 001 | 1 % |
| notificaciones | 38 | 20 (53 %) | 30 000 | 30 001 | 1 % |
| panel | 36 | 18 (50 %) | 15 473 | 30 001 | 1 % |
| reporte_clinico | 6 | 6 (100 %) | 30 001 | 30 001 | 0 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 21 | 595 | 0 % |
| 0.3 | 33 | 12 | 4 184 | 0 % |
| 0.8 | 63 | 30 | 30 001 | 100 % |
| 1.3 | 93 | 28 | 30 001 | 100 % |
| 1.8 | 123 | 42 | 30 001 | 100 % |
| 2.3 | 153 | 46 | 30 001 | 100 % |
| 2.8 | 183 | 55 | 30 001 | 100 % |
| 3.3 | 213 | 64 | 30 001 | 100 % |
| 3.8 | 243 | 82 | 30 001 | 100 % |
| 4.3 | 250 | 96 | 30 001 | 100 % |
| 4.8 | 250 | 80 | 30 001 | 100 % |
| 5.3 | 250 | 87 | 30 001 | 100 % |
| 5.8 | 250 | 95 | 30 001 | 100 % |
| 6.3 | 250 | 79 | 30 001 | 100 % |
| 6.8 | 237 | 86 | 30 001 | 100 % |
| 7.3 | 112 | 42 | 30 001 | 100 % |
| 7.8 | 18 | 8 | 30 001 | 100 % |

</details>

> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; SATURADO = menos de 5 % de errores pero p95 de 5 s o más; FALLA = 5 % o más de errores. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.
