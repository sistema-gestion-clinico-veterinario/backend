# Fase 3e: configuración de PostgreSQL y heap

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Panel p95 (ms) | CPU total por 1.000 peticiones (s) | Tiempo en GC | Planes lentos / con JIT | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 250 | Código anterior, configuración actual | SATURADO | 1.5 % | 28.7 | 6 939 | 9 060 | 51.8 | 6.3 % | 85 / 0 | 0 | 4089 / 191 | CONSISTENTES |
| 250 | Código anterior + plan por consulta y sin JIT | DEGRADADO | 0.6 % | 39.7 | 2 528 | 2 029 | 33.0 | 9.8 % | 16 / 0 | 0 | 6172 / 102 | CONSISTENTES |
| 350 | Código anterior, configuración actual | SATURADO | 1.9 % | 34.7 | 9 039 | 9 570 | 43.9 | 9.1 % | 79 / 0 | 0 | 5682 / 363 | CONSISTENTES |
| 350 | Código anterior + plan por consulta y sin JIT | FALLA | 25.1 % | 17.4 | 30 001 | 30 001 | 33.5 | 3.9 % | 6 / 0 | 2 703 | 2363 / 509 | CONSISTENTES |

> Cada corrida parte de la misma base de la plantilla y de la misma imagen. "Plan por consulta" = plan_cache_mode=force_custom_plan; "sin JIT" = jit=off. Una corrida por variante y nivel; la comparación válida es dentro de cada trabajo.
