# Capacidad: 250 usuarios simultáneos, configuración B

- recursos del backend: 1 CPU, 2g de RAM (como Render Standard)
- pausa entre acciones: 15 s (con variación aleatoria)
- mezcla de acciones (panel, agenda, historia clínica, reportes): 30,35,30,5 %

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | FALLA | FALLA |
| Último tramo sano (usuarios) | 63 | 94 |
| Peticiones hechas | 3 855 | 3 937 |
| Peticiones correctas (2xx) | 2 004 | 2 068 |
| Errores (%) | 48.0 % | 47.5 % |
| Ritmo medio (peticiones/s) | 7.9 | 8.0 |
| Tiempo mediano (ms) | 7 344 | 4 354 |
| Tiempo p95 (ms) | 30 001 | 30 001 |
| CPU del backend, media (%) | 22 | 18 |
| Tiempo con la CPU al tope (%) | 13 | 7 |
| CPU de PostgreSQL, media (%) | 10 | 6 |
| Memoria pico (MB) | 1 240 | 1 228 |
| Sentencias SQL | 241 914 | 51 237 |
| Reinicios del contenedor | 0 | 0 |

## Qué pasó en el ANTES: FALLA

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3673 avisos en el registro.
- CPU del backend: 22 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 13 % del tiempo.
- CPU de PostgreSQL: 10 % de media y 77 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 240 MB (61 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 2.1 con unos 138 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **63 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, ANTES** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 920 | 815 (89 %) | 30 000 | 30 001 | 43 % |
| citas_del_mes | 590 | 165 (28 %) | 1 144 | 30 001 | 10 % |
| empleados | 186 | 109 (59 %) | 30 000 | 30 001 | 6 % |
| propietarios | 175 | 98 (56 %) | 30 000 | 30 001 | 5 % |
| lista_de_historias | 162 | 95 (59 %) | 30 000 | 30 001 | 5 % |
| privacidad | 257 | 88 (34 %) | 665 | 30 001 | 5 % |
| navegacion | 257 | 83 (32 %) | 717 | 30 001 | 5 % |
| notificaciones | 240 | 81 (34 %) | 414 | 30 001 | 4 % |
| cumplimiento | 243 | 79 (33 %) | 527 | 30 001 | 4 % |
| avisos_de_cumplimiento | 227 | 70 (31 %) | 349 | 30 001 | 4 % |
| panel | 218 | 62 (28 %) | 799 | 30 001 | 3 % |
| reporte_clinico | 41 | 24 (59 %) | 30 000 | 30 001 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 3 | 28 | 695 | 0 % |
| 0.3 | 33 | 204 | 353 | 0 % |
| 0.8 | 63 | 217 | 1 915 | 0 % |
| 1.3 | 93 | 434 | 3 159 | 0 % |
| 1.8 | 123 | 69 | 16 365 | 0 % |
| 2.3 | 153 | 90 | 30 001 | 92 % |
| 2.8 | 183 | 81 | 30 001 | 100 % |
| 3.3 | 213 | 86 | 30 001 | 100 % |
| 3.8 | 243 | 107 | 30 001 | 100 % |
| 4.3 | 250 | 109 | 30 001 | 100 % |
| 4.8 | 250 | 102 | 30 001 | 100 % |
| 5.3 | 250 | 89 | 30 001 | 100 % |
| 5.8 | 250 | 106 | 30 001 | 100 % |
| 6.3 | 250 | 104 | 30 001 | 100 % |
| 6.8 | 239 | 84 | 30 001 | 100 % |
| 7.3 | 128 | 50 | 30 001 | 100 % |
| 7.8 | 64 | 27 | 30 001 | 100 % |

</details>

## Qué pasó en el DESPUÉS: FALLA

- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 3709 avisos en el registro.
- CPU del backend: 18 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 6 % de media y 54 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 228 MB (60 % de 2048 MB).
- Los fallos fueron: 100 % sin respuesta (tiempo agotado).
- **Los errores superaron el 5 % por primera vez al minuto 2.1 con unos 139 usuarios.**
- Último tramo sano (menos de 1 % de errores y p95 menor a 2 s): unos **94 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.

**Por endpoint, DESPUÉS** (los que más tiempo acumulan):

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 970 | 873 (90 %) | 30 000 | 30 001 | 46 % |
| citas_del_mes | 595 | 179 (30 %) | 117 | 30 001 | 10 % |
| empleados | 195 | 126 (65 %) | 30 000 | 30 001 | 7 % |
| propietarios | 182 | 113 (62 %) | 30 000 | 30 001 | 6 % |
| notificaciones | 241 | 75 (31 %) | 18 | 30 001 | 4 % |
| cumplimiento | 245 | 74 (30 %) | 28 | 30 001 | 4 % |
| privacidad | 251 | 73 (29 %) | 20 | 30 001 | 4 % |
| navegacion | 251 | 68 (27 %) | 24 | 30 001 | 4 % |
| avisos_de_cumplimiento | 233 | 72 (31 %) | 19 | 30 001 | 4 % |
| panel | 225 | 66 (29 %) | 259 | 30 001 | 4 % |
| lista_de_historias | 146 | 61 (42 %) | 1 823 | 30 001 | 3 % |
| pacientes_inactivos | 27 | 22 (81 %) | 30 001 | 30 001 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 35 | 591 | 0 % |
| 0.3 | 34 | 206 | 331 | 0 % |
| 0.8 | 64 | 360 | 326 | 0 % |
| 1.3 | 94 | 303 | 1 602 | 0 % |
| 1.8 | 124 | 2 | 30 001 | 100 % |
| 2.3 | 154 | 23 | 30 001 | 100 % |
| 2.8 | 184 | 41 | 30 001 | 100 % |
| 3.3 | 214 | 57 | 30 001 | 100 % |
| 3.8 | 244 | 78 | 30 001 | 100 % |
| 4.3 | 250 | 98 | 30 001 | 100 % |
| 4.8 | 250 | 94 | 30 001 | 100 % |
| 5.3 | 250 | 90 | 30 001 | 100 % |
| 5.8 | 250 | 98 | 30 001 | 100 % |
| 6.3 | 250 | 85 | 30 001 | 100 % |
| 6.8 | 237 | 95 | 30 001 | 100 % |
| 7.3 | 127 | 49 | 30 001 | 100 % |
| 7.8 | 58 | 26 | 30 001 | 100 % |

</details>

> Veredicto: AGUANTA = menos de 1 % de errores y p95 menor a 2 s; DEGRADADO = menos de 5 % y p95 menor a 5 s; SATURADO = menos de 5 % de errores pero p95 de 5 s o más; FALLA = 5 % o más de errores. Los usuarios virtuales hacen una acción cada ~15 s (con variación), no peticiones sin pausa. Es una medición de laboratorio, no una promesa de producción.
