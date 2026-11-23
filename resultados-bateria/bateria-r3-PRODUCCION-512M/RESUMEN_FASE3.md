# Fase 3: escalado vertical, 1 CPU, 512 MB, heap 192m

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 192m; núcleos de k6: 2; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 512m de RAM
- perfil del laboratorio: PRODUCCION-512M (memoria 512m, CPU 1, heap máximo 192m; configuración de la JVM en los niveles: P, en el control: P)
- usadas en esta corrida: P para los niveles y P para el control (perfil PRODUCCION-512M)
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Veredicto | Sin degradación hasta (usuarios) | Errores | Peticiones/s | p95 (ms) | p99 (ms) | CPU backend: media / al tope (% del límite) | CPU PostgreSQL media (%) | Memoria pico (MB) | Heap tras GC máx (MB) | Tiempo en GC | Metaspace pico (MB) | Avisos de pool agotado | Fallos: sin respuesta / 5xx / 4xx | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 100 | FALLA | 11 | 69.4 % | 2.3 | 30 001 | 30 001 | 19 / 16 | 1 | 423 | 185 | 17 % | 109 | 52 | 551 / 0 / 0 | 53 / 32 | CONSISTENTES |
| 250 | FALLA | 25 | 92.8 % | 3.9 | 30 001 | 30 001 | 19 / 17 | 1 | 424 | 185 | 63 % | 109 | 45 | 1766 / 0 / 0 | 11 / 55 | CONSISTENTES |

> "Sin degradación hasta" = usuarios del último tramo de 15 s antes del primero con 1 % de errores o p95 de 2 s. Los 4xx del laboratorio son reglas de negocio (solo hay 10 veterinarios). Una corrida por nivel y por configuración.
