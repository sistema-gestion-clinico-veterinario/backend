# Capacidad: 100 usuarios simultáneos, configuración B

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- mezcla de acciones (panel, agenda, historia clínica, reportes): 30,35,30,5 %

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | AGUANTA | AGUANTA |
| Último tramo sano (usuarios) | 100 | 100 |
| Peticiones hechas | 7 748 | 8 034 |
| Peticiones correctas (2xx) | 7 748 | 8 034 |
| Errores (%) | 0.0 % | 0.0 % |
| Ritmo medio (peticiones/s) | 25.6 | 26.3 |
| Tiempo mediano (ms) | 217 | 50 |
| Tiempo p95 (ms) | 1 637 | 1 544 |
| CPU del backend, media (%) | 75 | 66 |
| Tiempo con la CPU al tope (%) | 45 | 32 |
| CPU de PostgreSQL, media (%) | 47 | 41 |
| Memoria pico (MB) | 1 225 | 1 247 |
| Sentencias SQL | 918 831 | 201 299 |
| Reinicios del contenedor | 0 | 0 |

## Qué pasó en el ANTES: AGUANTA

- CPU del backend: 75 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 45 % del tiempo.
- CPU de PostgreSQL: 47 % de media y 99 % de pico (de 1 núcleo).
- **Diagnóstico: el límite es la CPU del backend** (pasó mucho tiempo al tope).
- Memoria del contenedor: pico de 1 225 MB (60 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 3.1 s.

**Por endpoint, ANTES** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| citas_del_mes | 2215 | 0 (0 %) | 364 | 1 794 | 34 % |
| reporte_clinico | 44 | 0 (0 %) | 11 035 | 13 585 | 12 % |
| panel | 455 | 0 (0 %) | 597 | 1 798 | 9 % |
| propietarios | 443 | 0 (0 %) | 308 | 1 594 | 6 % |
| login | 100 | 0 (0 %) | 533 | 4 873 | 4 % |
| empleados | 443 | 0 (0 %) | 112 | 1 593 | 4 % |
| lista_de_historias | 337 | 0 (0 %) | 111 | 1 624 | 4 % |
| navegacion | 455 | 0 (0 %) | 61 | 1 290 | 4 % |
| notificaciones | 455 | 0 (0 %) | 40 | 1 159 | 3 % |
| cumplimiento | 455 | 0 (0 %) | 49 | 1 097 | 3 % |
| avisos_de_cumplimiento | 455 | 0 (0 %) | 42 | 1 096 | 3 % |
| historia | 337 | 0 (0 %) | 116 | 1 277 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 12 | 84 | 356 | 0 % |
| 0.2 | 26 | 172 | 332 | 0 % |
| 0.5 | 39 | 216 | 325 | 0 % |
| 0.7 | 53 | 359 | 252 | 0 % |
| 1.0 | 67 | 321 | 816 | 0 % |
| 1.2 | 80 | 355 | 3 078 | 0 % |
| 1.5 | 94 | 511 | 1 744 | 0 % |
| 1.7 | 100 | 581 | 382 | 0 % |
| 2.0 | 100 | 455 | 2 151 | 0 % |
| 2.2 | 100 | 415 | 2 516 | 0 % |
| 2.5 | 100 | 497 | 1 854 | 0 % |
| 2.7 | 100 | 442 | 1 414 | 0 % |
| 3.0 | 100 | 522 | 2 085 | 0 % |
| 3.2 | 100 | 515 | 488 | 0 % |
| 3.5 | 100 | 526 | 475 | 0 % |
| 3.7 | 100 | 497 | 1 390 | 0 % |
| 4.0 | 100 | 455 | 2 178 | 0 % |
| 4.2 | 100 | 500 | 388 | 0 % |
| 4.5 | 88 | 311 | 270 | 0 % |
| 4.7 | 49 | 14 | 210 | 0 % |

</details>

## Qué pasó en el DESPUÉS: AGUANTA

- CPU del backend: 66 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 32 % del tiempo.
- CPU de PostgreSQL: 41 % de media y 101 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 247 MB (61 % de 2048 MB).
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 5.7 s.

**Por endpoint, DESPUÉS** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| citas_del_mes | 2065 | 0 (0 %) | 75 | 1 674 | 25 % |
| reporte_clinico | 63 | 0 (0 %) | 4 891 | 16 053 | 18 % |
| panel | 499 | 0 (0 %) | 278 | 1 763 | 10 % |
| propietarios | 413 | 0 (0 %) | 134 | 1 370 | 5 % |
| navegacion | 499 | 0 (0 %) | 19 | 1 155 | 4 % |
| lista_de_historias | 372 | 0 (0 %) | 65 | 1 372 | 4 % |
| privacidad | 499 | 0 (0 %) | 17 | 1 290 | 3 % |
| notificaciones | 499 | 0 (0 %) | 15 | 1 052 | 3 % |
| historia | 372 | 0 (0 %) | 51 | 1 447 | 3 % |
| diagnosticos | 372 | 0 (0 %) | 27 | 1 135 | 3 % |
| consulta | 372 | 0 (0 %) | 32 | 1 394 | 3 % |
| recetas | 372 | 0 (0 %) | 30 | 1 190 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 10 | 70 | 544 | 0 % |
| 0.2 | 24 | 158 | 332 | 0 % |
| 0.5 | 38 | 230 | 321 | 0 % |
| 0.7 | 51 | 281 | 512 | 0 % |
| 0.9 | 65 | 395 | 226 | 0 % |
| 1.2 | 79 | 458 | 286 | 0 % |
| 1.4 | 92 | 542 | 266 | 0 % |
| 1.7 | 100 | 555 | 280 | 0 % |
| 1.9 | 100 | 556 | 640 | 0 % |
| 2.2 | 100 | 566 | 371 | 0 % |
| 2.5 | 100 | 566 | 371 | 0 % |
| 2.7 | 100 | 529 | 186 | 0 % |
| 3.0 | 100 | 590 | 219 | 0 % |
| 3.2 | 100 | 564 | 275 | 0 % |
| 3.5 | 100 | 398 | 1 296 | 0 % |
| 3.7 | 100 | 395 | 4 355 | 0 % |
| 4.0 | 100 | 482 | 1 538 | 0 % |
| 4.2 | 100 | 299 | 3 698 | 0 % |
| 4.5 | 96 | 359 | 5 681 | 0 % |
| 4.7 | 71 | 41 | 148 | 0 % |

</details>

> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; SATURADO = menos de 5 % de errores pero p95 de 5 s o más; FALLA = 5 % o más de errores. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.
