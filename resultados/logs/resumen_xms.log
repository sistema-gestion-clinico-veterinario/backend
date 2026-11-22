# Fase 3d: panel optimizado frente al código anterior

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: 98d2a79845070e6dda4899bb1483a344dd4b29b1

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Pausas completas de GC | Tiempo en GC | CPU PostgreSQL media (%) | Sentencias SQL por carga del panel | Panel p95 bajo carga (ms) | Tiempo total en PostgreSQL (s) | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 250 | código anterior | DEGRADADO | 1.3 % | 32.4 | 4 702 | 28 | 6.6 % | 76 | 62.2 | 7 369 | 1 720 | 0 | 4807 / 182 | CONSISTENTES |
| 250 | código nuevo | AGUANTA | 0.1 % | 44.9 | 188 | 29 | 7.3 % | 34 | 52.7 | 374 | 211 | 0 | 7433 / 25 | CONSISTENTES |
| 350 | código anterior | FALLA | 17.9 % | 20.6 | 30 001 | 25 | 4.2 % | 43 | 62.2 | 30 001 | 918 | 1615 | 3034 / 456 | CONSISTENTES |
| 350 | código nuevo | DEGRADADO | 1.7 % | 50.1 | 2 015 | 50 | 11.5 % | 44 | 53.2 | 1 522 | 509 | 44 | 9152 / 335 | CONSISTENTES |
| 500 | código anterior | FALLA | 29.9 % | 20.2 | 30 001 | 20 | 2.4 % | 37 | 61.7 | 30 001 | 1 135 | 3141 | 3214 / 761 | CONSISTENTES |
| 500 | código nuevo | FALLA | 10.1 % | 40.0 | 30 001 | 43 | 7.5 % | 33 | 52.6 | 30 001 | 413 | 904 | 8937 / 738 | CONSISTENTES |

> Mismo servidor, misma imagen y misma base de partida en cada corrida; solo cambia la bandera app.dashboard.consultas-optimizadas. El orden alterna por nivel. Una corrida por variante y nivel. Detalle, consultas más costosas y comparación de respuestas en INFORME_CARGA_<nivel>.md.
