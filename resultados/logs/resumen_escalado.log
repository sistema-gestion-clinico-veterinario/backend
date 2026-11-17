# Fase 3: escalado vertical, 1 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.2 % | 20.4 | 642 | 54 / 20 | 59 | 1 069 | 580 | 6 % | 134 | 0 | 0 / 0 / 10 | 2179 / 10 | CONSISTENTES |
| 150 | AGUANTA | 150 | 0.3 % | 27.5 | 1 207 | 64 / 22 | 58 | 1 322 | 976 | 8 % | 135 | 0 | 0 / 0 / 25 | 3606 / 25 | CONSISTENTES |
| 200 | AGUANTA | 161 | 0.4 % | 33.9 | 1 306 | 72 / 35 | 57 | 1 358 | 989 | 9 % | 136 | 0 | 0 / 0 / 49 | 4853 / 49 | CONSISTENTES |
| 250 | FALLA | 141 | 34.2 % | 10.5 | 30 001 | 32 / 24 | 13 | 1 355 | 940 | 4 % | 134 | 2972 | 1756 / 0 / 4 | 1024 / 249 | CONSISTENTES |
| 350 | FALLA | 88 | 33.2 % | 13.9 | 30 001 | 37 / 15 | 32 | 1 075 | 537 | 3 % | 135 | 2740 | 2645 / 0 / 74 | 1486 / 491 | CONSISTENTES |
| 500 | FALLA | 54 | 46.8 % | 14.1 | 30 001 | 33 / 12 | 31 | 1 224 | 645 | 3 % | 135 | 3891 | 4735 / 0 / 127 | 1215 / 726 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
