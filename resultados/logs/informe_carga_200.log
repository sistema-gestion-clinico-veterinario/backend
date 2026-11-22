# Fase 3: 200 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

- procesador: Intel(R) Xeon(R) 6975P-C; hilos por núcleo: 2; núcleos por socket: 2
- versiones medidas: despues; heap máximo: 1g; núcleos de k6: 0; reservas de horario por usuario: 14
- recursos del backend: 1 CPU, 2g de RAM
- usadas en esta corrida: D (Metaspace 256 MB) para la escalera; E (Metaspace 128 MB, igual que el Dockerfile) para el control
- correo: servidor SMTP de pruebas en el contenedor sv-correo (sin TLS ni autenticación; los envíos terminan bien)
- pausa entre acciones: 15 s (con variación aleatoria)
- antes:   no medido
- despues: cb7f6bdb1865b9ca3b5e8aedc0a4ef6bcaf17e13

| | DESPUÉS |
|---|---|
| Veredicto | AGUANTA |
| Usuarios sin degradación | 134 |
| Peticiones hechas | 14 864 |
| Peticiones correctas (2xx) | 14 830 |
| Errores (%) | 0.2 % |
| Ritmo medio (peticiones/s) | 36.2 |
| Tiempo mediano (ms) | 24 |
| Tiempo p95 (ms) | 396 |
| CPU del backend, media (% del límite de 1 CPU) | 53 |
| Tiempo con la CPU al tope (%) | 8 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 41 |
| Memoria pico (MB, de 2048) | 1 313 |
| Sentencias SQL | 410 904 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5678; fallidas: 34.
- Acciones hechas: panel 695, agenda 603, historia 514, reportes 77, recepcion 727, atencion 385, cobro 261, cancelar 146; sin trabajo disponible: 597.
- CPU del backend: 53 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 8 % del tiempo.
- CPU de PostgreSQL: 41 % de media y 100 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 313 MB (64 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **134 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 2.4 s.
- Correo: el servidor de pruebas recibió 727 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 407 pausas, 36.2 s en total (9 % del tiempo de vida de la JVM); 41 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 959 MB (tope -Xmx1g).
- Clases cargadas: 25556 en total, 211 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 17 | 18 | 4408 |
| 0.1 | 36 | 36 | 8105 |
| 0.2 | 71 | 72 | 14598 |
| 0.3 | 80 | 81 | 16101 |
| 0.4 | 86 | 87 | 17104 |
| 0.5 | 109 | 110 | 21064 |
| 1.7 | 129 | 131 | 24486 |
| 2.4 | 132 | 135 | 25000 |
| 3.0 | 133 | 136 | 25293 |
| 3.5 | 134 | 137 | 25376 |
| 4.5 | 135 | 138 | 25452 |
| 5.2 | 135 | 138 | 25509 |
| 6.2 | 136 | 139 | 25537 |
| 6.8 | 136 | 139 | 25556 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 200 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 207 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |
| `com.fasterxml.jackson.databind` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 727 | 727 | coincide |
| Mascotas | 727 | 727 | coincide |
| Citas agendadas | 702 | 702 | coincide |
| Diagnósticos | 376 | 376 | coincide |
| Recetas | 376 | 376 | coincide |
| Pagos | 261 | 261 | coincide |
| Citas completadas | 376 | 376 | coincide |
| Citas canceladas | 146 | 146 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 727 | 0 | 0.0 % | 44 | 362 |
| Registrar mascota | 727 | 0 | 0.0 % | 24 | 129 |
| Agendar cita | 702 | 25 | 3.4 % | 33 | 138 |
| Recepción (llegada) | 385 | 0 | 0.0 % | 20 | 327 |
| Iniciar atención | 376 | 9 | 2.3 % | 27 | 160 |
| Leer la consulta | 376 | 0 | 0.0 % | 16 | 65 |
| Registrar diagnóstico | 376 | 0 | 0.0 % | 16 | 66 |
| Registrar receta | 376 | 0 | 0.0 % | 17 | 70 |
| Actualizar mascota | 376 | 0 | 0.0 % | 16 | 67 |
| Cerrar la consulta | 376 | 0 | 0.0 % | 29 | 154 |
| Caja: crear punto de cobro | 158 | 0 | 0.0 % | 17 | 446 |
| Caja: vincular equipo | 158 | 0 | 0.0 % | 15 | 69 |
| Caja: abrir la caja | 158 | 0 | 0.0 % | 20 | 163 |
| Cobrar | 261 | 0 | 0.0 % | 24 | 202 |
| Cancelar cita | 146 | 0 | 0.0 % | 25 | 316 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 895 | 0 (0 %) | 170 | 764 | 16 % |
| reporte_clinico | 77 | 0 (0 %) | 2 172 | 7 985 | 15 % |
| login | 200 | 0 (0 %) | 393 | 3 420 | 11 % |
| propietarios | 603 | 0 (0 %) | 83 | 297 | 5 % |
| apoderado | 727 | 0 (0 %) | 44 | 362 | 5 % |
| privacidad | 895 | 0 (0 %) | 29 | 146 | 4 % |
| citas_del_mes | 603 | 0 (0 %) | 30 | 471 | 4 % |
| navegacion | 895 | 0 (0 %) | 13 | 248 | 4 % |
| lista_de_historias | 514 | 0 (0 %) | 43 | 436 | 3 % |
| cita | 727 | 25 (3 %) | 33 | 138 | 3 % |
| mascota | 727 | 0 (0 %) | 24 | 129 | 3 % |
| cumplimiento | 895 | 0 (0 %) | 14 | 106 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 5 | 35 | 849 | 0 % |
| 0.1 | 19 | 113 | 337 | 0 % |
| 0.3 | 34 | 175 | 336 | 0 % |
| 0.6 | 48 | 231 | 301 | 0 % |
| 0.8 | 62 | 275 | 290 | 0 % |
| 1.1 | 76 | 352 | 339 | 1 % |
| 1.4 | 91 | 394 | 192 | 0 % |
| 1.6 | 105 | 467 | 210 | 0 % |
| 1.9 | 119 | 493 | 228 | 0 % |
| 2.1 | 134 | 596 | 213 | 0 % |
| 2.4 | 148 | 422 | 2 398 | 1 % |
| 2.6 | 162 | 731 | 1 986 | 0 % |
| 2.9 | 176 | 799 | 177 | 0 % |
| 3.1 | 191 | 816 | 288 | 0 % |
| 3.4 | 200 | 731 | 193 | 0 % |
| 3.6 | 200 | 805 | 130 | 0 % |
| 3.9 | 200 | 797 | 94 | 0 % |
| 4.1 | 200 | 766 | 397 | 0 % |
| 4.3 | 200 | 787 | 161 | 0 % |
| 4.6 | 200 | 833 | 166 | 0 % |
| 4.8 | 200 | 879 | 234 | 0 % |
| 5.1 | 200 | 677 | 96 | 0 % |
| 5.3 | 200 | 804 | 311 | 0 % |
| 5.6 | 200 | 718 | 138 | 0 % |
| 5.8 | 200 | 721 | 298 | 0 % |
| 6.1 | 192 | 393 | 651 | 1 % |
| 6.3 | 121 | 54 | 43 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
