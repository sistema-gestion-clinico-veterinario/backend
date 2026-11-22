# Fase 3e: configuración de PostgreSQL y heap

- procesador: INTEL(R) XEON(R) PLATINUM 8573C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Panel p95 (ms) | CPU total por 1.000 peticiones (s) | Tiempo en GC | Planes lentos / con JIT | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 350 | Código nuevo, configuración actual | FALLA | 14.9 % | 24.8 | 30 001 | 30 001 | 31.2 | 6.6 % | 5 / 0 | 1 660 | 3753 / 494 | CONSISTENTES |
| 350 | Código nuevo + plan por consulta y sin JIT | DEGRADADO | 1.0 % | 46.7 | 4 362 | 3 428 | 29.3 | 11.4 % | 7 / 0 | 0 | 8510 / 260 | CONSISTENTES |
| 350 | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB | DEGRADADO | 0.8 % | 52.7 | 2 171 | 1 867 | 24.9 | 7.9 % | 11 / 0 | 0 | 10142 / 217 | CONSISTENTES |
| 500 | Código nuevo, configuración actual | SATURADO | 2.8 % | 46.6 | 10 948 | 8 226 | 29.1 | 9.9 % | 26 / 0 | 147 | 9762 / 680 | CONSISTENTES |
| 500 | Código nuevo + plan por consulta y sin JIT | SATURADO | 1.8 % | 48.9 | 9 385 | 7 469 | 28.3 | 11.5 % | 14 / 0 | 0 | 10469 / 622 | CONSISTENTES |
| 500 | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB | SATURADO | 2.0 % | 46.8 | 10 317 | 8 778 | 30.2 | 12.0 % | 21 / 0 | 0 | 10018 / 623 | CONSISTENTES |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen. "Plan por consulta" = plan_cache_mode=force_custom_plan; "sin JIT" = jit=off. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo.
