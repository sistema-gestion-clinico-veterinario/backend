# Fase 3: escalado vertical, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.1 % | 20.2 | 279 | 39 / 7 | 32 | 953 | 490 | 5 % | 134 | 0 | 0 / 0 / 5 | 2222 / 5 | CONSISTENTES |
| 150 | AGUANTA | 150 | 0.1 % | 28.3 | 633 | 52 / 12 | 54 | 1 115 | 622 | 8 % | 135 | 0 | 0 / 0 / 15 | 3618 / 15 | CONSISTENTES |
| 200 | AGUANTA | 134 | 0.2 % | 36.2 | 396 | 53 / 8 | 41 | 1 313 | 959 | 9 % | 136 | 0 | 0 / 0 / 34 | 5678 / 34 | CONSISTENTES |
| 250 | DEGRADADO | 167 | 1.2 % | 34.3 | 3 490 | 54 / 11 | 69 | 1 152 | 637 | 7 % | 136 | 0 | 0 / 0 / 187 | 4933 / 187 | CONSISTENTES |
| 350 | SATURADO | 154 | 1.7 % | 30.8 | 9 933 | 57 / 7 | 83 | 1 318 | 904 | 8 % | 136 | 0 | 0 / 0 / 293 | 5005 / 293 | CONSISTENTES |
| 500 | FALLA | 144 | 26.2 % | 21.0 | 30 001 | 38 / 6 | 45 | 1 265 | 845 | 4 % | 137 | 2627 | 3956 / 0 / 111 | 3162 / 810 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
