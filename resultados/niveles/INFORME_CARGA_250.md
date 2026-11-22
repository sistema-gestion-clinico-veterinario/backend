# Fase 3: 250 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Veredicto | DEGRADADO |
| Usuarios sin degradación | 167 |
| Peticiones hechas | 15 357 |
| Peticiones correctas (2xx) | 15 170 |
| Errores (%) | 1.2 % |
| Ritmo medio (peticiones/s) | 34.3 |
| Tiempo mediano (ms) | 922 |
| Tiempo p95 (ms) | 3 490 |
| CPU del backend, media (% del límite de 1 CPU) | 54 |
| Tiempo con la CPU al tope (%) | 11 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 69 |
| Memoria pico (MB, de 2048) | 1 152 |
| Sentencias SQL | 423 500 |
| Reinicios del contenedor | 0 |

## DESPUÉS: DEGRADADO; datos CONSISTENTES

- Escrituras correctas: 4933; fallidas: 187.
- Acciones hechas: panel 814, agenda 613, historia 528, reportes 60, recepcion 807, atencion 417, cobro 165, cancelar 142; sin trabajo disponible: 644.
- CPU del backend: 54 % de media y 102 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 11 % del tiempo.
- CPU de PostgreSQL: 69 % de media y 104 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 1 152 MB (56 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **167 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 6.0 s.
- Correo: el servidor de pruebas recibió 807 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 136 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 134 a 136 MB (+1.2 %).
- GC: 392 pausas, 32.8 s en total (7 % del tiempo de vida de la JVM); 35 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 637 MB (tope -Xmx1g).
- Clases cargadas: 25516 en total, 122 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 17 | 17 | 4389 |
| 0.1 | 35 | 35 | 7861 |
| 0.2 | 69 | 70 | 14272 |
| 0.3 | 80 | 81 | 16101 |
| 0.4 | 86 | 87 | 17088 |
| 0.5 | 93 | 94 | 18022 |
| 1.5 | 129 | 132 | 24614 |
| 2.3 | 132 | 135 | 25002 |
| 3.0 | 133 | 136 | 25313 |
| 4.0 | 134 | 137 | 25396 |
| 4.9 | 135 | 138 | 25472 |
| 5.8 | 136 | 138 | 25472 |
| 6.7 | 136 | 139 | 25514 |
| 7.7 | 136 | 139 | 25516 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 115 |
| `java.lang.invoke.LambdaForm$DMH/` | 5 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 120 |
| `io.jsonwebtoken.CompressionCodecResolver` | 1 |
| `org.apache.tomcat.util` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 807 | 807 | coincide |
| Mascotas | 807 | 807 | coincide |
| Citas agendadas | 798 | 798 | coincide |
| Diagnósticos | 239 | 239 | coincide |
| Recetas | 239 | 239 | coincide |
| Pagos | 165 | 165 | coincide |
| Citas completadas | 239 | 239 | coincide |
| Citas canceladas | 142 | 142 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 807 | 0 | 0.0 % | 995 | 3 454 |
| Registrar mascota | 807 | 0 | 0.0 % | 888 | 3 490 |
| Agendar cita | 798 | 9 | 1.1 % | 1 007 | 3 539 |
| Recepción (llegada) | 417 | 0 | 0.0 % | 1 161 | 3 428 |
| Iniciar atención | 239 | 178 | 42.7 % | 1 364 | 4 137 |
| Leer la consulta | 239 | 0 | 0.0 % | 738 | 3 173 |
| Registrar diagnóstico | 239 | 0 | 0.0 % | 656 | 3 522 |
| Registrar receta | 239 | 0 | 0.0 % | 882 | 2 974 |
| Actualizar mascota | 239 | 0 | 0.0 % | 722 | 3 192 |
| Cerrar la consulta | 239 | 0 | 0.0 % | 720 | 3 000 |
| Caja: crear punto de cobro | 121 | 0 | 0.0 % | 740 | 3 824 |
| Caja: vincular equipo | 121 | 0 | 0.0 % | 783 | 2 970 |
| Caja: abrir la caja | 121 | 0 | 0.0 % | 755 | 3 313 |
| Cobrar | 165 | 0 | 0.0 % | 963 | 3 231 |
| Cancelar cita | 142 | 0 | 0.0 % | 1 225 | 3 345 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 1064 | 0 (0 %) | 1 496 | 3 923 | 9 % |
| privacidad | 1064 | 0 (0 %) | 1 773 | 3 842 | 9 % |
| cita | 807 | 9 (1 %) | 1 007 | 3 539 | 5 % |
| apoderado | 807 | 0 (0 %) | 995 | 3 454 | 5 % |
| navegacion | 1064 | 0 (0 %) | 574 | 2 966 | 5 % |
| avisos_de_cumplimiento | 1064 | 0 (0 %) | 503 | 2 913 | 5 % |
| mascota | 807 | 0 (0 %) | 888 | 3 490 | 5 % |
| cumplimiento | 1064 | 0 (0 %) | 453 | 2 902 | 5 % |
| notificaciones | 1064 | 0 (0 %) | 460 | 2 817 | 5 % |
| propietarios | 613 | 0 (0 %) | 1 231 | 3 696 | 5 % |
| citas_del_mes | 613 | 0 (0 %) | 1 159 | 4 028 | 5 % |
| empleados | 613 | 0 (0 %) | 1 101 | 3 392 | 4 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 2 | 8 | 1 228 | 0 % |
| 0.3 | 32 | 180 | 924 | 0 % |
| 0.8 | 62 | 323 | 296 | 0 % |
| 1.3 | 92 | 434 | 279 | 0 % |
| 1.8 | 122 | 528 | 473 | 0 % |
| 2.3 | 152 | 669 | 278 | 0 % |
| 2.8 | 182 | 600 | 2 242 | 1 % |
| 3.3 | 212 | 641 | 2 513 | 0 % |
| 3.8 | 242 | 574 | 4 054 | 2 % |
| 4.3 | 250 | 642 | 4 342 | 2 % |
| 4.8 | 250 | 647 | 4 261 | 3 % |
| 5.3 | 250 | 735 | 2 691 | 3 % |
| 5.8 | 250 | 563 | 4 466 | 1 % |
| 6.3 | 250 | 690 | 3 033 | 3 % |
| 6.8 | 236 | 490 | 2 498 | 1 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
