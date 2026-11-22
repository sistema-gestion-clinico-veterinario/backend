# Fase 3: 500 usuarios simultáneos, 1 CPU, 2 GB, heap 1g

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
| Veredicto | FALLA |
| Usuarios sin degradación | 144 |
| Peticiones hechas | 15 546 |
| Peticiones correctas (2xx) | 11 479 |
| Errores (%) | 26.2 % |
| Ritmo medio (peticiones/s) | 21.0 |
| Tiempo mediano (ms) | 3 036 |
| Tiempo p95 (ms) | 30 001 |
| CPU del backend, media (% del límite de 1 CPU) | 38 |
| Tiempo con la CPU al tope (%) | 6 |
| CPU de PostgreSQL, media (% de 1 núcleo) | 45 |
| Memoria pico (MB, de 2048) | 1 265 |
| Sentencias SQL | 329 376 |
| Reinicios del contenedor | 0 |

## DESPUÉS: FALLA; datos CONSISTENTES

- Escrituras correctas: 3162; fallidas: 810.
- Acciones hechas: panel 719, agenda 671, historia 578, reportes 79, recepcion 1008, atencion 346, cobro 102, cancelar 122; sin trabajo disponible: 726.
- Al terminar, el chequeo de salud respondió 000 (no 200).
- Se agotó el pool de conexiones a la base de datos: 2627 avisos en el registro.
- CPU del backend: 38 % de media y 101 % de pico (100 % = el límite de 1 CPU); estuvo al tope el 6 % del tiempo.
- CPU de PostgreSQL: 45 % de media y 103 % de pico (de 1 núcleo).
- **Diagnóstico: hay errores sin que la CPU del backend ni la de la base estén al tope.** Revisar el pool de conexiones y los tiempos de espera.
- Memoria del contenedor: pico de 1 265 MB (62 % de 2048 MB).
- Los fallos fueron: 97 % sin respuesta (tiempo agotado), 3 % otros 4xx.
- **Los errores superaron el 5 % por primera vez al minuto 6.4 con unos 399 usuarios.**
- Sin degradación hasta (primer tramo de 15 s con 1 % de errores o p95 de 2 s): unos **144 usuarios**.
- El peor tiempo p95 en un tramo de 15 s fue de 30.0 s.
- Correo: el servidor de pruebas recibió 650 mensajes; 0 envíos fallidos en el registro del backend.
- Metaspace usado: pico de 137 MB tras un GC (tope configurado: 256 MB).
- **Metaspace estable**: en la segunda mitad pasó de 136 a 137 MB (+0.7 %).
- GC: 383 pausas, 33.1 s en total (4 % del tiempo de vida de la JVM); 36 completas, 3 provocadas por el umbral de metadatos de clases.
- Heap tras GC: máximo de 845 MB (tope -Xmx1g).
- Clases cargadas: 25506 en total, 92 en la segunda mitad de la vida de la JVM.

**Metaspace y clases cargadas (registro de la propia JVM):**

| Minuto de la JVM | Metaspace usado (MB) | Reservado (MB) | Clases cargadas hasta ahí |
|---|---|---|---|
| 0.0 | 2 | 2 | 1187 |
| 0.1 | 16 | 16 | 4248 |
| 0.1 | 33 | 34 | 7608 |
| 0.2 | 66 | 67 | 13754 |
| 0.3 | 80 | 81 | 16101 |
| 0.4 | 86 | 87 | 17086 |
| 0.5 | 96 | 97 | 18355 |
| 1.6 | 129 | 131 | 24505 |
| 2.5 | 132 | 135 | 25013 |
| 3.2 | 133 | 136 | 25297 |
| 4.0 | 134 | 137 | 25380 |
| 4.9 | 135 | 138 | 25405 |
| 5.7 | 135 | 138 | 25412 |
| 6.4 | 136 | 138 | 25415 |
| 9.7 | 136 | 139 | 25503 |

**Clases generadas que siguen cargándose en la segunda mitad (mismo patrón, varias veces):**

| Patrón de clase | Veces |
|---|---|
| `java.lang.invoke.LambdaForm$MH/` | 46 |
| `java.lang.invoke.LambdaForm$DMH/` | 4 |
| `org.springframework.security.core.context.ThreadLocalSecurityContextHolderStrategy$$Lambda/` | 3 |

**Paquetes con más clases cargadas en la segunda mitad:**

| Paquete | Clases |
|---|---|
| `java.lang.invoke.LambdaForm` | 50 |
| `ch.qos.logback.classic` | 6 |
| `org.apache.catalina.core` | 6 |
| `org.springframework.security.core` | 3 |
| `org.springframework.dao.support` | 2 |
| `java.util.stream.ReduceOps` | 2 |
| `org.hibernate.internal.util` | 1 |
| `java.sql.SQLInvalidAuthorizationSpecException` | 1 |

**Comprobación contra la base de datos:**

| Qué | k6 dice que creó | Nuevos en la base | Resultado |
|---|---|---|---|
| Propietarios | 615 | 650 | la base tiene más (ver nota) |
| Mascotas | 603 | 608 | la base tiene más (ver nota) |
| Citas agendadas | 586 | 590 | la base tiene más (ver nota) |
| Diagnósticos | 126 | 127 | la base tiene más (ver nota) |
| Recetas | 126 | 126 | coincide |
| Pagos | 78 | 78 | coincide |
| Citas completadas | 121 | 125 | la base tiene más (ver nota) |
| Citas canceladas | 87 | 91 | la base tiene más (ver nota) |
| Mascotas sin propietario (debe ser 0) | 0 | 0 | coincide |
| Citas sin mascota (debe ser 0) | 0 | 0 | coincide |
| Consultas sin cita (debe ser 0) | 0 | 0 | coincide |
| Documentos de propietarios repetidos (debe ser 0) | 0 | 0 | coincide |

_Nota: si la base tiene más registros que los que k6 contó como correctos, son peticiones a las que k6 renunció por tiempo agotado pero que el servidor sí terminó de procesar. Si faltan registros, sería pérdida de datos._

**Escrituras por paso:**

| Paso | Correctos | Fallidos | % de fallos | Mediana (ms) | p95 (ms) |
|---|---|---|---|---|---|
| Registrar propietario | 615 | 393 | 39.0 % | 6 042 | 30 001 |
| Registrar mascota | 603 | 12 | 2.0 % | 1 522 | 14 732 |
| Agendar cita | 586 | 17 | 2.8 % | 1 453 | 13 716 |
| Recepción (llegada) | 250 | 96 | 27.7 % | 4 154 | 30 001 |
| Iniciar atención | 128 | 218 | 63.0 % | 4 379 | 30 001 |
| Leer la consulta | 126 | 2 | 1.6 % | 639 | 8 354 |
| Registrar diagnóstico | 126 | 2 | 1.6 % | 752 | 15 128 |
| Registrar receta | 126 | 2 | 1.6 % | 943 | 11 344 |
| Actualizar mascota | 126 | 2 | 1.6 % | 1 114 | 12 498 |
| Cerrar la consulta | 121 | 7 | 5.5 % | 1 029 | 30 000 |
| Caja: crear punto de cobro | 64 | 18 | 22.0 % | 1 599 | 30 001 |
| Caja: vincular equipo | 63 | 1 | 1.6 % | 905 | 7 346 |
| Caja: abrir la caja | 63 | 0 | 0.0 % | 1 434 | 9 490 |
| Cobrar | 78 | 5 | 6.0 % | 1 656 | 30 000 |
| Cancelar cita | 87 | 35 | 28.7 % | 3 831 | 30 001 |

**Endpoints que más tiempo acumulan, DESPUÉS:**

| Endpoint | Peticiones | Errores | Mediana (ms) | p95 (ms) | % del tiempo total |
|---|---|---|---|---|---|
| login | 1040 | 667 (64 %) | 30 000 | 30 001 | 14 % |
| apoderado | 1008 | 393 (39 %) | 6 042 | 30 001 | 9 % |
| privacidad | 1201 | 292 (24 %) | 3 151 | 30 001 | 7 % |
| panel | 1092 | 260 (24 %) | 3 447 | 30 001 | 7 % |
| cumplimiento | 1169 | 275 (24 %) | 2 340 | 30 001 | 7 % |
| navegacion | 1201 | 268 (22 %) | 2 229 | 30 001 | 7 % |
| notificaciones | 1139 | 269 (24 %) | 2 047 | 30 001 | 7 % |
| avisos_de_cumplimiento | 1117 | 261 (23 %) | 2 172 | 30 001 | 6 % |
| empleados | 694 | 227 (33 %) | 4 145 | 30 001 | 5 % |
| citas_del_mes | 694 | 210 (30 %) | 4 584 | 30 001 | 5 % |
| propietarios | 671 | 215 (32 %) | 4 637 | 30 001 | 5 % |
| lista_de_historias | 578 | 204 (35 %) | 4 747 | 30 001 | 5 % |

<details><summary>Línea de tiempo cada 15 s</summary>

| Minuto | Usuarios | Peticiones (15 s) | p95 (ms) | Errores |
|---|---|---|---|---|
| 0.0 | 9 | 63 | 758 | 0 % |
| 0.7 | 54 | 221 | 331 | 0 % |
| 1.4 | 99 | 418 | 430 | 0 % |
| 2.2 | 144 | 510 | 1 599 | 0 % |
| 2.9 | 189 | 528 | 3 242 | 1 % |
| 3.7 | 234 | 569 | 6 720 | 1 % |
| 4.4 | 279 | 587 | 7 987 | 2 % |
| 5.2 | 324 | 474 | 12 449 | 1 % |
| 5.9 | 369 | 558 | 17 376 | 1 % |
| 6.7 | 414 | 179 | 30 001 | 100 % |
| 7.4 | 459 | 206 | 30 001 | 100 % |
| 8.2 | 500 | 185 | 30 001 | 100 % |
| 8.9 | 500 | 194 | 30 001 | 100 % |
| 9.7 | 500 | 175 | 30 001 | 100 % |
| 10.4 | 500 | 183 | 30 001 | 100 % |
| 11.2 | 432 | 171 | 30 001 | 100 % |
| 11.9 | 194 | 67 | 30 001 | 100 % |

</details>

> Todos los números salen de los archivos de la prueba. Los usuarios hacen una acción cada ~15 s. Laboratorio con la base de datos en el mismo equipo: sirve para comparar, no es una promesa para producción.
