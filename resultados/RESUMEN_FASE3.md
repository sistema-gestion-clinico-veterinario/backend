# Fase 3: escalado vertical, 2 CPU, 4 GB, heap 2g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 2g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 4g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.2 % | 20.0 | 499 | 29 / 0 | 56 | 1 126 | 598 | 8 % | 134 | 0 | 0 / 0 / 10 | 2348 / 10 | CONSISTENTES |
| 150 | DEGRADADO | 101 | 0.4 % | 24.5 | 3 674 | 30 / 0 | 66 | 1 086 | 584 | 8 % | 135 | 0 | 0 / 0 / 36 | 2974 / 36 | CONSISTENTES |
| 200 | AGUANTA | 200 | 0.3 % | 35.2 | 1 360 | 34 / 0 | 57 | 1 439 | 833 | 10 % | 136 | 0 | 0 / 0 / 41 | 5161 / 41 | CONSISTENTES |
| 250 | SATURADO | 179 | 1.1 % | 30.6 | 5 443 | 34 / 0 | 74 | 1 281 | 703 | 9 % | 136 | 0 | 0 / 0 / 154 | 4598 / 154 | CONSISTENTES |
| 350 | SATURADO | 102 | 1.9 % | 25.4 | 11 553 | 30 / 0 | 89 | 1 368 | 766 | 8 % | 136 | 0 | 0 / 0 / 265 | 4053 / 265 | CONSISTENTES |
| 500 | SATURADO | 84 | 1.6 % | 24.2 | 18 570 | 28 / 0 | 92 | 1 187 | 637 | 6 % | 136 | 0 | 0 / 1 / 280 | 4514 / 281 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
