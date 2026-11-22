# Fase 3b: efecto de -Xms = -Xmx (1 CPU, 2 GB) y consultas más costosas

- procesador: AMD EPYC 9V45 96-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Heap en la 1ª pausa (MB) | Pausas de GC | Pausas completas | Tiempo en pausas completas (s) | Tiempo en GC | Heap tras GC máx (MB) | CPU PostgreSQL media (%) | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 200 | sin -Xms | AGUANTA | 0.2 % | 36.3 | 601 | 30 | 405 | 39 | 11.4 | 8.8 % | 696 | 52 | 0 | 5486 / 34 | CONSISTENTES |
| 200 | con -Xms | AGUANTA | 0.1 % | 37.9 | 169 | 989 | 185 | 23 | 6.5 | 5.6 % | 971 | 36 | 0 | 5714 / 18 | CONSISTENTES |
| 250 | sin -Xms | DEGRADADO | 1.1 % | 33.3 | 4 012 | 30 | 473 | 52 | 18.2 | 10.0 % | 952 | 74 | 0 | 4997 / 158 | CONSISTENTES |
| 250 | con -Xms | DEGRADADO | 0.6 % | 39.5 | 2 316 | 989 | 233 | 31 | 11.7 | 7.6 % | 981 | 60 | 0 | 6165 / 102 | CONSISTENTES |
| 350 | sin -Xms | SATURADO | 1.6 % | 35.1 | 7 883 | 30 | 455 | 40 | 13.3 | 7.3 % | 989 | 81 | 0 | 5801 / 303 | CONSISTENTES |
| 350 | con -Xms | SATURADO | 1.5 % | 34.3 | 8 174 | 989 | 245 | 28 | 8.9 | 6.0 % | 971 | 83 | 0 | 5758 / 286 | CONSISTENTES |

> Las dos variantes corren en la misma máquina, una detrás de otra, alternando cuál va primero en cada nivel. Una corrida por variante y nivel. Las consultas más costosas de cada nivel están en su informe INFORME_CARGA_<nivel>.md.
