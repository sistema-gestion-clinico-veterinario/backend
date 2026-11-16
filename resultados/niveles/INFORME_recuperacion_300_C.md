# Recuperación: pico de 300 usuarios durante 3 minutos, configuración C

- Base de 50 usuarios, subida a 300 usuarios en 60 s, pico de 3 minutos, bajada a 50 y 5 minutos de recuperación.
- Se considera "normal" cuando p95 < 2000 ms y menos de 1 % de errores durante 30 s seguidos.

| | ANTES | DESPUÉS |
|---|---|---|
| Veredicto | NO SE RECUPERÓ | SE RECUPERÓ en 323 s |
| Base: p95 (ms) / errores | 756 / 0.0 % | 222 / 0.0 % |
| Pico: p95 (ms) / errores | 30 001 / 100.0 % | 30 001 / 100.0 % |
| Tras el pico (últimos 2 min): p95 (ms) / errores | 30 001 / 100.0 % | 30 001 / 24.2 % |
| Salud al terminar (HTTP) | 000 | 200 |
| Contenedor muerto por memoria (OOMKilled) | no | no |
| Reinicios del contenedor | 0 | 0 |
| Errores OutOfMemoryError en el registro | 1 369 | 0 |
| Avisos de pool de conexiones agotado | 3 493 | 7 361 |

## Qué pasó en el ANTES: NO SE RECUPERÓ

- Durante el pico el p95 típico fue 30 001 ms (peor tramo 30 001 ms) y 100.0 % de errores.
- **No volvió a la normalidad en los 5 minutos posteriores a bajar la carga.**
- Hubo agotamiento de memoria durante la prueba.
- Al terminar el chequeo de salud respondió 000: el servicio **no quedó funcionando**.

<details><summary>Línea de tiempo cada 30 s</summary>

| Minuto | Fase | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | base | 13 | 69 | 1 799 | 0 % |
| 0.4 | base | 50 | 335 | 612 | 0 % |
| 0.9 | base | 50 | 301 | 756 | 0 % |
| 1.4 | base | 82 | 189 | 2 675 | 0 % |
| 2.1 | subida | 270 | 84 | 30 001 | 100 % |
| 2.6 | pico | 300 | 105 | 30 001 | 98 % |
| 3.1 | pico | 300 | 83 | 30 001 | 100 % |
| 3.6 | pico | 300 | 109 | 30 001 | 100 % |
| 4.1 | pico | 300 | 116 | 30 001 | 100 % |
| 4.6 | pico | 300 | 89 | 30 001 | 100 % |
| 5.1 | pico | 300 | 99 | 30 001 | 100 % |
| 5.6 | bajada | 291 | 105 | 30 001 | 100 % |
| 6.1 | recuperación | 153 | 55 | 30 001 | 100 % |
| 6.6 | recuperación | 55 | 15 | 30 001 | 100 % |
| 7.1 | recuperación | 50 | 21 | 30 001 | 100 % |
| 7.6 | recuperación | 50 | 19 | 30 001 | 100 % |
| 8.1 | recuperación | 50 | 22 | 30 001 | 100 % |
| 8.6 | recuperación | 50 | 26 | 30 001 | 100 % |
| 9.1 | recuperación | 50 | 25 | 30 001 | 100 % |
| 9.6 | recuperación | 50 | 26 | 30 001 | 100 % |
| 10.1 | recuperación | 50 | 26 | 30 001 | 100 % |
| 10.6 | recuperación | 50 | 22 | 30 001 | 100 % |
| 11.1 | recuperación | 50 | 21 | 30 001 | 100 % |
| 11.6 | recuperación | 37 | 13 | 30 001 | 100 % |
| 12.1 | recuperación | 18 | 6 | 30 001 | 100 % |

</details>

## Qué pasó en el DESPUÉS: SE RECUPERÓ en 323 s

- Durante el pico el p95 típico fue 30 001 ms (peor tramo 30 001 ms) y 100.0 % de errores.
- **Volvió a la normalidad 323 s después de bajar la carga.**

<details><summary>Línea de tiempo cada 30 s</summary>

| Minuto | Fase | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|---|
| 0.0 | base | 13 | 70 | 2 076 | 0 % |
| 0.4 | base | 50 | 324 | 523 | 0 % |
| 0.9 | base | 50 | 344 | 133 | 0 % |
| 1.4 | base | 83 | 151 | 261 | 0 % |
| 2.1 | subida | 270 | 91 | 30 001 | 100 % |
| 2.6 | pico | 300 | 124 | 30 001 | 100 % |
| 3.1 | pico | 300 | 90 | 30 001 | 100 % |
| 3.6 | pico | 300 | 132 | 30 001 | 100 % |
| 4.1 | pico | 300 | 123 | 30 001 | 100 % |
| 4.6 | pico | 300 | 86 | 30 001 | 100 % |
| 5.1 | pico | 300 | 117 | 30 001 | 100 % |
| 5.6 | bajada | 292 | 116 | 30 001 | 100 % |
| 6.1 | recuperación | 165 | 57 | 30 001 | 100 % |
| 6.6 | recuperación | 54 | 20 | 30 001 | 100 % |
| 7.1 | recuperación | 50 | 23 | 30 001 | 100 % |
| 7.6 | recuperación | 50 | 22 | 30 001 | 100 % |
| 8.1 | recuperación | 50 | 17 | 30 001 | 100 % |
| 8.6 | recuperación | 50 | 24 | 30 001 | 100 % |
| 9.1 | recuperación | 50 | 23 | 30 001 | 100 % |
| 9.6 | recuperación | 50 | 21 | 30 001 | 100 % |
| 10.1 | recuperación | 50 | 17 | 30 001 | 100 % |
| 10.6 | recuperación | 50 | 195 | 25 007 | 4 % |
| 11.1 | recuperación | 47 | 158 | 150 | 0 % |

</details>

> Laboratorio con la base de datos en el mismo equipo: sirve para comparar versiones, no es una promesa para producción.
