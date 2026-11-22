# Fase 3: 350 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 169 |
| Peticiones hechas | 19 029 |
| Peticiones correctas (2xx) | 18 731 |
| Errores (%) | 1.6 % |
| Ritmo medio (peticiones/s) | 34.8 |
| Tiempo mediano (ms) | 2 271 |
| Tiempo p95 (ms) | 6 785 |
| CPU del backend, media (% del límite de 2 CPU) | 29 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 84 |
| Memoria pico (MB, de 4096) | 1 382 |
| Sentencias SQL | 541 265 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 5817; fallidas: 298.
- Acciones hechas: panel 939, agenda 869, historia 652, reportes 89, recepcion 1091, atencion 505, cobro 149, cancelar 206; sin trabajo disponible: 824.
- CPU del backend: 29 % de media y 76 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 84 % de media y 106 % de pico (de 1 núcleo).
- **Diagnóstico: el límite parece ser la base de datos** (PostgreSQL casi al tope y el backend con CPU de sobra).
- Memoria del contenedor: pico de 1 382 MB (34 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **169 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 9.3 s.
- Correo: el servidor de pruebas recibió 1091 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.0 %).
- GC: 477 pausas, 45.4 s en total (8 % del tiempo de vida de la JVM); 48 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 743 MB (tope -Xmx2g).
- Clases cargadas: 25559 en total, 129 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.1 | 35 | 35 | 7950 |
| 0.2 | 78 | 79 | 15928 |
| 0.2 | 81 | 82 | 16195 |
| 0.3 | 87 | 88 | 17219 |
| 1.0 | 126 | 128 | 24044 |
| 2.0 | 132 | 134 | 24999 |
| 2.7 | 133 | 136 | 25335 |
| 3.5 | 134 | 137 | 25405 |
| 4.7 | 135 | 138 | 25454 |
| 5.5 | 136 | 139 | 25497 |
| 6.5 | 136 | 139 | 25500 |
| 7.3 | 136 | 139 | 25503 |
| 8.1 | 136 | 139 | 25558 |
| 9.0 | 136 | 139 | 25559 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 121 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 126 |
| `org.apache.tomcat.util` | 1 |
| `com.fasterxml.jackson.databind` | 1 |
| `jdk.internal.math.FormattedFPDecimal` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 1091 | 1091 | coincide |
| Mascotas | 1091 | 1091 | coincide |
| Citas agendadas | 1071 | 1071 | coincide |
| Diagnósticos | 227 | 227 | coincide |
| Recetas | 227 | 227 | coincide |
| Pagos | 149 | 149 | coincide |
| Citas completadas | 227 | 227 | coincide |
| Citas canceladas | 206 | 206 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 1091 | 0 | 0.0 % | 2 293 | 6 798 |
| Registrar mascota | 1091 | 0 | 0.0 % | 2 345 | 6 700 |
| Agendar cita | 1071 | 20 | 1.8 % | 2 381 | 6 442 |
| Recepción (llegada) | 505 | 0 | 0.0 % | 2 650 | 6 874 |
| Iniciar atención | 227 | 278 | 55.0 % | 3 212 | 7 774 |
| Leer la consulta | 227 | 0 | 0.0 % | 1 384 | 5 962 |
| Registrar diagnóstico | 227 | 0 | 0.0 % | 1 231 | 6 095 |
| Registrar receta | 227 | 0 | 0.0 % | 1 455 | 5 917 |
| Actualizar mascota | 227 | 0 | 0.0 % | 1 383 | 6 428 |
| Cerrar la consulta | 227 | 0 | 0.0 % | 1 239 | 5 352 |
| Caja: crear punto de cobro | 114 | 0 | 0.0 % | 1 071 | 5 339 |
| Caja: vincular equipo | 114 | 0 | 0.0 % | 1 051 | 5 749 |
| Caja: abrir la caja | 114 | 0 | 0.0 % | 1 547 | 6 130 |
| Cobrar | 149 | 0 | 0.0 % | 1 690 | 5 954 |
| Cancelar cita | 206 | 0 | 0.0 % | 2 993 | 7 011 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1289 | 0 (0 %) | 2 980 | 7 670 | 8 % |
| privacidad | 1289 | 0 (0 %) | 2 651 | 6 284 | 7 % |
| apoderado | 1091 | 0 (0 %) | 2 293 | 6 798 | 6 % |
| mascota | 1091 | 0 (0 %) | 2 345 | 6 700 | 6 % |
| cita | 1091 | 20 (2 %) | 2 381 | 6 442 | 6 % |
| propietarios | 869 | 0 (0 %) | 3 139 | 7 735 | 6 % |
| avisos_de_cumplimiento | 1289 | 0 (0 %) | 1 728 | 6 013 | 6 % |
| notificaciones | 1289 | 0 (0 %) | 1 731 | 6 017 | 6 % |
| navegacion | 1289 | 0 (0 %) | 1 768 | 5 673 | 5 % |
| citas_del_mes | 869 | 0 (0 %) | 2 832 | 7 689 | 5 % |
| cumplimiento | 1289 | 0 (0 %) | 1 677 | 5 881 | 5 % |
| empleados | 869 | 0 (0 %) | 2 708 | 7 275 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 4 | 30 | 760 | 0 % |
| 0.3 | 34 | 176 | 291 | 0 % |
| 0.8 | 64 | 296 | 263 | 0 % |
| 1.3 | 94 | 454 | 420 | 0 % |
| 1.8 | 124 | 480 | 502 | 0 % |
| 2.3 | 154 | 531 | 1 436 | 1 % |
| 2.8 | 184 | 590 | 2 307 | 1 % |
| 3.3 | 214 | 674 | 2 025 | 0 % |
| 3.8 | 244 | 685 | 3 277 | 2 % |
| 4.3 | 274 | 587 | 5 329 | 2 % |
| 4.8 | 304 | 529 | 7 788 | 2 % |
| 5.3 | 334 | 699 | 4 688 | 3 % |
| 5.8 | 350 | 579 | 8 031 | 2 % |
| 6.3 | 350 | 607 | 6 491 | 2 % |
| 6.8 | 350 | 600 | 8 389 | 2 % |
| 7.3 | 350 | 645 | 6 908 | 3 % |
| 7.8 | 350 | 587 | 7 763 | 2 % |
| 8.3 | 350 | 607 | 6 375 | 2 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
