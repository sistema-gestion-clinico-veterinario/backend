# Fase 3e: configuración de PostgreSQL y heap

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Panel p95 (ms) | CPU total por 1.000 peticiones (s) | Tiempo en GC | Planes lentos / con JIT | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 350 | Código nuevo, configuración actual | AGUANTA | 0.6 % | 54.1 | 1 635 | 1 609 | 23.0 | 14.2 % | 14 / 0 | 0 | 10356 / 190 | CONSISTENTES |
| 350 | Código nuevo + plan por consulta y sin JIT | DEGRADADO | 0.7 % | 53.5 | 2 073 | 1 823 | 22.0 | 12.5 % | 10 / 0 | 0 | 9982 / 200 | CONSISTENTES |
| 350 | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB | AGUANTA | 0.5 % | 55.9 | 1 188 | 1 132 | 21.4 | 10.0 % | 11 / 0 | 0 | 11332 / 149 | CONSISTENTES |
| 500 | Código nuevo, configuración actual | SATURADO | 1.5 % | 54.9 | 7 624 | 6 895 | 24.2 | 11.6 % | 20 / 0 | 0 | 12136 / 580 | CONSISTENTES |
| 500 | Código nuevo + plan por consulta y sin JIT | SATURADO | 1.5 % | 58.6 | 5 888 | 4 813 | 23.2 | 13.7 % | 21 / 0 | 0 | 13056 / 619 | CONSISTENTES |
| 500 | Código nuevo + plan por consulta, sin JIT y heap de 1,25 GB | DEGRADADO | 1.5 % | 61.5 | 4 466 | 3 920 | 21.5 | 11.9 % | 22 / 0 | 0 | 14256 / 633 | CONSISTENTES |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen. "Plan por consulta" = plan_cache_mode=force_custom_plan; "sin JIT" = jit=off. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo.
