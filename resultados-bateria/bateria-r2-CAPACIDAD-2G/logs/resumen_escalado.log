# Fase 3: escalado vertical, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- perfil del laboratorio: CAPACIDAD-2G (memoria 2g, CPU 1, heap máximo 1g; configuración de la JVM en los niveles: D, en el control: E)
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | p99 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | AGUANTA | 100 | 0.1 % | 20.5 | 257 | 655 | 45 / 2 | 31 | 865 | 423 | 5 % | 135 | 0 | 0 / 0 / 4 | 2363 / 4 | CONSISTENTES |
| 250 | FALLA | 152 | 20.9 % | 15.2 | 30 001 | 30 001 | 32 / 7 | 22 | 1 058 | 526 | 3 % | 135 | 2558 | 1463 / 0 / 7 | 1978 / 241 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
