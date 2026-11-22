# Fase 3: escalado vertical, 2 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.1 % | 20.6 | 509 | 26 / 0 | 48 | 955 | 485 | 6 % | 135 | 0 | 0 / 0 / 4 | 2335 / 4 | CONSISTENTES |
| 150 | DEGRADADO | 132 | 0.6 % | 24.3 | 3 307 | 26 / 0 | 75 | 1 047 | 561 | 9 % | 135 | 0 | 0 / 0 / 52 | 2968 / 52 | CONSISTENTES |
| 200 | AGUANTA | 192 | 0.3 % | 35.8 | 961 | 33 / 0 | 57 | 1 269 | 865 | 10 % | 136 | 0 | 0 / 0 / 38 | 5171 / 38 | CONSISTENTES |
| 250 | SATURADO | 196 | 1.0 % | 31.5 | 5 231 | 32 / 0 | 73 | 1 334 | 971 | 10 % | 136 | 0 | 0 / 0 / 146 | 4619 / 146 | CONSISTENTES |
| 350 | SATURADO | 135 | 1.7 % | 26.4 | 10 686 | 28 / 0 | 87 | 1 232 | 697 | 8 % | 136 | 0 | 0 / 0 / 247 | 4181 / 247 | CONSISTENTES |
| 500 | SATURADO | 133 | 1.8 % | 26.4 | 16 801 | 29 / 0 | 92 | 1 222 | 654 | 8 % | 136 | 0 | 0 / 0 / 334 | 5199 / 334 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
