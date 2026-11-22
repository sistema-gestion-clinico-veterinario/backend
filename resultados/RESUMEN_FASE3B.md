# Fase 3b: efecto de -Xms = -Xmx (1 CPU, 2 GB) y consultas más costosas

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Heap en la 1ª pausa (MB) | Pausas de GC | Pausas completas | Tiempo en pausas completas (s) | Tiempo en GC | Heap tras GC máx (MB) | CPU PostgreSQL media (%) | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 200 | sin -Xms | AGUANTA | 0.5 % | 33.7 | 1 997 | 30 | 392 | 38 | 12.2 | 7.2 % | 827 | 64 | 0 | 4933 / 72 | CONSISTENTES |
| 200 | con -Xms | AGUANTA | 0.1 % | 37.0 | 382 | 989 | 220 | 29 | 11.4 | 7.6 % | 970 | 51 | 0 | 5493 / 21 | CONSISTENTES |
| 250 | sin -Xms | DEGRADADO | 1.2 % | 32.3 | 4 138 | 30 | 410 | 40 | 14.0 | 7.7 % | 940 | 78 | 0 | 4692 / 169 | CONSISTENTES |
| 250 | con -Xms | DEGRADADO | 0.7 % | 38.3 | 2 714 | 989 | 226 | 30 | 13.9 | 7.3 % | 955 | 64 | 0 | 5820 / 127 | CONSISTENTES |
| 350 | sin -Xms | SATURADO | 1.5 % | 32.7 | 9 405 | 30 | 483 | 45 | 16.4 | 7.8 % | 953 | 83 | 0 | 5332 / 272 | CONSISTENTES |
| 350 | con -Xms | SATURADO | 1.7 % | 30.7 | 10 113 | 989 | 249 | 31 | 12.8 | 6.5 % | 977 | 84 | 0 | 4986 / 286 | CONSISTENTES |

> Las dos variantes corren en la misma máquina, una detrás de otra, alternando cuál va primero en cada nivel. Una corrida por variante y nivel. Las consultas más costosas de cada nivel están en su informe INFORME_CARGA_<nivel>.md.
