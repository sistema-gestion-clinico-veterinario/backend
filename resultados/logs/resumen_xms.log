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
| 250 | código anterior | SATURADO | 1.2 % | 21.6 | 12 167 | 19 | 3.9 % | 84 | 61.6 | 16 441 | 2 304 | 0 | 2947 / 120 | CONSISTENTES |
| 250 | código nuevo | AGUANTA | 0.3 % | 42.7 | 1 066 | 35 | 9.7 % | 47 | 53.4 | 1 166 | 411 | 0 | 6968 / 54 | CONSISTENTES |
| 350 | código anterior | FALLA | 29.7 % | 14.8 | 30 001 | 17 | 2.1 % | 38 | 62.1 | 30 001 | 860 | 2786 | 1685 / 494 | CONSISTENTES |
| 350 | código nuevo | DEGRADADO | 1.2 % | 45.7 | 4 601 | 47 | 11.1 % | 55 | 52.6 | 3 795 | 814 | 0 | 8234 / 303 | CONSISTENTES |
| 500 | código anterior | FALLA | 40.6 % | 15.8 | 30 001 | 16 | 1.7 % | 35 | 61.6 | 30 001 | 942 | 4089 | 1891 / 689 | CONSISTENTES |
| 500 | código nuevo | FALLA | 24.5 % | 24.1 | 30 001 | 30 | 4.5 % | 21 | 52.6 | 30 001 | 208 | 3187 | 4383 / 755 | CONSISTENTES |

> Mismo servidor, misma imagen y misma base de partida en cada corrida; solo cambia la bandera app.dashboard.consultas-optimizadas. El orden alterna por nivel. Una corrida por variante y nivel. Detalle, consultas más costosas y comparación de respuestas en INFORME_CARGA_<nivel>.md.
