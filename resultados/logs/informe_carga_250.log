# Fase 3: 250 usuarios simultáneos, 2 CPU, 4 GB, heap 2g

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
| Veredicto | SATURADO (muy lento, casi sin errores) |
| Usuarios sin degradación | 179 |
| Peticiones hechas | 13 777 |
| Peticiones correctas (2xx) | 13 623 |
| Errores (%) | 1.1 % |
| Ritmo medio (peticiones/s) | 30.6 |
| Tiempo mediano (ms) | 1 324 |
| Tiempo p95 (ms) | 5 443 |
| CPU del backend, media (% del límite de 2 CPU) | 34 |
| Tiempo con la CPU al tope (%) | 0 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 74 |
| Memoria pico (MB, de 4096) | 1 281 |
| Sentencias SQL | 385 522 |
| Reinicios del contenedor | 0 |

## DESPUÉS: SATURADO (muy lento, casi sin errores); datos CONSISTENTES

- Escrituras correctas: 4598; fallidas: 154.
- Acciones hechas: panel 678, agenda 561, historia 450, reportes 58, recepcion 773, atencion 362, cobro 143, cancelar 146; sin trabajo disponible: 576.
- CPU del backend: 34 % de media y 81 % de pico (100 % = el límite de 2 CPU); estuvo al tope el 0 % del tiempo.
- CPU de PostgreSQL: 74 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 281 MB (31 % de 4096 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **179 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 9.1 s.
- Correo: el servidor de pruebas recibió 773 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 135 a 136 MB (+1.1 %).
- GC: 372 pausas, 42.7 s en total (9 % del tiempo de vida de la JVM); 38 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 703 MB (tope -Xmx2g).
- Clases cargadas: 25543 en total, 122 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1189 |
| 0.0 | 30 | 30 | 6985 |
| 0.1 | 62 | 62 | 13024 |
| 0.1 | 81 | 82 | 16165 |
| 0.2 | 83 | 83 | 16469 |
| 0.3 | 94 | 95 | 18099 |
| 1.5 | 130 | 132 | 24706 |
| 2.4 | 133 | 136 | 25195 |
| 2.9 | 134 | 137 | 25355 |
| 3.4 | 134 | 137 | 25421 |
| 4.1 | 135 | 138 | 25432 |
| 4.7 | 135 | 138 | 25481 |
| 5.4 | 136 | 139 | 25499 |
| 6.3 | 136 | 139 | 25500 |
| 7.2 | 136 | 139 | 25543 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 115 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 120 |
| `org.apache.tomcat.util` | 1 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 773 | 773 | coincide |
| Mascotas | 773 | 773 | coincide |
| Citas agendadas | 763 | 763 | coincide |
| Diagnósticos | 218 | 218 | coincide |
| Recetas | 218 | 218 | coincide |
| Pagos | 143 | 143 | coincide |
| Citas completadas | 218 | 218 | coincide |
| Citas canceladas | 146 | 146 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 773 | 0 | 0.0 % | 1 470 | 5 557 |
| Registrar mascota | 773 | 0 | 0.0 % | 1 360 | 5 713 |
| Agendar cita | 763 | 10 | 1.3 % | 1 412 | 5 454 |
| Recepción (llegada) | 362 | 0 | 0.0 % | 1 559 | 5 490 |
| Iniciar atención | 218 | 144 | 39.8 % | 1 965 | 5 642 |
| Leer la consulta | 218 | 0 | 0.0 % | 645 | 5 355 |
| Registrar diagnóstico | 218 | 0 | 0.0 % | 742 | 4 834 |
| Registrar receta | 218 | 0 | 0.0 % | 496 | 4 458 |
| Actualizar mascota | 218 | 0 | 0.0 % | 684 | 4 113 |
| Cerrar la consulta | 218 | 0 | 0.0 % | 628 | 4 622 |
| Caja: crear punto de cobro | 110 | 0 | 0.0 % | 1 097 | 5 198 |
| Caja: vincular equipo | 110 | 0 | 0.0 % | 880 | 3 971 |
| Caja: abrir la caja | 110 | 0 | 0.0 % | 1 104 | 4 566 |
| Cobrar | 143 | 0 | 0.0 % | 1 392 | 5 182 |
| Cancelar cita | 146 | 0 | 0.0 % | 1 568 | 5 800 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 928 | 0 (0 %) | 2 390 | 6 000 | 9 % |
| privacidad | 928 | 0 (0 %) | 1 357 | 5 562 | 7 % |
| apoderado | 773 | 0 (0 %) | 1 470 | 5 557 | 6 % |
| mascota | 773 | 0 (0 %) | 1 360 | 5 713 | 6 % |
| cita | 773 | 10 (1 %) | 1 412 | 5 454 | 6 % |
| propietarios | 561 | 0 (0 %) | 2 064 | 6 257 | 5 % |
| notificaciones | 928 | 0 (0 %) | 775 | 4 751 | 5 % |
| citas_del_mes | 561 | 0 (0 %) | 2 077 | 6 143 | 5 % |
| navegacion | 928 | 0 (0 %) | 936 | 4 573 | 5 % |
| cumplimiento | 928 | 0 (0 %) | 799 | 4 391 | 5 % |
| avisos_de_cumplimiento | 928 | 0 (0 %) | 782 | 4 189 | 5 % |
| empleados | 561 | 0 (0 %) | 1 795 | 5 617 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 14 | 101 | 326 | 0 % |
| 0.5 | 44 | 226 | 286 | 0 % |
| 1.0 | 74 | 344 | 276 | 0 % |
| 1.5 | 104 | 491 | 207 | 0 % |
| 2.0 | 134 | 566 | 281 | 0 % |
| 2.5 | 164 | 690 | 979 | 0 % |
| 3.0 | 194 | 486 | 2 429 | 0 % |
| 3.5 | 224 | 515 | 4 626 | 0 % |
| 4.0 | 250 | 563 | 5 497 | 2 % |
| 4.5 | 250 | 749 | 2 557 | 1 % |
| 5.0 | 250 | 425 | 6 150 | 2 % |
| 5.5 | 250 | 556 | 4 911 | 2 % |
| 6.0 | 250 | 483 | 7 075 | 3 % |
| 6.5 | 250 | 449 | 6 540 | 1 % |
| 7.0 | 199 | 201 | 3 792 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
