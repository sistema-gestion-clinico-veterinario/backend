# Fase 2: resumen de la escalera de carga con escrituras

| Usuarios | Versión | Veredicto | Último tramo sano | Errores | p95 (ms) | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|
| 3 | ANTES | AGUANTA | ~3 usuarios | 0.0 % | 335 | 12 / 0 | CONSISTENTES |
| 3 | DESPUÉS | DEGRADADO | ~3 usuarios | 1.2 % | 346 | 36 / 1 | CONSISTENTES |
| 10 | ANTES | FALLA | ~10 usuarios | 5.7 % | 22 971 | 129 / 7 | CONSISTENTES |
| 10 | DESPUÉS | FALLA | ~10 usuarios | 5.1 % | 2 378 | 133 / 9 | CONSISTENTES |
| 20 | ANTES | FALLA | ~20 usuarios | 8.2 % | 30 000 | 275 / 13 | CONSISTENTES |
| 20 | DESPUÉS | FALLA | ~20 usuarios | 14.6 % | 7 744 | 210 / 16 | CONSISTENTES |
| 40 | ANTES | FALLA | ~40 usuarios | 34.6 % | 30 001 | 68 / 55 | CONSISTENTES |
| 40 | DESPUÉS | FALLA | ~40 usuarios | 23.3 % | 30 001 | 190 / 43 | CONSISTENTES |
| 60 | ANTES | FALLA | ~34 usuarios | 44.2 % | 30 001 | 69 / 82 | CONSISTENTES |
| 60 | DESPUÉS | FALLA | ~38 usuarios | 33.6 % | 30 001 | 108 / 58 | CONSISTENTES |
| 100 | ANTES | FALLA | ~59 usuarios | 45.8 % | 30 001 | 110 / 66 | CONSISTENTES |
| 100 | DESPUÉS | FALLA | ~66 usuarios | 41.1 % | 30 001 | 138 / 93 | CONSISTENTES |
| 250 | ANTES | FALLA | ~66 usuarios | 68.0 % | 30 001 | 138 / 157 | CONSISTENTES |
| 250 | DESPUÉS | FALLA | ~52 usuarios | 65.1 % | 30 001 | 155 / 169 | CONSISTENTES |

> Datos CONSISTENTES = lo que k6 contó como creado coincide con la base y no hay registros huérfanos. Cada nivel reinicia el backend; la base conserva lo escrito en niveles anteriores.
