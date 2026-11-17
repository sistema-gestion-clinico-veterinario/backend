# Fase 2b: resumen de la escalera de carga con escrituras

## Control: Metaspace de 128 MB (igual que el Dockerfile actual)

| Usuarios | Versión | Veredicto | Sin degradación hasta (usuarios) | Errores | p95 (ms) | Escrituras correctas / fallidas | Datos | Metaspace pico (MB) | Metaspace | Correos recibidos |
|---|---|---|---|---|---|---|---|---|---|---|
| 10 | DESPUÉS, 128 MB | AGUANTA | ~10 | 0.4 % | 298 | 167 / 2 | CONSISTENTES | 126 | estable | 25 |

## Escalera con Metaspace de 256 MB y correo de pruebas

| Usuarios | Versión | Veredicto | Sin degradación hasta (usuarios) | Errores | p95 (ms) | Escrituras correctas / fallidas | Datos | Metaspace pico (MB) | Metaspace | Correos recibidos |
|---|---|---|---|---|---|---|---|---|---|---|
| 3 | ANTES | AGUANTA | ~3 | 0.0 % | 412 | 29 / 0 | CONSISTENTES | 125 | crece +6 % | 5 |
| 3 | DESPUÉS | AGUANTA | ~3 | 0.0 % | 460 | 13 / 0 | CONSISTENTES | 122 | crece +6 % | 4 |
| 10 | ANTES | AGUANTA | ~10 | 0.0 % | 294 | 180 / 0 | CONSISTENTES | 130 | estable | 25 |
| 10 | DESPUÉS | AGUANTA | ~10 | 0.0 % | 353 | 202 / 0 | CONSISTENTES | 130 | estable | 21 |
| 20 | ANTES | AGUANTA | ~20 | 0.0 % | 302 | 433 / 0 | CONSISTENTES | 132 | estable | 55 |
| 20 | DESPUÉS | AGUANTA | ~20 | 0.0 % | 352 | 329 / 0 | CONSISTENTES | 131 | estable | 47 |
| 40 | ANTES | AGUANTA | ~40 | 0.0 % | 298 | 810 / 0 | CONSISTENTES | 133 | estable | 108 |
| 40 | DESPUÉS | AGUANTA | ~40 | 0.0 % | 401 | 814 / 0 | CONSISTENTES | 132 | estable | 104 |
| 60 | ANTES | AGUANTA | ~60 | 0.3 % | 358 | 1238 / 11 | CONSISTENTES | 134 | estable | 181 |
| 60 | DESPUÉS | AGUANTA | ~60 | 0.2 % | 378 | 1234 / 8 | CONSISTENTES | 133 | estable | 174 |
| 100 | ANTES | AGUANTA | ~100 | 0.2 % | 503 | 2240 / 11 | CONSISTENTES | 135 | estable | 321 |
| 100 | DESPUÉS | AGUANTA | ~100 | 0.1 % | 245 | 2196 / 9 | CONSISTENTES | 135 | estable | 315 |
| 250 | ANTES | FALLA | ~87 | 18.9 % | 30 001 | 1684 / 322 | CONSISTENTES | 136 | estable | 400 |
| 250 | DESPUÉS | DEGRADADO | ~232 | 0.8 % | 2 982 | 5539 / 134 | CONSISTENTES | 136 | estable | 855 |

> Datos CONSISTENTES = lo que k6 contó como creado coincide con la base y no hay registros huérfanos. "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Metaspace: lectura de los registros de GC de la propia JVM. Cada nivel reinicia el backend; la base conserva lo escrito en niveles anteriores.
