# Fase 3d: panel optimizado frente al código anterior

- procesador: AMD EPYC 7763 64-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Pausas completas de GC | Tiempo en GC | CPU PostgreSQL media (%) | Sentencias SQL por carga del panel | Panel p95 bajo carga (ms) | Tiempo total en PostgreSQL (s) | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 250 | código anterior | SATURADO | 1.1 % | 30.4 | 7 035 | 24 | 6.2 % | 79 | 61.6 | 9 329 | 1 794 | 0 | 4372 / 155 | CONSISTENTES |
| 250 | código nuevo | DEGRADADO | 0.4 % | 41.2 | 2 226 | 35 | 9.5 % | 42 | 53.1 | 1 645 | 378 | 0 | 6499 / 67 | CONSISTENTES |
| 350 | código anterior | FALLA | 31.5 % | 14.2 | 30 001 | 12 | 2.0 % | 34 | 61.8 | 30 001 | 780 | 2820 | 1612 / 504 | CONSISTENTES |
| 350 | código nuevo | FALLA | 15.0 % | 24.8 | 30 001 | 27 | 5.1 % | 25 | 52.8 | 30 001 | 182 | 1773 | 4040 / 491 | CONSISTENTES |
| 500 | código anterior | FALLA | 50.5 % | 13.3 | 30 001 | 17 | 1.7 % | 21 | 61.6 | 30 001 | 658 | 4716 | 1412 / 547 | CONSISTENTES |
| 500 | código nuevo | FALLA | 26.1 % | 23.1 | 30 001 | 32 | 4.8 % | 20 | 52.6 | 30 001 | 191 | 3272 | 4036 / 791 | CONSISTENTES |

> Mismo servidor, misma imagen y misma base de partida en cada corrida; solo cambia la bandera app.dashboard.consultas-optimizadas. El orden alterna por nivel. Una corrida por variante y nivel. Detalle, consultas más costosas y comparación de respuestas en INFORME_CARGA_<nivel>.md.
