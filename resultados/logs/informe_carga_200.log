# Fase 3: 200 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

- procesador: AMD EPYC 9V74 80-Core Processor; hilos por núcleo: 2; núcleos por socket: 2
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
| Peticiones hechas | 14 411 |
| Peticiones correctas (2xx) | 14 370 |
| Errores (%) | 0.3 % |
| Ritmo medio (peticiones/s) | 35.2 |
| Tiempo mediano (ms) | 55 |
| Tiempo p95 (ms) | 1 360 |
| CPU del backend, media (% del límite de 2 CPU) | 34 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 57 |
| Memoria pico (MB, de 4096) | 1 439 |
| Sentencias SQL | 394 092 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 5161; fallidas: 41.
- Acciones hechas: panel 751, agenda 550, historia 487, reportes 64, recepcion 698, atencion 355, cobro 212, cancelar 129; sin trabajo disponible: 507.
- CPU del backend: 34 % de media y 64 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 57 % de media y 105 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 439 MB (35 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **200 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 4.3 s.
- Correo: el servidor de pruebas recibió 698 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.7 %).
- GC: 374 pausas, 40.0 s en total (10 % del tiempo de vida de la JVM); 37 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 833 MB (tope -Xmx2g).
- Clases cargadas: 25587 en total, 179 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 29 | 29 | 6832 |
| 0.1 | 59 | 59 | 12607 |
| 0.1 | 81 | 82 | 16164 |
| 0.2 | 81 | 82 | 16205 |
| 0.2 | 87 | 88 | 17214 |
| 0.5 | 118 | 120 | 22681 |
| 1.6 | 130 | 133 | 24782 |
| 2.4 | 133 | 135 | 25180 |
| 3.0 | 134 | 136 | 25339 |
| 3.7 | 134 | 137 | 25425 |
| 4.2 | 135 | 138 | 25483 |
| 5.0 | 135 | 138 | 25501 |
| 5.5 | 136 | 139 | 25541 |
| 6.4 | 136 | 139 | 25568 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 169 |
| `java.lang.invoke.LambdaForm$DMH/` | 7 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 176 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 698 | 698 | coincide |
| Mascotas | 698 | 698 | coincide |
| Citas agendadas | 687 | 687 | coincide |
| Diagnósticos | 325 | 325 | coincide |
| Recetas | 325 | 325 | coincide |
| Pagos | 212 | 212 | coincide |
| Citas completadas | 325 | 325 | coincide |
| Citas canceladas | 129 | 129 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 698 | 0 | 0.0 % | 89 | 1 420 |
| Registrar mascota | 698 | 0 | 0.0 % | 43 | 1 204 |
| Agendar cita | 687 | 11 | 1.6 % | 59 | 1 188 |
| Recepción (llegada) | 355 | 0 | 0.0 % | 39 | 1 066 |
| Iniciar atención | 325 | 30 | 8.5 % | 52 | 1 089 |
| Leer la consulta | 325 | 0 | 0.0 % | 28 | 628 |
| Registrar diagnóstico | 325 | 0 | 0.0 % | 28 | 517 |
| Registrar receta | 325 | 0 | 0.0 % | 29 | 617 |
| Actualizar mascota | 325 | 0 | 0.0 % | 29 | 519 |
| Cerrar la consulta | 325 | 0 | 0.0 % | 47 | 581 |
| Caja: crear punto de cobro | 144 | 0 | 0.0 % | 34 | 1 098 |
| Caja: vincular equipo | 144 | 0 | 0.0 % | 25 | 1 138 |
| Caja: abrir la caja | 144 | 0 | 0.0 % | 36 | 1 209 |
| Cobrar | 212 | 0 | 0.0 % | 48 | 1 099 |
| Cancelar cita | 129 | 0 | 0.0 % | 49 | 802 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 951 | 0 (0 %) | 682 | 2 113 | 19 % |
| reporte_clinico | 64 | 0 (0 %) | 4 644 | 8 046 | 7 % |
| apoderado | 698 | 0 (0 %) | 89 | 1 420 | 6 % |
| privacidad | 951 | 0 (0 %) | 91 | 843 | 5 % |
| lista_de_historias | 487 | 0 (0 %) | 186 | 1 607 | 5 % |
| propietarios | 550 | 0 (0 %) | 176 | 1 285 | 5 % |
| navegacion | 951 | 0 (0 %) | 20 | 1 031 | 5 % |
| cita | 698 | 11 (2 %) | 59 | 1 188 | 4 % |
| citas_del_mes | 550 | 0 (0 %) | 71 | 1 257 | 4 % |
| mascota | 698 | 0 (0 %) | 43 | 1 204 | 4 % |
| cumplimiento | 951 | 0 (0 %) | 22 | 862 | 4 % |
| notificaciones | 951 | 0 (0 %) | 14 | 736 | 3 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 6 | 42 | 502 | 0 % |
| 0.1 | 20 | 116 | 295 | 0 % |
| 0.4 | 35 | 192 | 315 | 1 % |
| 0.6 | 49 | 239 | 281 | 0 % |
| 0.9 | 63 | 291 | 273 | 0 % |
| 1.1 | 77 | 380 | 243 | 0 % |
| 1.4 | 92 | 405 | 281 | 0 % |
| 1.6 | 106 | 509 | 432 | 0 % |
| 1.9 | 120 | 459 | 274 | 0 % |
| 2.1 | 135 | 591 | 242 | 0 % |
| 2.4 | 149 | 656 | 891 | 0 % |
| 2.6 | 163 | 611 | 809 | 0 % |
| 2.9 | 177 | 758 | 1 708 | 1 % |
| 3.1 | 192 | 693 | 1 432 | 0 % |
| 3.4 | 200 | 795 | 1 402 | 0 % |
| 3.6 | 200 | 703 | 890 | 0 % |
| 3.9 | 200 | 801 | 1 201 | 0 % |
| 4.1 | 200 | 765 | 222 | 0 % |
| 4.4 | 200 | 710 | 806 | 0 % |
| 4.6 | 200 | 787 | 1 065 | 1 % |
| 4.9 | 200 | 513 | 3 270 | 0 % |
| 5.1 | 200 | 714 | 4 274 | 1 % |
| 5.4 | 200 | 712 | 587 | 0 % |
| 5.6 | 200 | 786 | 397 | 0 % |
| 5.9 | 200 | 720 | 1 021 | 1 % |
| 6.1 | 186 | 423 | 324 | 0 % |
| 6.4 | 108 | 40 | 123 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
