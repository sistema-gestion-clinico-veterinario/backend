# Fase 3: 200 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Usuarios sin degradación | 200 |
| Peticiones hechas | 15 108 |
| Peticiones correctas (2xx) | 15 083 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 37.1 |
| Tiempo mediano (ms) | 20 |
| Tiempo p95 (ms) | 273 |
| CPU del backend, media (% del límite de 2 CPU) | 34 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 44 |
| Memoria pico (MB, de 4096) | 1 084 |
| Sentencias SQL | 416 361 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5529; fallidas: 25.
- Acciones hechas: panel 775, agenda 615, historia 476, reportes 77, recepcion 728, atencion 363, cobro 250, cancelar 137; sin trabajo disponible: 556.
- CPU del backend: 34 % de media y 86 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 44 % de media y 100 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 084 MB (26 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **200 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.5 s.
- Correo: el servidor de pruebas recibió 728 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 446 pausas, 40.2 s en total (10 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 569 MB (tope -Xmx2g).
- Clases cargadas: 25573 en total, 212 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1188 |
| 0.1 | 33 | 34 | 7581 |
| 0.2 | 73 | 74 | 14883 |
| 0.2 | 81 | 82 | 16196 |
| 0.3 | 87 | 88 | 17215 |
| 0.8 | 119 | 121 | 22861 |
| 1.7 | 131 | 133 | 24767 |
| 2.3 | 132 | 135 | 25009 |
| 3.0 | 133 | 136 | 25312 |
| 3.5 | 134 | 137 | 25366 |
| 4.3 | 135 | 138 | 25469 |
| 4.8 | 135 | 138 | 25486 |
| 5.4 | 136 | 139 | 25526 |
| 6.1 | 136 | 139 | 25571 |
| 6.9 | 136 | 139 | 25573 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 201 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 208 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 728 | 728 | coincide |
| Mascotas | 728 | 728 | coincide |
| Citas agendadas | 707 | 707 | coincide |
| Diagnósticos | 359 | 359 | coincide |
| Recetas | 359 | 359 | coincide |
| Pagos | 250 | 250 | coincide |
| Citas completadas | 359 | 359 | coincide |
| Citas canceladas | 137 | 137 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 728 | 0 | 0.0 % | 37 | 302 |
| Registrar mascota | 728 | 0 | 0.0 % | 20 | 80 |
| Agendar cita | 707 | 21 | 2.9 % | 27 | 94 |
| Recepción (llegada) | 363 | 0 | 0.0 % | 15 | 289 |
| Iniciar atención | 359 | 4 | 1.1 % | 22 | 115 |
| Leer la consulta | 359 | 0 | 0.0 % | 12 | 58 |
| Registrar diagnóstico | 359 | 0 | 0.0 % | 12 | 62 |
| Registrar receta | 359 | 0 | 0.0 % | 13 | 72 |
| Actualizar mascota | 359 | 0 | 0.0 % | 12 | 47 |
| Cerrar la consulta | 359 | 0 | 0.0 % | 21 | 71 |
| Caja: crear punto de cobro | 154 | 0 | 0.0 % | 13 | 129 |
| Caja: vincular equipo | 154 | 0 | 0.0 % | 12 | 59 |
| Caja: abrir la caja | 154 | 0 | 0.0 % | 17 | 102 |
| Cobrar | 250 | 0 | 0.0 % | 21 | 131 |
| Cancelar cita | 137 | 0 | 0.0 % | 20 | 155 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 975 | 0 (0 %) | 179 | 893 | 27 % |
| reporte_clinico | 77 | 0 (0 %) | 2 174 | 3 768 | 17 % |
| login | 200 | 0 (0 %) | 280 | 808 | 7 % |
| propietarios | 615 | 0 (0 %) | 73 | 291 | 6 % |
| apoderado | 728 | 0 (0 %) | 37 | 302 | 5 % |
| privacidad | 975 | 0 (0 %) | 29 | 143 | 5 % |
| citas_del_mes | 615 | 0 (0 %) | 29 | 260 | 4 % |
| lista_de_historias | 476 | 0 (0 %) | 43 | 247 | 4 % |
| navegacion | 975 | 0 (0 %) | 11 | 140 | 3 % |
| cita | 728 | 21 (3 %) | 27 | 94 | 3 % |
| mascota | 728 | 0 (0 %) | 20 | 80 | 2 % |
| checkin | 363 | 0 (0 %) | 15 | 289 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 13 | 103 | 299 | 0 % |
| 0.2 | 27 | 152 | 276 | 1 % |
| 0.5 | 41 | 210 | 263 | 0 % |
| 0.7 | 56 | 274 | 259 | 0 % |
| 1.0 | 70 | 292 | 308 | 0 % |
| 1.2 | 84 | 380 | 332 | 0 % |
| 1.5 | 98 | 417 | 253 | 0 % |
| 1.7 | 113 | 479 | 276 | 1 % |
| 2.0 | 127 | 528 | 308 | 0 % |
| 2.2 | 141 | 640 | 138 | 0 % |
| 2.5 | 156 | 708 | 284 | 0 % |
| 2.7 | 170 | 695 | 296 | 0 % |
| 3.0 | 184 | 736 | 225 | 0 % |
| 3.2 | 198 | 818 | 251 | 0 % |
| 3.5 | 200 | 780 | 197 | 0 % |
| 3.7 | 200 | 855 | 339 | 0 % |
| 4.0 | 200 | 789 | 536 | 0 % |
| 4.2 | 200 | 787 | 120 | 0 % |
| 4.5 | 200 | 807 | 196 | 0 % |
| 4.7 | 200 | 801 | 415 | 0 % |
| 5.0 | 200 | 820 | 308 | 0 % |
| 5.2 | 200 | 735 | 169 | 0 % |
| 5.5 | 200 | 717 | 143 | 0 % |
| 5.7 | 200 | 738 | 548 | 0 % |
| 6.0 | 200 | 631 | 179 | 0 % |
| 6.2 | 154 | 216 | 97 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
