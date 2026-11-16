# Resumen de capacidad por configuración de memoria

Configuraciones: **A** = heap 192 MB, compilador reducido (como el Dockerfile de hoy); **B** = heap 1 GB, compilador reducido; **C** = heap 1 GB, compilador completo. Todas con 1 CPU y 2 GB.

| Prueba | Configuración | ANTES | DESPUÉS |
|---|---|---|---|
| 100 usuarios | A | **FALLA** · ~42 usuarios sanos · errores 61.7 % · p95 30 001 ms · sin memoria | **FALLA** · ~19 usuarios sanos · errores 58.8 % · p95 30 001 ms · sin memoria |
| 100 usuarios | B | **AGUANTA** · ~100 usuarios sanos · errores 0.0 % · p95 1 637 ms | **AGUANTA** · ~100 usuarios sanos · errores 0.0 % · p95 1 544 ms |
| 250 usuarios | A | **FALLA** · ~34 usuarios sanos · errores 83.7 % · p95 30 001 ms · sin memoria | **FALLA** · ~18 usuarios sanos · errores 93.2 % · p95 30 001 ms · sin memoria |
| 250 usuarios | B | **FALLA** · ~63 usuarios sanos · errores 48.0 % · p95 30 001 ms | **FALLA** · ~94 usuarios sanos · errores 47.5 % · p95 30 001 ms |
| Recuperación (pico de 300) | C | **NO SE RECUPERÓ** · p95 en el pico 30 001 ms · salud final 000 | **SE RECUPERÓ en 323 s** · p95 en el pico 30 001 ms · salud final 200 |

> Cada fila sale de una corrida con el backend reiniciado desde cero. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
