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
| 200 | sin índice | DEGRADADO | 0.7 % | 29.8 | 3 659 | 27 | 6.8 % | 72 | 3.0 | 0.7 | 1 254 | 0 | 4063 / 89 | CONSISTENTES |
| 200 | con índice | DEGRADADO | 0.6 % | 32.3 | 2 422 | 22 | 4.9 % | 69 | 0.4 | 0.1 | 1 410 | 0 | 4651 / 74 | CONSISTENTES |
| 250 | sin índice | DEGRADADO | 1.3 % | 32.9 | 4 370 | 33 | 7.9 % | 75 | 11.8 | 2.0 | 1 618 | 0 | 4971 / 189 | CONSISTENTES |
| 250 | con índice | SATURADO | 1.2 % | 31.4 | 5 139 | 29 | 7.1 % | 76 | 0.4 | 0.1 | 1 526 | 0 | 4403 / 169 | CONSISTENTES |
| 350 | sin índice | SATURADO | 1.3 % | 34.4 | 10 306 | 30 | 6.4 % | 80 | 25.1 | 3.3 | 2 058 | 0 | 5629 / 251 | CONSISTENTES |
| 350 | con índice | SATURADO | 1.4 % | 35.7 | 8 969 | 35 | 7.7 % | 79 | 0.5 | 0.1 | 2 176 | 0 | 6176 / 280 | CONSISTENTES |

> Cada corrida parte de la misma base copiada de una plantilla; el orden alterna por nivel. Ambas variantes usan -Xms = -Xmx. Una corrida por variante y nivel. Las consultas más costosas y los planes de cada nivel están en INFORME_CARGA_<nivel>.md.
