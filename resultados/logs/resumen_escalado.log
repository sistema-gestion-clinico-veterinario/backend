# Fase 3: escalado vertical, 2 CPU, 4 GB, heap 2g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 2g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 4g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.1 % | 20.6 | 279 | 21 / 0 | 39 | 923 | 457 | 6 % | 134 | 0 | 0 / 0 / 5 | 2194 / 5 | CONSISTENTES |
| 150 | AGUANTA | 43 | 0.1 % | 28.8 | 272 | 25 / 0 | 42 | 1 223 | 659 | 6 % | 136 | 0 | 0 / 0 / 12 | 3850 / 12 | CONSISTENTES |
| 200 | AGUANTA | 200 | 0.2 % | 37.1 | 273 | 34 / 0 | 44 | 1 084 | 569 | 10 % | 136 | 0 | 0 / 0 / 25 | 5529 / 25 | CONSISTENTES |
| 250 | DEGRADADO | 201 | 1.0 % | 34.4 | 3 541 | 32 / 0 | 71 | 1 405 | 774 | 8 % | 136 | 0 | 0 / 0 / 153 | 5045 / 153 | CONSISTENTES |
| 350 | SATURADO | 169 | 1.6 % | 34.8 | 6 785 | 29 / 0 | 84 | 1 382 | 743 | 8 % | 136 | 0 | 0 / 0 / 298 | 5817 / 298 | CONSISTENTES |
| 500 | SATURADO | 144 | 1.9 % | 32.7 | 12 348 | 31 / 0 | 88 | 1 356 | 749 | 8 % | 137 | 0 | 0 / 0 / 452 | 6690 / 452 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
