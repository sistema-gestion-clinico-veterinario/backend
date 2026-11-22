# Fase 3: 150 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 2g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 4g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 43 |
| Peticiones hechas | 10 342 |
| Peticiones correctas (2xx) | 10 330 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 28.8 |
| Tiempo mediano (ms) | 18 |
| Tiempo p95 (ms) | 272 |
| CPU del backend, media (% del límite de 2 CPU) | 25 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 42 |
| Memoria pico (MB, de 4096) | 1 223 |
| Sentencias SQL | 285 556 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 3850; fallidas: 12.
- Acciones hechas: panel 516, agenda 412, historia 326, reportes 40, recepcion 519, atencion 247, cobro 160, cancelar 102; sin trabajo disponible: 398.
- CPU del backend: 25 % de media y 70 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 42 % de media y 107 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 223 MB (30 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **43 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.8 s.
- Correo: el servidor de pruebas recibió 519 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.6 %).
- GC: 296 pausas, 21.7 s en total (6 % del tiempo de vida de la JVM); 29 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 659 MB (tope -Xmx2g).
- Clases cargadas: 25536 en total, 219 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1188 |
| 0.0 | 26 | 26 | 6043 |
| 0.1 | 46 | 46 | 10117 |
| 0.1 | 71 | 72 | 14613 |
| 0.2 | 81 | 82 | 16163 |
| 0.2 | 82 | 83 | 16462 |
| 0.3 | 87 | 88 | 17224 |
| 0.9 | 126 | 128 | 24181 |
| 2.0 | 131 | 134 | 24978 |
| 2.5 | 133 | 136 | 25171 |
| 3.0 | 134 | 137 | 25353 |
| 3.4 | 134 | 137 | 25370 |
| 4.3 | 135 | 138 | 25493 |
| 4.9 | 135 | 138 | 25495 |
| 5.4 | 135 | 138 | 25535 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 211 |
| `java.lang.invoke.LambdaForm$DMH/` | 6 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 217 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 519 | 519 | coincide |
| Mascotas | 519 | 519 | coincide |
| Citas agendadas | 509 | 509 | coincide |
| Diagnósticos | 245 | 245 | coincide |
| Recetas | 245 | 245 | coincide |
| Pagos | 160 | 160 | coincide |
| Citas completadas | 245 | 245 | coincide |
| Citas canceladas | 102 | 102 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 519 | 0 | 0.0 % | 31 | 192 |
| Registrar mascota | 519 | 0 | 0.0 % | 18 | 65 |
| Agendar cita | 509 | 10 | 1.9 % | 24 | 75 |
| Recepción (llegada) | 247 | 0 | 0.0 % | 15 | 133 |
| Iniciar atención | 245 | 2 | 0.8 % | 20 | 82 |
| Leer la consulta | 245 | 0 | 0.0 % | 12 | 34 |
| Registrar diagnóstico | 245 | 0 | 0.0 % | 10 | 38 |
| Registrar receta | 245 | 0 | 0.0 % | 11 | 36 |
| Actualizar mascota | 245 | 0 | 0.0 % | 11 | 33 |
| Cerrar la consulta | 245 | 0 | 0.0 % | 19 | 87 |
| Caja: crear punto de cobro | 108 | 0 | 0.0 % | 12 | 76 |
| Caja: vincular equipo | 108 | 0 | 0.0 % | 10 | 44 |
| Caja: abrir la caja | 108 | 0 | 0.0 % | 11 | 55 |
| Cobrar | 160 | 0 | 0.0 % | 15 | 103 |
| Cancelar cita | 102 | 0 | 0.0 % | 15 | 145 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 666 | 0 (0 %) | 191 | 2 194 | 41 % |
| reporte_clinico | 40 | 0 (0 %) | 1 856 | 4 461 | 12 % |
| login | 150 | 0 (0 %) | 281 | 565 | 7 % |
| apoderado | 519 | 0 (0 %) | 31 | 192 | 4 % |
| propietarios | 412 | 0 (0 %) | 43 | 172 | 4 % |
| citas_del_mes | 412 | 0 (0 %) | 30 | 212 | 4 % |
| lista_de_historias | 326 | 0 (0 %) | 34 | 257 | 3 % |
| cita | 519 | 10 (2 %) | 24 | 75 | 3 % |
| privacidad | 666 | 0 (0 %) | 14 | 74 | 2 % |
| navegacion | 666 | 0 (0 %) | 10 | 85 | 2 % |
| mascota | 519 | 0 (0 %) | 18 | 65 | 2 % |
| cumplimiento | 666 | 0 (0 %) | 9 | 41 | 1 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 6 | 785 | 0 % |
| 0.0 | 15 | 114 | 433 | 0 % |
| 0.3 | 29 | 152 | 288 | 0 % |
| 0.5 | 43 | 217 | 287 | 0 % |
| 0.8 | 58 | 285 | 288 | 1 % |
| 1.0 | 72 | 305 | 290 | 0 % |
| 1.3 | 86 | 382 | 287 | 0 % |
| 1.5 | 100 | 449 | 282 | 0 % |
| 1.8 | 114 | 470 | 261 | 0 % |
| 2.0 | 128 | 558 | 453 | 0 % |
| 2.3 | 142 | 637 | 422 | 0 % |
| 2.5 | 150 | 601 | 849 | 0 % |
| 2.8 | 150 | 551 | 748 | 0 % |
| 3.0 | 150 | 625 | 208 | 0 % |
| 3.3 | 150 | 572 | 106 | 0 % |
| 3.5 | 150 | 613 | 110 | 0 % |
| 3.8 | 150 | 594 | 107 | 0 % |
| 4.0 | 150 | 652 | 171 | 0 % |
| 4.3 | 150 | 597 | 178 | 1 % |
| 4.5 | 150 | 552 | 105 | 0 % |
| 4.8 | 150 | 594 | 168 | 0 % |
| 5.0 | 150 | 514 | 121 | 0 % |
| 5.3 | 141 | 247 | 91 | 0 % |
| 5.5 | 71 | 55 | 94 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
