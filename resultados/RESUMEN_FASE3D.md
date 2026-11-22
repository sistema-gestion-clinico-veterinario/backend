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
| 250 | código anterior | SATURADO | 1.1 % | 31.3 | 5 245 | 20 | 5.8 % | 76 | 62.5 | 7 649 | 1 856 | 0 | 4316 / 155 | CONSISTENTES |
| 250 | código nuevo | AGUANTA | 0.1 % | 45.0 | 165 | 28 | 6.9 % | 37 | 52.6 | 299 | 200 | 0 | 7574 / 22 | CONSISTENTES |
| 350 | código anterior | SATURADO | 1.3 % | 36.6 | 10 128 | 34 | 6.8 % | 79 | 61.9 | 12 368 | 2 116 | 0 | 6149 / 257 | CONSISTENTES |
| 350 | código nuevo | AGUANTA | 0.5 % | 56.0 | 1 259 | 53 | 12.0 % | 46 | 52.6 | 1 196 | 547 | 0 | 10999 / 164 | CONSISTENTES |
| 500 | código anterior | FALLA | 30.1 % | 20.0 | 30 001 | 21 | 2.3 % | 34 | 61.8 | 30 001 | 1 046 | 3080 | 3132 / 742 | CONSISTENTES |
| 500 | código nuevo | SATURADO | 1.5 % | 52.0 | 9 078 | 76 | 13.8 % | 53 | 52.6 | 7 885 | 1 214 | 0 | 11467 / 528 | CONSISTENTES |

> Mismo servidor, misma imagen y misma base de partida en cada corrida; solo cambia la bandera app.dashboard.consultas-optimizadas. El orden alterna por nivel. Una corrida por variante y nivel. Detalle, consultas más costosas y comparación de respuestas en INFORME_CARGA_<nivel>.md.
