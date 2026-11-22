# Fase 3: 250 usuarios simultáneos, 2 CPU, 2 GB, heap 1g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 2 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 196 |
| Peticiones hechas | 14 129 |
| Peticiones correctas (2xx) | 13 983 |
| Errores (%) | 1.0 % |
| Ritmo medio (peticiones/s) | 31.5 |
| Tiempo mediano (ms) | 829 |
| Tiempo p95 (ms) | 5 231 |
| CPU del backend, media (% del límite de 2 CPU) | 32 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 73 |
| Memoria pico (MB, de 2048) | 1 334 |
| Sentencias SQL | 393 668 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4619; fallidas: 146.
- Acciones hechas: panel 681, agenda 590, historia 514, reportes 72, recepcion 755, atencion 363, cobro 147, cancelar 143; sin trabajo disponible: 553.
- CPU del backend: 32 % de media y 87 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 73 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 334 MB (65 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **196 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 8.9 s.
- Correo: el servidor de pruebas recibió 755 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 414 pausas, 45.1 s en total (10 % del tiempo de vida de la JVM); 41 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 971 MB (tope -Xmx1g).
- Clases cargadas: 25555 en total, 153 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 19 | 20 | 4981 |
| 0.1 | 35 | 35 | 7981 |
| 0.1 | 72 | 72 | 14687 |
| 0.2 | 81 | 82 | 16194 |
| 0.2 | 86 | 87 | 17116 |
| 0.4 | 113 | 115 | 22023 |
| 1.5 | 130 | 133 | 24772 |
| 2.1 | 132 | 135 | 25018 |
| 2.7 | 133 | 136 | 25319 |
| 3.6 | 134 | 138 | 25417 |
| 4.1 | 135 | 138 | 25427 |
| 5.0 | 135 | 138 | 25495 |
| 6.0 | 136 | 139 | 25496 |
| 7.2 | 136 | 139 | 25555 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 146 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 151 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 755 | 755 | coincide |
| Mascotas | 755 | 755 | coincide |
| Citas agendadas | 746 | 746 | coincide |
| Diagnósticos | 226 | 226 | coincide |
| Recetas | 226 | 226 | coincide |
| Pagos | 147 | 147 | coincide |
| Citas completadas | 226 | 226 | coincide |
| Citas canceladas | 143 | 143 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 755 | 0 | 0.0 % | 1 065 | 5 594 |
| Registrar mascota | 755 | 0 | 0.0 % | 1 073 | 4 960 |
| Agendar cita | 746 | 9 | 1.2 % | 990 | 4 925 |
| Recepción (llegada) | 363 | 0 | 0.0 % | 1 512 | 5 405 |
| Iniciar atención | 226 | 137 | 37.7 % | 1 709 | 5 924 |
| Leer la consulta | 226 | 0 | 0.0 % | 163 | 3 739 |
| Registrar diagnóstico | 226 | 0 | 0.0 % | 137 | 4 006 |
| Registrar receta | 226 | 0 | 0.0 % | 193 | 3 975 |
| Actualizar mascota | 226 | 0 | 0.0 % | 170 | 4 239 |
| Cerrar la consulta | 226 | 0 | 0.0 % | 190 | 4 009 |
| Caja: crear punto de cobro | 118 | 0 | 0.0 % | 1 143 | 4 159 |
| Caja: vincular equipo | 118 | 0 | 0.0 % | 573 | 4 266 |
| Caja: abrir la caja | 118 | 0 | 0.0 % | 988 | 4 875 |
| Cobrar | 147 | 0 | 0.0 % | 1 240 | 6 073 |
| Cancelar cita | 143 | 0 | 0.0 % | 1 687 | 4 627 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 931 | 0 (0 %) | 1 575 | 6 017 | 9 % |
| privacidad | 931 | 0 (0 %) | 951 | 4 945 | 7 % |
| apoderado | 755 | 0 (0 %) | 1 065 | 5 594 | 6 % |
| citas_del_mes | 590 | 0 (0 %) | 1 548 | 6 316 | 5 % |
| cita | 755 | 9 (1 %) | 990 | 4 925 | 5 % |
| propietarios | 590 | 0 (0 %) | 1 566 | 5 569 | 5 % |
| navegacion | 931 | 0 (0 %) | 386 | 4 639 | 5 % |
| mascota | 755 | 0 (0 %) | 1 073 | 4 960 | 5 % |
| cumplimiento | 931 | 0 (0 %) | 299 | 4 327 | 5 % |
| notificaciones | 931 | 0 (0 %) | 299 | 4 156 | 5 % |
| empleados | 590 | 0 (0 %) | 1 319 | 5 442 | 5 % |
| avisos_de_cumplimiento | 931 | 0 (0 %) | 358 | 3 876 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 1 | 1 | 935 | 0 % |
| 0.3 | 31 | 165 | 286 | 0 % |
| 0.8 | 61 | 261 | 279 | 0 % |
| 1.3 | 91 | 386 | 289 | 0 % |
| 1.8 | 121 | 501 | 537 | 0 % |
| 2.3 | 151 | 601 | 385 | 0 % |
| 2.8 | 181 | 707 | 909 | 0 % |
| 3.3 | 211 | 498 | 2 713 | 0 % |
| 3.8 | 241 | 669 | 2 361 | 0 % |
| 4.3 | 250 | 428 | 8 852 | 2 % |
| 4.8 | 250 | 485 | 6 530 | 2 % |
| 5.3 | 250 | 497 | 3 791 | 3 % |
| 5.8 | 250 | 557 | 4 690 | 2 % |
| 6.3 | 250 | 558 | 3 825 | 3 % |
| 6.8 | 238 | 517 | 3 526 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
