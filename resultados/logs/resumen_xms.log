# Fase 3c: índice en consentimiento_datos, cada corrida desde la misma base

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| Usuarios | Variante | Veredicto | Errores | Peticiones/s | p95 (ms) | Pausas completas de GC | Tiempo en GC | CPU PostgreSQL media (%) | Tiempo SQL en consentimiento_datos (s) | Media por llamada (ms) | Filas de la tabla | Avisos de pool agotado | Escrituras correctas / fallidas | Datos |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 200 | sin índice | DEGRADADO | 0.7 % | 31.5 | 2 975 | 24 | 5.6 % | 72 | 1.5 | 0.3 | 1 286 | 0 | 4380 / 87 | CONSISTENTES |
| 200 | con índice | DEGRADADO | 0.6 % | 32.7 | 2 317 | 28 | 6.5 % | 72 | 0.3 | 0.1 | 1 380 | 0 | 4580 / 80 | CONSISTENTES |
| 250 | sin índice | DEGRADADO | 1.0 % | 34.0 | 4 117 | 29 | 6.7 % | 76 | 12.9 | 2.2 | 1 564 | 0 | 4754 / 149 | CONSISTENTES |
| 250 | con índice | SATURADO | 1.3 % | 30.7 | 5 261 | 15 | 4.3 % | 74 | 0.3 | 0.1 | 1 464 | 0 | 4270 / 182 | CONSISTENTES |
| 350 | sin índice | SATURADO | 1.3 % | 38.2 | 8 545 | 35 | 7.0 % | 76 | 34.6 | 4.2 | 2 194 | 0 | 6544 / 282 | CONSISTENTES |
| 350 | con índice | SATURADO | 1.4 % | 35.2 | 10 799 | 32 | 6.8 % | 77 | 0.5 | 0.1 | 2 128 | 0 | 5939 / 263 | CONSISTENTES |

> Cada corrida parte de la misma base copiada de una plantilla; el orden alterna por nivel. Ambas variantes usan -Xms = -Xmx. Una corrida por variante y nivel. Las consultas más costosas y los planes de cada nivel están en INFORME_CARGA_<nivel>.md.
