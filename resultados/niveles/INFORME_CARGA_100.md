# Fase 3: 100 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Usuarios sin degradación | 100 |
| Peticiones hechas | 6 236 |
| Peticiones correctas (2xx) | 6 231 |
| Errores (%) | 0.1 % |
| Ritmo medio (peticiones/s) | 20.2 |
| Tiempo mediano (ms) | 19 |
| Tiempo p95 (ms) | 279 |
| CPU del backend, media (% del límite de 1 CPU) | 39 |
| Tiempo con la CPU al tope (%) | 7 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 32 |
| Memoria pico (MB, de 2048) | 953 |
| Sentencias SQL | 176 223 |
| Reinicios del contenedor | 0 |

## DESPUÉS: AGUANTA; datos CONSISTENTES

- Escrituras correctas: 2222; fallidas: 5.
- Acciones hechas: panel 289, agenda 285, historia 214, reportes 26, recepcion 310, atencion 144, cobro 72, cancelar 61; sin trabajo disponible: 193.
- CPU del backend: 39 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 7 % del tiempo.
- CPU de PostgreSQL: 32 % de media y 99 % de pico (de 1 núcleo).
- Memoria del contenedor: pico de 953 MB (47 % de 2048 MB).
- Los fallos fueron: 100 % otros 4xx.
- Los errores no superaron el 5 % en ningún tramo de 15 s.
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **100 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 0.6 s.
- Correo: el servidor de pruebas recibió 310 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 134 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 132 a 134 MB (+1.6 %).
- GC: 291 pausas, 16.2 s en total (5 % del tiempo de vida de la JVM); 27 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 490 MB (tope -Xmx1g).
- Clases cargadas: 25408 en total, 267 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.0 | 13 | 14 | 3807 |
| 0.1 | 22 | 23 | 5578 |
| 0.1 | 43 | 43 | 9667 |
| 0.2 | 66 | 67 | 13753 |
| 0.2 | 80 | 81 | 16074 |
| 0.3 | 80 | 81 | 16092 |
| 0.4 | 86 | 87 | 17103 |
| 0.4 | 91 | 92 | 17768 |
| 1.1 | 125 | 128 | 24130 |
| 1.9 | 130 | 133 | 24753 |
| 2.7 | 132 | 135 | 25123 |
| 3.5 | 133 | 136 | 25303 |
| 4.4 | 134 | 137 | 25350 |
| 5.0 | 134 | 137 | 25407 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 249 |
| `java.lang.invoke.LambdaForm$DMH/` | 8 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 257 |
| `org.springframework.transaction.support` | 2 |
| `org.hibernate.internal.SessionImpl` | 2 |
| `org.springframework.web.servlet` | 2 |
| `java.util.concurrent.locks` | 1 |
| `org.hibernate.event.spi` | 1 |
| `org.springframework.web.method` | 1 |
| `org.springframework.core.ExceptionDepthComparator` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 310 | 310 | coincide |
| Mascotas | 310 | 310 | coincide |
| Citas agendadas | 305 | 305 | coincide |
| Diagnósticos | 144 | 144 | coincide |
| Recetas | 144 | 144 | coincide |
| Pagos | 72 | 72 | coincide |
| Citas completadas | 144 | 144 | coincide |
| Citas canceladas | 61 | 61 | coincide |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 310 | 0 | 0.0 % | 38 | 100 |
| Registrar mascota | 310 | 0 | 0.0 % | 22 | 56 |
| Agendar cita | 305 | 5 | 1.6 % | 28 | 69 |
| Recepción (llegada) | 144 | 0 | 0.0 % | 16 | 56 |
| Iniciar atención | 144 | 0 | 0.0 % | 21 | 58 |
| Leer la consulta | 144 | 0 | 0.0 % | 12 | 38 |
| Registrar diagnóstico | 144 | 0 | 0.0 % | 13 | 33 |
| Registrar receta | 144 | 0 | 0.0 % | 13 | 30 |
| Actualizar mascota | 144 | 0 | 0.0 % | 11 | 28 |
| Cerrar la consulta | 144 | 0 | 0.0 % | 18 | 48 |
| Caja: crear punto de cobro | 52 | 0 | 0.0 % | 15 | 43 |
| Caja: vincular equipo | 52 | 0 | 0.0 % | 12 | 38 |
| Caja: abrir la caja | 52 | 0 | 0.0 % | 16 | 43 |
| Cobrar | 72 | 0 | 0.0 % | 19 | 55 |
| Cancelar cita | 61 | 0 | 0.0 % | 19 | 68 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| panel | 389 | 0 (0 %) | 269 | 697 | 34 % |
| login | 100 | 0 (0 %) | 375 | 1 099 | 13 % |
| reporte_clinico | 26 | 0 (0 %) | 1 613 | 3 190 | 13 % |
| propietarios | 285 | 0 (0 %) | 46 | 140 | 5 % |
| apoderado | 310 | 0 (0 %) | 38 | 100 | 4 % |
| citas_del_mes | 285 | 0 (0 %) | 32 | 94 | 4 % |
| lista_de_historias | 214 | 0 (0 %) | 38 | 104 | 3 % |
| cita | 310 | 5 (2 %) | 28 | 69 | 3 % |
| mascota | 310 | 0 (0 %) | 22 | 56 | 2 % |
| navegacion | 389 | 0 (0 %) | 13 | 38 | 2 % |
| historia | 214 | 0 (0 %) | 25 | 72 | 2 % |
| empleados | 285 | 0 (0 %) | 18 | 48 | 2 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 7 | 42 | 572 | 0 % |
| 0.1 | 20 | 113 | 315 | 0 % |
| 0.4 | 34 | 174 | 482 | 0 % |
| 0.6 | 48 | 229 | 385 | 0 % |
| 0.9 | 61 | 289 | 273 | 0 % |
| 1.1 | 75 | 296 | 356 | 0 % |
| 1.4 | 89 | 401 | 291 | 0 % |
| 1.6 | 100 | 415 | 382 | 0 % |
| 1.9 | 100 | 414 | 264 | 0 % |
| 2.1 | 100 | 445 | 273 | 0 % |
| 2.4 | 100 | 415 | 306 | 0 % |
| 2.6 | 100 | 370 | 242 | 0 % |
| 2.9 | 100 | 398 | 118 | 0 % |
| 3.1 | 100 | 373 | 110 | 0 % |
| 3.4 | 100 | 377 | 119 | 0 % |
| 3.6 | 100 | 348 | 286 | 0 % |
| 3.9 | 100 | 395 | 247 | 0 % |
| 4.1 | 100 | 404 | 265 | 0 % |
| 4.4 | 98 | 277 | 279 | 0 % |
| 4.6 | 63 | 61 | 25 | 0 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
